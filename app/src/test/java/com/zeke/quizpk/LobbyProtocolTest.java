package com.zeke.quizpk;

import static org.junit.Assert.*;

import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class LobbyProtocolTest {
  @Test
  public void javaCreateCodecMatchesSingleSharedNodeFixture() throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("multiplayer-v1.json")) {
      assertNotNull(in);
      JSONObject fixture = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                               .getJSONObject("createFixture");
      JSONObject payload = fixture.getJSONObject("payload");
      JSONObject settings = payload.getJSONObject("settings");
      RoomProtocol.Command command =
          RoomProtocol.create(payload.getString("mode"), payload.getString("displayName"),
              new RoomProtocol.Settings(settings.getString("category"),
                  settings.getString("difficulty"), settings.getInt("questionCount"),
                  settings.getInt("timerSeconds")),
              fixture.getLong("issuedAtMs"), fixture.getString("requestId"));
      assertEquals(fixture.toString(), new JSONObject(command.body).toString());
      assertEquals("/api/rooms", command.path);
    }
  }
  @Test
  public void namesCodesSettingsAndUnknownCommandsAreValidated() throws Exception {
    assertEquals("Alice", RoomProtocol.name("  Ａlice "));
    assertEquals("Alice", RoomProtocol.name("\ufeff\u00a0Alice\u2028\t"));
    assertEquals("ABC234", RoomProtocol.code(" abc234 "));
    for (String name :
        new String[] {"", "a\u0000", "a\u001c", "a\u0085", "a\u202e", "x".repeat(41)})
      assertThrows(IllegalArgumentException.class, () -> RoomProtocol.name(name));
    assertThrows(IllegalArgumentException.class, () -> RoomProtocol.code("123IO0"));
    assertThrows(
        IllegalArgumentException.class, () -> new RoomProtocol.Settings("Unknown", "Easy", 5, 10));
    assertThrows(IllegalArgumentException.class,
        ()
            -> RoomProtocol.member(LobbyFixtures.ROOM, "start", false, null, 0, LobbyFixtures.TIME,
                LobbyFixtures.id(99)));
  }
  @Test
  public void moderatorSnapshotHasSeparateRoleAndEightContestantSeats() throws Exception {
    RoomProtocol.Snapshot s = new RoomProtocol.Snapshot(
        LobbyFixtures.snapshot(true, "MODERATOR", true, false, 8, 1).toString());
    assertEquals(9, s.members.size());
    assertEquals("MODERATOR", s.selfRole);
    assertFalse(s.self().ready);
    assertTrue(s.owner);
    assertEquals(8, s.members.stream().filter(m -> m.role.equals("CONTESTANT")).count());
  }
  @Test
  public void snapshotRejectsPrivateFieldsCoercionsDuplicateIdsAndCapacityOverflow()
      throws Exception {
    JSONObject privateField = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 1);
    privateField.getJSONObject("room").put("correctIndex", 2);
    JSONObject coercion = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 1);
    coercion.getJSONObject("self").put("isOwner", "true");
    JSONObject duplicate = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 1, 1);
    duplicate.getJSONObject("room").getJSONArray("members").getJSONObject(1).put(
        "participantId", LobbyFixtures.SELF);
    JSONObject role = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 1);
    role.getJSONObject("self").put("role", "MODERATOR");
    for (JSONObject o : Arrays.asList(privateField, coercion, duplicate, role,
             LobbyFixtures.snapshot(true, "CLASSIC", true, false, 8, 1)))
      assertThrows(Exception.class, () -> new RoomProtocol.Snapshot(o.toString()));
  }
  @Test
  public void revisionsAndEpochsReconcileWithoutMergingStaleState() throws Exception {
    RoomProtocol.Snapshot first = new RoomProtocol.Snapshot(
        LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 5).toString());
    RoomProtocol.Snapshot stale = new RoomProtocol.Snapshot(
        LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 4).toString());
    assertFalse(stale.follows(first));
    JSONObject boot = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 6);
    boot.getJSONObject("authority").put("instanceId", LobbyFixtures.id(90));
    assertTrue(new RoomProtocol.Snapshot(boot.toString()).follows(first));
  }
  @Test
  public void terminalSnapshotOmitsInviteSettingsAndRoster() throws Exception {
    JSONObject terminal = LobbyFixtures.snapshot(true, "MODERATOR", true, false, 2, 10),
               room = terminal.getJSONObject("room");
    room.put("state", "CLOSED").put("hostOnline", false).put("members", new JSONArray());
    room.remove("settings");
    room.remove("inviteCode");
    RoomProtocol.Snapshot s = new RoomProtocol.Snapshot(terminal.toString());
    assertFalse(s.lobby());
    assertNull(s.inviteCode);
    assertNull(s.settings);
    assertTrue(s.members.isEmpty());
  }
  @Test
  public void localAndModeratorGameplayFailClosedWhileClassicCapabilityIsSupported()
      throws Exception {
    JSONObject local = LobbyFixtures.snapshot(false, "CLASSIC", true, false, 0, 1);
    local.getJSONObject("authority").put("network", "LOCAL");
    assertThrows(JSONException.class, () -> new RoomProtocol.Snapshot(local.toString()));
    JSONObject matches = LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 1);
    matches.getJSONObject("capabilities").put("matches", true);
    assertTrue(new RoomProtocol.Snapshot(matches.toString()).matchEnabled);
    JSONObject moderator = LobbyFixtures.snapshot(true, "MODERATOR", true, false, 0, 1);
    moderator.getJSONObject("capabilities").put("matches", true);
    assertThrows(JSONException.class, () -> new RoomProtocol.Snapshot(moderator.toString()));
    assertThrows(JSONException.class, () -> RoomProtocol.json("{} trailing"));
  }
  @Test
  public void receiptMustMatchPendingIdAndNeverImportsServerRoles() throws Exception {
    RoomProtocol.Command command = RoomProtocol.create(
        "CLASSIC", "Alice", LobbyFixtures.settings(), LobbyFixtures.TIME, LobbyFixtures.id(50));
    JSONObject receipt = new JSONObject()
                             .put("protocolVersion", 1)
                             .put("requestId", LobbyFixtures.id(51))
                             .put("result",
                                 new JSONObject()
                                     .put("roomId", LobbyFixtures.ROOM)
                                     .put("participantId", LobbyFixtures.SELF)
                                     .put("inviteCode", "ABC234"));
    assertThrows(JSONException.class, () -> RoomProtocol.receipt(receipt.toString(), command));
    receipt.put("requestId", command.requestId);
    receipt.getJSONObject("result").put("role", "MODERATOR");
    assertThrows(JSONException.class, () -> RoomProtocol.receipt(receipt.toString(), command));
  }
  @Test
  public void typedHttpErrorsAndRateAdviceNeverExposeResponseBodies() throws Exception {
    RoomBackendClient.Failure full = assertThrows(RoomBackendClient.Failure.class,
        ()
            -> RoomBackendClient.decodeHttp(409,
                "{\"protocolVersion\":1,\"error\":{\"code\":\"ROOM_FULL\",\"secret\":\"canary\"}}",
                null, true));
    assertEquals("ROOM_FULL", full.code);
    assertFalse(full.uncertain);
    assertFalse(full.getMessage().contains("canary"));
    RoomBackendClient.Failure rate = assertThrows(RoomBackendClient.Failure.class,
        () -> RoomBackendClient.decodeHttp(429, "unexpected private body", "120", true));
    assertEquals(120000, rate.retryAfterMs);
    assertFalse(rate.getMessage().contains("private"));
    RoomBackendClient.Failure old = assertThrows(RoomBackendClient.Failure.class,
        ()
            -> RoomBackendClient.decodeHttp(
                404, "{\"schemaVersion\":1,\"error\":{\"code\":\"NOT_FOUND\"}}", null, false));
    assertEquals("ROOMS_UNAVAILABLE", old.code);
  }
  static final class Connection extends HttpsURLConnection {
    final ByteArrayOutputStream sent = new ByteArrayOutputStream();
    byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
    int status = 200;
    boolean disconnected;
    Connection(URL url) {
      super(url);
    }
    public void connect() {}
    public void disconnect() {
      disconnected = true;
    }
    public boolean usingProxy() {
      return false;
    }
    public String getCipherSuite() {
      return "test";
    }
    public Certificate[] getLocalCertificates() {
      return null;
    }
    public Certificate[] getServerCertificates() {
      return new Certificate[0];
    }
    public int getResponseCode() {
      return status;
    }
    public InputStream getInputStream() {
      return new ByteArrayInputStream(response);
    }
    public InputStream getErrorStream() {
      return getInputStream();
    }
    public OutputStream getOutputStream() {
      return sent;
    }
    public String getHeaderField(String name) {
      return null;
    }
  }
  @Test
  public void httpsAdapterKeepsTokenInHeaderAndRefusesRedirects() throws Exception {
    Connection connection = new Connection(new URL(LobbyFixtures.ENDPOINT));
    AtomicInteger calls = new AtomicInteger();
    RoomBackendClient.HttpsTransport adapter = new RoomBackendClient.HttpsTransport(url -> {
      assertEquals("https", url.getProtocol());
      assertEquals("/api/rooms", url.getPath());
      calls.incrementAndGet();
      return connection;
    });
    assertEquals("{}",
        adapter.request(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "POST", "/api/rooms", "{}"));
    assertEquals("Bearer " + LobbyFixtures.TOKEN, connection.getRequestProperty("Authorization"));
    assertEquals("{}", connection.sent.toString("UTF-8"));
    assertFalse(connection.getInstanceFollowRedirects());
    assertTrue(connection.disconnected);
    connection.status = 302;
    assertThrows(RoomBackendClient.Failure.class,
        ()
            -> adapter.request(
                LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "POST", "/api/rooms", "{}"));
    assertEquals(2, calls.get());
  }
  @Test
  public void httpsAdapterRejectsCleartextOversizedBodiesResponsesAndCertificateFailure()
      throws Exception {
    AtomicInteger opens = new AtomicInteger();
    Connection connection = new Connection(new URL(LobbyFixtures.ENDPOINT));
    RoomBackendClient.HttpsTransport adapter = new RoomBackendClient.HttpsTransport(url -> {
      opens.incrementAndGet();
      return connection;
    });
    assertThrows(RoomBackendClient.Failure.class,
        ()
            -> adapter.request(
                "http://127.0.0.1", LobbyFixtures.TOKEN, "GET", "/api/rooms/current", null));
    assertEquals(0, opens.get());
    assertThrows(RoomBackendClient.Failure.class,
        ()
            -> adapter.request(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "POST", "/api/rooms",
                "x".repeat(4097)));
    connection.response = new byte[32769];
    RoomBackendClient.Failure big = assertThrows(RoomBackendClient.Failure.class,
        ()
            -> adapter.request(
                LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "POST", "/api/rooms", "{}"));
    assertTrue(big.uncertain);
    assertEquals("MALFORMED_RESPONSE", big.code);
    RoomBackendClient.HttpsTransport tls = new RoomBackendClient.HttpsTransport(
        url -> { throw new SSLHandshakeException("private certificate detail"); });
    assertEquals("SECURE_CONNECTION_FAILED",
        assertThrows(RoomBackendClient.Failure.class,
            ()
                -> tls.request(
                    LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "GET", "/api/rooms/current", null))
            .code);
  }
}
