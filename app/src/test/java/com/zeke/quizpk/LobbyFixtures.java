package com.zeke.quizpk;

import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import com.zeke.quizpk.ui.screens.LobbyViewModel;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Synthetic identities and server responses, never production credentials. */
final class LobbyFixtures {
  static final String TOKEN = "x".repeat(64), ENDPOINT = "https://lobby.invalid";
  static final String ROOM = id(1), SELF = id(2), INSTANCE = id(3);
  static String id(int n) {
    return String.format(Locale.ROOT, "00000000-0000-4000-8000-%012d", n);
  }
  static final long TIME = 1791849600000L;
  static RoomProtocol.Settings settings() {
    return new RoomProtocol.Settings("All Categories", "Normal", 10, 15);
  }
  static JSONObject member(String id, String role, String name, boolean ready, boolean online)
      throws Exception {
    return new JSONObject()
        .put("participantId", id)
        .put("role", role)
        .put("displayName", name)
        .put("ready", ready)
        .put("online", online);
  }
  static JSONObject snapshot(boolean exists, String mode, boolean owner, boolean ready, int extras,
      long revision) throws Exception {
    JSONObject result =
        new JSONObject()
            .put("protocolVersion", 1)
            .put("serverTimeMs", TIME)
            .put(
                "authority", new JSONObject().put("network", "ONLINE").put("instanceId", INSTANCE));
    if (!exists)
      return result.put("room", JSONObject.NULL);
    boolean mod = mode.equals("MODERATOR");
    String role = mod && owner ? "MODERATOR" : "CONTESTANT";
    JSONArray members =
        new JSONArray().put(member(SELF, role, "Alice", ready && role.equals("CONTESTANT"), true));
    if (mod && !owner)
      members.put(member(id(4), "MODERATOR", "Host", false, true));
    for (int i = 0; i < extras; i++)
      members.put(member(id(10 + i), "CONTESTANT", "Player " + (i + 1), false, true));
    return result
        .put("room",
            new JSONObject()
                .put("id", ROOM)
                .put("mode", mode)
                .put("state", "LOBBY")
                .put("revision", revision)
                .put("expiresAtMs", TIME + 7200000)
                .put("inviteCode", "ABC234")
                .put("settings", settings().toJson())
                .put("maxContestants", 8)
                .put("minContestants", 2)
                .put("hostOnline", true)
                .put("members", members))
        .put("self",
            new JSONObject().put("participantId", SELF).put("role", role).put("isOwner", owner))
        .put("capabilities",
            new JSONObject().put("lobby", true).put("matches", false).put("localHost", false));
  }
  static final class Service implements LobbyViewModel.TransportFactory {
    volatile boolean exists, owner = true, ready, loseReply;
    volatile int extras;
    volatile String mode = "CLASSIC", getCode, postCode;
    volatile CountDownLatch entered, release;
    long revision = 1;
    final List<String> bodies = Collections.synchronizedList(new ArrayList<>()),
                       paths = Collections.synchronizedList(new ArrayList<>());
    final Map<String, String> receipts = new HashMap<>();
    @Override
    public RoomBackendClient.Transport create() {
      return new RoomBackendClient.Transport() {
        public String request(String e, String t, String method, String path, String body)
            throws RoomBackendClient.Failure {
          return Service.this.request(method, path, body);
        }
        public void cancel() {
          if (release != null)
            release.countDown();
        }
      };
    }
    private String request(String method, String path, String body)
        throws RoomBackendClient.Failure {
      paths.add(method + " " + path);
      if (method.equals("GET") && getCode != null)
        throw new RoomBackendClient.Failure(getCode, false);
      if (method.equals("POST")) {
        bodies.add(body);
        if (entered != null && body.contains("room.create")) {
          entered.countDown();
          try {
            if (release != null)
              release.await(5, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      synchronized (this) {
        try {
          if (method.equals("GET"))
            return snapshot(exists, mode, owner, ready, extras, revision).toString();
          if (postCode != null)
            throw new RoomBackendClient.Failure(
                postCode, false, postCode.equals("ROOM_RATE_LIMITED") ? 60000 : 0);
          JSONObject command = new JSONObject(body);
          String id = command.getString("requestId"), type = command.getString("type");
          if (receipts.containsKey(id))
            return receipts.get(id);
          switch (type) {
            case "room.create":
              exists = true;
              owner = true;
              mode = command.getJSONObject("payload").getString("mode");
              break;
            case "room.join":
              exists = true;
              owner = false;
              break;
            case "room.ready":
              ready = command.getJSONObject("payload").getBoolean("ready");
              break;
            case "room.settings":
              if (command.getLong("expectedRevision") != revision)
                throw new RoomBackendClient.Failure("REVISION_CONFLICT", false);
              ready = false;
              break;
            case "room.leave":
              exists = false;
              break;
          }
          revision++;
          JSONObject result = new JSONObject().put("roomId", ROOM).put("participantId", SELF);
          if (type.equals("room.create"))
            result.put("inviteCode", "ABC234");
          String receipt = new JSONObject()
                               .put("protocolVersion", 1)
                               .put("requestId", id)
                               .put("result", result)
                               .toString();
          receipts.put(id, receipt);
          if (loseReply) {
            loseReply = false;
            throw new RoomBackendClient.Failure("NO_CONNECTION", true);
          }
          return receipt;
        } catch (RoomBackendClient.Failure e) {
          throw e;
        } catch (Exception e) {
          throw new RoomBackendClient.Failure("MALFORMED_RESPONSE", false);
        }
      }
    }
    long posts(String type) {
      return bodies.stream().filter(b -> b.contains("\"" + type + "\"")).count();
    }
  }
}
