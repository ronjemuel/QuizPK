package com.zeke.quizpk;

import static org.junit.Assert.*;

import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.multiplayer.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class ClassicBackendIntegrationTest {
  @Test
  public void twoJavaClientsCompleteARealNodeSqliteMatchAndRecoverDurableResults()
      throws Exception {
    File root = new File(System.getProperty("user.dir"));
    while (root != null && !new File(root, "backend/test/android-classic-server.mjs").isFile())
      root = root.getParentFile();
    assertNotNull(root);
    Process process = new ProcessBuilder(
        "node", new File(root, "backend/test/android-classic-server.mjs").getAbsolutePath())
                          .start();
    try {
      BufferedReader control = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter commands = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      String line = control.readLine();
      assertNotNull(line);
      assertTrue(line.startsWith("PORT="));
      int port = Integer.parseInt(line.substring(5));
      RoomBackendClient.Transport transport = new RoomBackendClient.Transport() {
        public String request(String endpoint, String token, String method, String path,
            String body) throws RoomBackendClient.Failure {
          HttpURLConnection c = null;
          try {
            c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(5000);
            c.setRequestMethod(method);
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("X-QuizPK-Features", "classic-v1");
            if (body != null) {
              c.setDoOutput(true);
              c.setRequestProperty("Content-Type", "application/json");
              try (OutputStream out = c.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
              }
            }
            int status = c.getResponseCode();
            try (InputStream in = status == 200 ? c.getInputStream() : c.getErrorStream()) {
              return RoomBackendClient.decodeHttp(status,
                  new String(in.readAllBytes(), StandardCharsets.UTF_8),
                  c.getHeaderField("Retry-After"), body != null);
            }
          } catch (RoomBackendClient.Failure e) {
            throw e;
          } catch (Exception e) {
            throw new RoomBackendClient.Failure("NO_CONNECTION", body != null);
          } finally {
            if (c != null)
              c.disconnect();
          }
        }
        public void cancel() {}
      };
      RoomBackendClient host = new RoomBackendClient(
                            "https://classic.invalid", "a".repeat(64), transport),
                        player = new RoomBackendClient(
                            "https://classic.invalid", "b".repeat(64), transport);
      String id = host.execute(RoomProtocol.create("CLASSIC", "Host",
          new RoomProtocol.Settings("Science", "Normal", 5, 10), host.current().serverTimeMs,
          UUID.randomUUID().toString()));
      player.execute(RoomProtocol.join(host.room(id).inviteCode, "Player",
          player.current().serverTimeMs, UUID.randomUUID().toString()));
      for (RoomBackendClient client : List.of(host, player))
        client.execute(RoomProtocol.member(id, "ready", true, null, 0, client.room(id).serverTimeMs,
            UUID.randomUUID().toString()));
      RoomProtocol.Snapshot ready = host.room(id);
      assertEquals("FORBIDDEN",
          assertThrows(RoomBackendClient.Failure.class,
              ()
                  -> player.execute(RoomProtocol.startMatch(
                      id, ready.revision, ready.serverTimeMs, UUID.randomUUID().toString())))
              .code);
      RoomProtocol.Command start = RoomProtocol.startMatch(
          id, ready.revision, ready.serverTimeMs, UUID.randomUUID().toString());
      host.execute(start);
      assertEquals(id, host.execute(start));
      int expected = 0;
      Set<String> rounds = new HashSet<>();
      for (int n = 0; n < 5; n++) {
        advance(commands, control, 3000);
        RoomProtocol.Snapshot live = host.room(id);
        RoomProtocol.Snapshot other = player.room(id);
        assertEquals("ROUND_LIVE", live.state);
        assertNull(live.match.round.reveal);
        assertEquals(live.match.round.text, other.match.round.text);
        assertEquals(live.match.round.deadlineMs, other.match.round.deadlineMs);
        assertTrue(rounds.add(live.match.round.id));
        advance(commands, control, 1000);
        RoomProtocol.Command answer = RoomProtocol.answer(
            id, live.match.round.id, 0, live.serverTimeMs + 1000, UUID.randomUUID().toString());
        host.execute(answer);
        host.execute(answer);
        assertEquals(Integer.valueOf(0), host.room(id).match.round.ownOption);
        assertNull(player.room(id).match.round.ownOption);
        assertEquals("ANSWER_ALREADY_ACCEPTED",
            assertThrows(RoomBackendClient.Failure.class,
                ()
                    -> host.execute(RoomProtocol.answer(id, live.match.round.id, 1,
                        live.serverTimeMs + 1000, UUID.randomUUID().toString())))
                .code);
        assertNull(host.room(id).match.round.reveal);
        advance(commands, control, 9000);
        RoomProtocol.Snapshot revealed = host.room(id);
        assertEquals("ROUND_REVEALED", revealed.state);
        if (revealed.match.round.reveal.correctIndex == 0)
          expected += 145;
        assertEquals(expected,
            revealed.match.standings.stream()
                .filter(p -> p.participantId.equals(revealed.selfId))
                .findFirst()
                .get()
                .score);
        advance(commands, control, 5000);
        host.room(id);
      }
      RoomProtocol.Snapshot done = host.current();
      assertEquals("MATCH_FINISHED", done.state);
      assertEquals("COMPLETED", done.match.finishReason);
      String epoch = done.instanceId;
      commands.write("{\"type\":\"restart\"}\n");
      commands.flush();
      assertEquals("ACK", control.readLine());
      RoomProtocol.Snapshot recovered = host.current();
      assertNotEquals(epoch, recovered.instanceId);
      assertEquals(expected,
          recovered.match.standings.stream()
              .filter(p -> p.participantId.equals(recovered.selfId))
              .findFirst()
              .get()
              .score);
      host.execute(RoomProtocol.member(
          id, "leave", false, null, 0, recovered.serverTimeMs, UUID.randomUUID().toString()));
      assertFalse(host.current().hasRoom());
      commands.write("{\"type\":\"close\"}\n");
      commands.flush();
      assertTrue(process.waitFor(10, TimeUnit.SECONDS));
      assertEquals(0, process.exitValue());
    } finally {
      process.destroy();
      if (!process.waitFor(5, TimeUnit.SECONDS))
        process.destroyForcibly();
    }
  }
  private void advance(BufferedWriter out, BufferedReader in, long ms) throws Exception {
    out.write("{\"type\":\"advance\",\"ms\":" + ms + "}\n");
    out.flush();
    assertEquals("ACK", in.readLine());
  }
}
