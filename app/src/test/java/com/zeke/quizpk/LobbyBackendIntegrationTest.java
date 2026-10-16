package com.zeke.quizpk;

import static org.junit.Assert.*;

import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

/**
 * Actual Java codec -> test-only HTTP adapter -> real M4.1 Node/SQLite authority.
 * This deliberately tests no real provider, live DB, phone or public TLS edge.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class LobbyBackendIntegrationTest {
  @Test
  public void twoNativeClientsUseRealModeratorLobbyAuthorityWithoutChangingM3() throws Exception {
    File root = new File(System.getProperty("user.dir"));
    while (root != null && !new File(root, "backend/test/android-lobby-server.mjs").isFile())
      root = root.getParentFile();
    assertNotNull(root);
    Process server = new ProcessBuilder(
        "node", new File(root, "backend/test/android-lobby-server.mjs").getAbsolutePath())
                         .start();
    try {
      String line =
          new BufferedReader(new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8))
              .readLine();
      assertNotNull(line);
      assertTrue(line.startsWith("PORT="));
      int port = Integer.parseInt(line.substring(5));
      RoomBackendClient.Transport testOnlyHttp = new RoomBackendClient.Transport() {
        public String request(String endpoint, String token, String method, String path,
            String body) throws RoomBackendClient.Failure {
          HttpURLConnection connection = null;
          try {
            connection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
              connection.setDoOutput(true);
              connection.setRequestProperty("Content-Type", "application/json");
              try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
              }
            }
            int status = connection.getResponseCode();
            try (InputStream in =
                     status == 200 ? connection.getInputStream() : connection.getErrorStream()) {
              return RoomBackendClient.decodeHttp(status,
                  new String(in.readAllBytes(), StandardCharsets.UTF_8),
                  connection.getHeaderField("Retry-After"), body != null);
            }
          } catch (RoomBackendClient.Failure e) {
            throw e;
          } catch (Exception e) {
            throw new RoomBackendClient.Failure("NO_CONNECTION", body != null);
          } finally {
            if (connection != null)
              connection.disconnect();
          }
        }
        public void cancel() {}
      };
      RoomBackendClient host = new RoomBackendClient(
                            "https://fixture.invalid", "a".repeat(64), testOnlyHttp),
                        player = new RoomBackendClient(
                            "https://fixture.invalid", "b".repeat(64), testOnlyHttp);
      RoomProtocol.Snapshot empty = host.current();
      assertFalse(empty.hasRoom());
      RoomProtocol.Command create = RoomProtocol.create("MODERATOR", "Host",
          LobbyFixtures.settings(), empty.serverTimeMs, UUID.randomUUID().toString());
      String room = host.execute(create);
      assertEquals(room, host.execute(create));
      RoomProtocol.Snapshot lobby = host.room(room);
      player.execute(RoomProtocol.join(
          lobby.inviteCode, "Player", player.current().serverTimeMs, UUID.randomUUID().toString()));
      RoomProtocol.Snapshot joined = player.room(room);
      assertEquals("CONTESTANT", joined.selfRole);
      assertFalse(joined.owner);
      assertEquals(2, joined.members.size());
      player.execute(RoomProtocol.member(
          room, "ready", true, null, 0, joined.serverTimeMs, UUID.randomUUID().toString()));
      RoomProtocol.Snapshot ready = host.room(room);
      assertEquals("MODERATOR", ready.selfRole);
      assertTrue(ready.members.stream().anyMatch(m -> m.role.equals("CONTESTANT") && m.ready));
      assertFalse(ready.self().ready);
      host.execute(RoomProtocol.member(room, "settings", false,
          new RoomProtocol.Settings("Science", "Hard", 5, 20), ready.revision, ready.serverTimeMs,
          UUID.randomUUID().toString()));
      assertFalse(player.room(room).self().ready);
      RoomProtocol.Snapshot current = player.room(room);
      RoomBackendClient.Failure denied = assertThrows(RoomBackendClient.Failure.class,
          ()
              -> player.execute(
                  RoomProtocol.member(room, "settings", false, LobbyFixtures.settings(),
                      current.revision, current.serverTimeMs, UUID.randomUUID().toString())));
      assertEquals("FORBIDDEN", denied.code);
      host.execute(RoomProtocol.member(room, "leave", false, null, 0, host.room(room).serverTimeMs,
          UUID.randomUUID().toString()));
      RoomProtocol.Snapshot closed = player.room(room);
      assertEquals("CLOSED", closed.state);
      assertNull(closed.inviteCode);
      assertTrue(closed.members.isEmpty());
    } finally {
      server.destroy();
      if (!server.waitFor(10, TimeUnit.SECONDS))
        server.destroyForcibly();
    }
  }
}
