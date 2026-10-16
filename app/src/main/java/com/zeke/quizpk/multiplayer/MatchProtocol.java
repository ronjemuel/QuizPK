package com.zeke.quizpk.multiplayer;

import static com.zeke.quizpk.multiplayer.RoomProtocol.*;

import java.util.*;
import org.json.*;

/** Contestant-safe Classic snapshot. This codec never grades an answer. */
public final class MatchProtocol {
  private MatchProtocol() {}
  private static final long MAX_TIME = 9007199254740991L;
  private static Long optionalNumber(JSONObject o, String k, long min, long max)
      throws JSONException {
    return o.isNull(k) ? null : integer(o, k, min, max);
  }
  private static String optionalId(JSONObject o, String k, Set<String> ids) throws JSONException {
    if (o.isNull(k))
      return null;
    String value = id(o, k);
    if (!ids.contains(value))
      throw new JSONException("Unknown participant");
    return value;
  }
  public static final class Standing {
    public final String participantId, displayName;
    public final int rank, score, correctCount, fastestWins, accuracy;
    Standing(JSONObject o, int count, Set<String> ids) throws JSONException {
      keys(o, "participantId", "displayName", "rank", "score", "correctCount", "fastestWins",
          "accuracy");
      participantId = id(o, "participantId");
      displayName = name(string(o, "displayName"));
      rank = (int) integer(o, "rank", 1, 8);
      score = (int) integer(o, "score", 0, count * 150);
      correctCount = (int) integer(o, "correctCount", 0, count);
      fastestWins = (int) integer(o, "fastestWins", 0, correctCount);
      accuracy = (int) integer(o, "accuracy", 0, 100);
      if (!ids.contains(participantId) || score < correctCount * 100 || score > correctCount * 150
          || accuracy != 100 * correctCount / count)
        throw new JSONException("Invalid standings");
    }
  }
  public static final class Answer {
    public final String participantId;
    public final Integer option;
    public final Long elapsedMs, sequence;
    public final boolean correct;
    public final int points;
    Answer(JSONObject o, long duration, Set<String> ids) throws JSONException {
      keys(o, "participantId", "option", "elapsedMs", "sequence", "correct", "points");
      participantId = id(o, "participantId");
      if (!ids.contains(participantId))
        throw new JSONException("Unknown participant");
      Long opt = optionalNumber(o, "option", 0, 3);
      option = opt == null ? null : opt.intValue();
      elapsedMs = optionalNumber(o, "elapsedMs", 0, duration - 1);
      sequence = optionalNumber(o, "sequence", 1, 8);
      correct = bool(o, "correct");
      points = (int) integer(o, "points", 0, 150);
      if ((option == null) != (elapsedMs == null) || (option == null) != (sequence == null)
          || option == null && (correct || points != 0) || !correct && points != 0)
        throw new JSONException("Invalid answer record");
    }
  }
  public static final class Reveal {
    public final int correctIndex;
    public final String explanation, firstSubmission, fastestCorrect;
    public final List<Answer> answers;
    Reveal(JSONObject o, long duration, Set<String> ids) throws JSONException {
      keys(o, "correctIndex", "explanation", "firstSubmission", "fastestCorrect", "answers");
      correctIndex = (int) integer(o, "correctIndex", 0, 3);
      explanation = string(o, "explanation");
      if (explanation.isEmpty() || explanation.length() > 1000)
        throw new JSONException("Invalid explanation");
      firstSubmission = optionalId(o, "firstSubmission", ids);
      fastestCorrect = optionalId(o, "fastestCorrect", ids);
      JSONArray list = o.getJSONArray("answers");
      if (list.length() != ids.size())
        throw new JSONException("Missing answers");
      List<Answer> found = new ArrayList<>();
      Set<String> unique = new HashSet<>();
      Set<Long> seq = new HashSet<>();
      for (int i = 0; i < list.length(); i++) {
        Answer a = new Answer(list.getJSONObject(i), duration, ids);
        if (!unique.add(a.participantId) || a.sequence != null && !seq.add(a.sequence))
          throw new JSONException("Duplicate answer");
        found.add(a);
      }
      answers = Collections.unmodifiableList(found);
      Answer first = null, fastest = null;
      for (Answer a : found)
        if (a.sequence != null) {
          if (first == null || a.sequence < first.sequence)
            first = a;
          if (a.correct && (fastest == null || a.sequence < fastest.sequence))
            fastest = a;
        }
      if (!Objects.equals(firstSubmission, first == null ? null : first.participantId)
          || !Objects.equals(fastestCorrect, fastest == null ? null : fastest.participantId))
        throw new JSONException("Invalid ordering");
    }
  }
  public static final class Round {
    public final String id, state, text;
    public final int number;
    public final long startedAtMs, deadlineMs;
    public final Long revealUntilMs;
    public final List<String> options, submitted;
    public final Integer ownOption;
    public final Reveal reveal;
    Round(JSONObject o, int count, Set<String> ids, String self) throws JSONException {
      keys(o, "id", "number", "state", "startedAtMs", "deadlineMs", "revealUntilMs", "question",
          "submitted", "ownOption", "reveal");
      id = id(o, "id");
      number = (int) integer(o, "number", 1, count);
      state = string(o, "state");
      if (!Arrays.asList("ROUND_PREPARED", "ROUND_LIVE", "ROUND_REVEALED", "CANCELLED")
              .contains(state))
        throw new JSONException("Invalid phase");
      startedAtMs = integer(o, "startedAtMs", 1, MAX_TIME);
      deadlineMs = integer(o, "deadlineMs", startedAtMs + 1, MAX_TIME);
      if (!Arrays.asList(10000L, 15000L, 20000L, 30000L).contains(deadlineMs - startedAtMs))
        throw new JSONException("Invalid duration");
      revealUntilMs = optionalNumber(o, "revealUntilMs", deadlineMs, MAX_TIME);
      boolean publicQuestion = state.equals("ROUND_LIVE") || state.equals("ROUND_REVEALED");
      List<String> choices = new ArrayList<>();
      if (publicQuestion) {
        JSONObject q = o.getJSONObject("question");
        keys(q, "text", "options");
        text = string(q, "text");
        if (text.length() < 5 || text.length() > 600)
          throw new JSONException("Invalid question");
        JSONArray a = q.getJSONArray("options");
        if (a.length() != 4)
          throw new JSONException("Invalid options");
        for (int i = 0; i < 4; i++) {
          Object v = a.get(i);
          if (!(v instanceof String) || ((String) v).isEmpty() || ((String) v).length() > 240)
            throw new JSONException("Invalid option");
          choices.add((String) v);
        }
        if (new HashSet<>(choices).size() != 4)
          throw new JSONException("Duplicate option");
      } else {
        if (!o.isNull("question"))
          throw new JSONException("Unreleased question");
        text = null;
      }
      options = Collections.unmodifiableList(choices);
      JSONArray a = o.getJSONArray("submitted");
      Set<String> unique = new HashSet<>();
      List<String> submissions = new ArrayList<>();
      if (a.length() > ids.size())
        throw new JSONException("Invalid submissions");
      for (int i = 0; i < a.length(); i++) {
        Object v = a.get(i);
        if (!(v instanceof String) || !ids.contains(v) || !unique.add((String) v))
          throw new JSONException("Invalid participant");
        submissions.add((String) v);
      }
      submitted = Collections.unmodifiableList(submissions);
      Long opt = optionalNumber(o, "ownOption", 0, 3);
      ownOption = opt == null ? null : opt.intValue();
      if ((ownOption != null) != submitted.contains(self)
          || !publicQuestion && !submitted.isEmpty())
        throw new JSONException("Invalid own answer");
      if (state.equals("ROUND_REVEALED")) {
        if (revealUntilMs == null)
          throw new JSONException("Missing reveal deadline");
        reveal = new Reveal(o.getJSONObject("reveal"), deadlineMs - startedAtMs, ids);
      } else {
        if (!o.isNull("reveal") || revealUntilMs != null)
          throw new JSONException("Unrevealed answer key");
        reveal = null;
      }
    }
  }
  public static final class Match {
    public final String phase, finishReason;
    public final int questionCount;
    public final long startedAtMs;
    public final Long finishedAtMs;
    public final Round round;
    public final List<Standing> standings;
    Match(JSONObject o, RoomProtocol.Snapshot snapshot) throws JSONException {
      keys(o, "phase", "questionCount", "startedAtMs", "finishedAtMs", "finishReason", "round",
          "standings");
      phase = string(o, "phase");
      if (!phase.equals(snapshot.state))
        throw new JSONException("Mismatched phase");
      questionCount = (int) integer(o, "questionCount", 5, 20);
      if (!Arrays.asList(5, 10, 15, 20).contains(questionCount)
          || questionCount != snapshot.settings.questionCount)
        throw new JSONException("Invalid count");
      startedAtMs = integer(o, "startedAtMs", 1, MAX_TIME);
      finishedAtMs = optionalNumber(o, "finishedAtMs", startedAtMs, MAX_TIME);
      finishReason = o.isNull("finishReason") ? null : string(o, "finishReason");
      if (finished() != (finishedAtMs != null) || finished() != (finishReason != null)
          || finished()
              && !Arrays.asList("COMPLETED", "INTERRUPTED", "ROOM_EXPIRED").contains(finishReason))
        throw new JSONException("Invalid finish");
      Set<String> ids = new HashSet<>();
      for (RoomProtocol.Member m : snapshot.members) ids.add(m.participantId);
      if (ids.size() < 2 || ids.size() > 8)
        throw new JSONException("Invalid contestant count");
      round = new Round(o.getJSONObject("round"), questionCount, ids, snapshot.selfId);
      if (!finished() && !round.state.equals(phase)
          || finished() && !Arrays.asList("CANCELLED", "ROUND_REVEALED").contains(round.state))
        throw new JSONException("Invalid round phase");
      JSONArray list = o.getJSONArray("standings");
      if (list.length() != ids.size())
        throw new JSONException("Missing standings");
      List<Standing> found = new ArrayList<>();
      Set<String> unique = new HashSet<>();
      for (int i = 0; i < list.length(); i++) {
        Standing s = new Standing(list.getJSONObject(i), questionCount, ids);
        if (!unique.add(s.participantId))
          throw new JSONException("Duplicate standing");
        found.add(s);
      }
      standings = Collections.unmodifiableList(found);
    }
    public boolean finished() {
      return phase.equals("MATCH_FINISHED");
    }
  }
}
