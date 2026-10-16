package com.zeke.quizpk;

import android.os.SystemClock;
import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.ui.screens.LobbyViewModel;
import java.util.*;
import org.json.*;

final class ClassicFixtures implements LobbyViewModel.TransportFactory {
  volatile String phase = "LOBBY", error;
  volatile boolean owner = true, loseAnswer;
  Integer ownOption;
  long revision = 1;
  final long clockStart = SystemClock.elapsedRealtime();
  final Map<String, String> receipts = new HashMap<>();
  final List<String> answerBodies = new ArrayList<>();
  long now() {
    return LobbyFixtures.TIME + SystemClock.elapsedRealtime() - clockStart;
  }
  static JSONObject game(String phase, boolean owner, Integer option, long revision, long now)
      throws Exception {
    JSONObject o = LobbyFixtures.snapshot(true, "CLASSIC", owner, true, 1, revision);
    o.put("serverTimeMs", now);
    o.getJSONObject("capabilities").put("matches", true);
    o.getJSONObject("room").getJSONArray("members").getJSONObject(1).put("ready", true);
    if (phase.equals("LOBBY"))
      return o;
    o.getJSONObject("room").put("state", phase);
    boolean revealed = phase.equals("ROUND_REVEALED") || phase.equals("MATCH_FINISHED");
    JSONObject round =
        new JSONObject()
            .put("id", LobbyFixtures.id(50))
            .put("number", 1)
            .put("state", revealed ? "ROUND_REVEALED" : phase)
            .put("startedAtMs", LobbyFixtures.TIME + 3000)
            .put("deadlineMs", LobbyFixtures.TIME + 18000)
            .put("revealUntilMs", revealed ? LobbyFixtures.TIME + 23000 : JSONObject.NULL)
            .put("question",
                phase.equals("ROUND_PREPARED")
                    ? JSONObject.NULL
                    : new JSONObject()
                          .put("text", "Which answer did the server choose?")
                          .put(
                              "options", new JSONArray(List.of("Alpha", "Beta", "Gamma", "Delta"))))
            .put("submitted",
                option == null ? new JSONArray() : new JSONArray().put(LobbyFixtures.SELF))
            .put("ownOption", option == null ? JSONObject.NULL : option)
            .put("reveal", JSONObject.NULL);
    JSONArray standings = new JSONArray();
    for (String id : List.of(LobbyFixtures.SELF, LobbyFixtures.id(10)))
      standings.put(new JSONObject()
              .put("participantId", id)
              .put("displayName", id.equals(LobbyFixtures.SELF) ? "Alice" : "Player 1")
              .put("rank", 1)
              .put("score", 0)
              .put("correctCount", 0)
              .put("fastestWins", 0)
              .put("accuracy", 0));
    if (revealed) {
      JSONArray answers = new JSONArray();
      for (String id : List.of(LobbyFixtures.SELF, LobbyFixtures.id(10))) {
        boolean answered = id.equals(LobbyFixtures.SELF) && option != null;
        answers.put(new JSONObject()
                .put("participantId", id)
                .put("option", answered ? option : JSONObject.NULL)
                .put("elapsedMs", answered ? 1000 : JSONObject.NULL)
                .put("sequence", answered ? 1 : JSONObject.NULL)
                .put("correct", false)
                .put("points", 0));
      }
      round.put("reveal",
          new JSONObject()
              .put("correctIndex", 2)
              .put("explanation", "SERVER_REVEALED_EXPLANATION")
              .put("firstSubmission", option == null ? JSONObject.NULL : LobbyFixtures.SELF)
              .put("fastestCorrect", JSONObject.NULL)
              .put("answers", answers));
    }
    o.getJSONObject("room").put("match",
        new JSONObject()
            .put("phase", phase)
            .put("questionCount", 10)
            .put("startedAtMs", LobbyFixtures.TIME)
            .put("finishedAtMs",
                phase.equals("MATCH_FINISHED") ? LobbyFixtures.TIME + 23000 : JSONObject.NULL)
            .put("finishReason", phase.equals("MATCH_FINISHED") ? "COMPLETED" : JSONObject.NULL)
            .put("round", round)
            .put("standings", standings));
    return o;
  }
  @Override
  public RoomBackendClient.Transport create() {
    return new RoomBackendClient.Transport() {
      public String request(String endpoint, String token, String method, String path, String body)
          throws RoomBackendClient.Failure {
        synchronized (ClassicFixtures.this) {
          try {
            if (error != null)
              throw new RoomBackendClient.Failure(error, false);
            if (method.equals("GET"))
              return phase.equals("NONE")
                  ? LobbyFixtures.snapshot(false, "CLASSIC", owner, false, 0, revision).toString()
                  : game(phase, owner, ownOption, revision, now()).toString();
            JSONObject c = new JSONObject(body);
            String id = c.getString("requestId"), type = c.getString("type");
            if (type.equals("match.answer"))
              answerBodies.add(body);
            if (receipts.containsKey(id))
              return receipts.get(id);
            if (type.equals("match.start"))
              phase = "ROUND_PREPARED";
            if (type.equals("match.answer"))
              ownOption = c.getJSONObject("payload").getInt("option");
            if (type.equals("room.leave"))
              phase = "NONE";
            revision++;
            String reply = new JSONObject()
                               .put("protocolVersion", 1)
                               .put("requestId", id)
                               .put("result",
                                   new JSONObject()
                                       .put("roomId", LobbyFixtures.ROOM)
                                       .put("participantId", LobbyFixtures.SELF))
                               .toString();
            receipts.put(id, reply);
            if (type.equals("match.answer") && loseAnswer) {
              loseAnswer = false;
              throw new RoomBackendClient.Failure("TIMEOUT", true);
            }
            return reply;
          } catch (RoomBackendClient.Failure e) {
            throw e;
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      }
      public void cancel() {}
    };
  }
}
