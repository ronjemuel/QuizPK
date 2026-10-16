package com.zeke.quizpk.multiplayer;

import java.text.Normalizer;
import java.util.*;
import org.json.*;

/** Transport-neutral v1 lobby codec. Contains no cloud identity or gameplay authority. */
public final class RoomProtocol {
  private RoomProtocol() {}
  public static final int VERSION = 1;
  public static final String[] CATEGORIES = {"All Categories", "General", "Science", "History",
      "Geography", "Technology", "Sports", "Movies", "Music", "Literature", "Animals", "Food",
      "Space"};
  public static final String[] DIFFICULTIES = {"Easy", "Normal", "Hard", "Expert"};
  public static boolean uuid(String value) {
    return value != null
        && value.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
  }
  // Match ECMAScript String.trim used by the v1 authority. Java trim also drops
  // NUL and other C0 controls; accepting those would violate the common codec.
  private static boolean boundarySpace(char c) {
    return c >= 0x09 && c <= 0x0d || c == ' ' || c == '\u00a0' || c == '\u1680'
        || c >= '\u2000' && c <= '\u200a' || c == '\u2028' || c == '\u2029' || c == '\u202f'
        || c == '\u205f' || c == '\u3000' || c == '\ufeff';
  }
  private static String normalized(String input) {
    String value = Normalizer.normalize(input, Normalizer.Form.NFKC);
    int start = 0, end = value.length();
    while (start < end && boundarySpace(value.charAt(start))) start++;
    while (end > start && boundarySpace(value.charAt(end - 1))) end--;
    return value.substring(start, end);
  }
  public static String name(String input) {
    if (input == null)
      throw new IllegalArgumentException();
    String clean = normalized(input);
    if (clean.isEmpty() || clean.length() > 40
        || clean.codePoints().anyMatch(c
            -> Character.getType(c) == Character.CONTROL
                || Character.getType(c) == Character.FORMAT))
      throw new IllegalArgumentException();
    return clean;
  }
  public static String code(String input) {
    String value = normalized(input == null ? "" : input).toUpperCase(Locale.ROOT);
    if (!value.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}"))
      throw new IllegalArgumentException();
    return value;
  }
  static void keys(JSONObject o, String... expected) throws JSONException {
    Set<String> actual = new HashSet<>();
    Iterator<String> it = o.keys();
    while (it.hasNext()) actual.add(it.next());
    if (!actual.equals(new HashSet<>(Arrays.asList(expected))))
      throw new JSONException("Invalid fields");
  }
  static String string(JSONObject o, String key) throws JSONException {
    Object value = o.get(key);
    if (!(value instanceof String))
      throw new JSONException("Invalid string");
    return (String) value;
  }
  static boolean bool(JSONObject o, String key) throws JSONException {
    Object value = o.get(key);
    if (!(value instanceof Boolean))
      throw new JSONException("Invalid boolean");
    return (Boolean) value;
  }
  static long integer(JSONObject o, String key, long min, long max) throws JSONException {
    Object value = o.get(key);
    if (!(value instanceof Integer || value instanceof Long))
      throw new JSONException("Invalid integer");
    long n = ((Number) value).longValue();
    if (n < min || n > max)
      throw new JSONException("Invalid range");
    return n;
  }
  static String id(JSONObject o, String key) throws JSONException {
    String value = string(o, key);
    if (!uuid(value))
      throw new JSONException("Invalid ID");
    return value;
  }
  public static JSONObject json(String text) throws JSONException {
    JSONTokener tokener = new JSONTokener(text);
    Object value = tokener.nextValue();
    if (!(value instanceof JSONObject) || tokener.nextClean() != 0)
      throw new JSONException("Invalid JSON");
    return (JSONObject) value;
  }
  public static final class Settings {
    public final String category, difficulty;
    public final int questionCount, timerSeconds;
    public Settings(String category, String difficulty, int count, int seconds) {
      if (!Arrays.asList(CATEGORIES).contains(category)
          || !Arrays.asList(DIFFICULTIES).contains(difficulty)
          || !Arrays.asList(5, 10, 15, 20).contains(count)
          || !Arrays.asList(10, 15, 20, 30).contains(seconds))
        throw new IllegalArgumentException();
      this.category = category;
      this.difficulty = difficulty;
      questionCount = count;
      timerSeconds = seconds;
    }
    public JSONObject toJson() throws JSONException {
      return new JSONObject()
          .put("category", category)
          .put("difficulty", difficulty)
          .put("questionCount", questionCount)
          .put("timerSeconds", timerSeconds);
    }
    static Settings parse(JSONObject o) throws JSONException {
      keys(o, "category", "difficulty", "questionCount", "timerSeconds");
      return new Settings(string(o, "category"), string(o, "difficulty"),
          (int) integer(o, "questionCount", 5, 20), (int) integer(o, "timerSeconds", 10, 30));
    }
  }
  public static final class Command {
    public final String type, path, requestId, body;
    private Command(String type, String path, JSONObject payload, long time, String requestId,
        Long revision) throws JSONException {
      if (!uuid(requestId) || time <= 0 || time > 9007199254740991L)
        throw new IllegalArgumentException();
      this.type = type;
      this.path = path;
      this.requestId = requestId;
      JSONObject o = new JSONObject()
                         .put("protocolVersion", VERSION)
                         .put("requestId", requestId)
                         .put("issuedAtMs", time)
                         .put("type", type)
                         .put("payload", payload);
      if (revision != null) {
        if (revision < 1)
          throw new IllegalArgumentException();
        o.put("expectedRevision", revision);
      }
      body = o.toString();
    }
  }
  public static Command create(String mode, String displayName, Settings settings, long time,
      String requestId) throws JSONException {
    if (!Arrays.asList("CLASSIC", "MODERATOR").contains(mode))
      throw new IllegalArgumentException();
    return new Command("room.create", "/api/rooms",
        new JSONObject()
            .put("mode", mode)
            .put("displayName", name(displayName))
            .put("settings", settings.toJson()),
        time, requestId, null);
  }
  public static Command join(String invite, String displayName, long time, String requestId)
      throws JSONException {
    return new Command("room.join", "/api/rooms/join",
        new JSONObject().put("code", code(invite)).put("displayName", name(displayName)), time,
        requestId, null);
  }
  public static Command member(String roomId, String action, boolean ready, Settings settings,
      long revision, long time, String requestId) throws JSONException {
    if (!uuid(roomId) || !Arrays.asList("heartbeat", "leave", "ready", "settings").contains(action))
      throw new IllegalArgumentException();
    JSONObject payload = action.equals("settings") ? settings.toJson() : new JSONObject();
    if (action.equals("ready"))
      payload.put("ready", ready);
    return new Command("room." + action, "/api/rooms/" + roomId + "/" + action, payload, time,
        requestId, action.equals("settings") ? revision : null);
  }
  public static String receipt(String text, Command command) throws JSONException {
    JSONObject o = json(text);
    keys(o, "protocolVersion", "requestId", "result");
    if (integer(o, "protocolVersion", 1, 1) != 1
        || !command.requestId.equals(string(o, "requestId")))
      throw new JSONException("Invalid receipt");
    JSONObject result = o.getJSONObject("result");
    if (command.type.equals("room.create")) {
      keys(result, "roomId", "participantId", "inviteCode");
      code(string(result, "inviteCode"));
    } else
      keys(result, "roomId", "participantId");
    id(result, "participantId");
    return id(result, "roomId");
  }
  public static Command startMatch(String roomId, long revision, long time, String requestId)
      throws JSONException {
    if (!uuid(roomId))
      throw new IllegalArgumentException();
    return new Command("match.start", "/api/rooms/" + roomId + "/match/start", new JSONObject(),
        time, requestId, revision);
  }
  public static Command answer(
      String roomId, String roundId, int option, long time, String requestId) throws JSONException {
    if (!uuid(roomId) || !uuid(roundId) || option < 0 || option > 3)
      throw new IllegalArgumentException();
    return new Command("match.answer", "/api/rooms/" + roomId + "/match/answer",
        new JSONObject().put("roundId", roundId).put("option", option), time, requestId, null);
  }
  public static final class Member {
    public final String participantId, role, displayName;
    public final boolean ready, online;
    private Member(JSONObject o) throws JSONException {
      keys(o, "participantId", "role", "displayName", "ready", "online");
      participantId = id(o, "participantId");
      role = string(o, "role");
      displayName = name(string(o, "displayName"));
      ready = bool(o, "ready");
      online = bool(o, "online");
      if (!Arrays.asList("CONTESTANT", "MODERATOR").contains(role) || !online && ready
          || role.equals("MODERATOR") && ready)
        throw new JSONException("Invalid member");
    }
  }
  public static final class Snapshot {
    public final long serverTimeMs, revision, expiresAtMs;
    public final String instanceId, roomId, mode, state, inviteCode, selfId, selfRole;
    public final boolean owner, hostOnline;
    public final boolean matchEnabled;
    public final MatchProtocol.Match match;
    public final Settings settings;
    public final List<Member> members;
    public Snapshot(String text) throws JSONException {
      JSONObject o = json(text);
      integer(o, "protocolVersion", 1, 1);
      serverTimeMs = integer(o, "serverTimeMs", 1, 9007199254740991L);
      JSONObject authority = o.getJSONObject("authority");
      keys(authority, "network", "instanceId");
      if (!string(authority, "network").equals("ONLINE"))
        throw new JSONException("Wrong authority");
      instanceId = id(authority, "instanceId");
      if (o.isNull("room")) {
        keys(o, "protocolVersion", "serverTimeMs", "authority", "room");
        roomId = mode = state = inviteCode = selfId = selfRole = null;
        owner = hostOnline = false;
        matchEnabled = false;
        match = null;
        settings = null;
        members = Collections.emptyList();
        revision = expiresAtMs = 0;
        return;
      }
      keys(o, "protocolVersion", "serverTimeMs", "authority", "room", "self", "capabilities");
      JSONObject room = o.getJSONObject("room");
      roomId = id(room, "id");
      mode = string(room, "mode");
      state = string(room, "state");
      if (!Arrays.asList("CLASSIC", "MODERATOR").contains(mode)
          || !Arrays
              .asList("LOBBY", "CLOSED", "EXPIRED", "ROUND_PREPARED", "ROUND_LIVE",
                  "ROUND_REVEALED", "MATCH_FINISHED")
              .contains(state))
        throw new JSONException("Unsupported room");
      revision = integer(room, "revision", 1, 9007199254740991L);
      expiresAtMs = integer(room, "expiresAtMs", 1, 9007199254740991L);
      integer(room, "maxContestants", 8, 8);
      integer(room, "minContestants", 2, 2);
      hostOnline = bool(room, "hostOnline");
      boolean playing = state.startsWith("ROUND_") || state.equals("MATCH_FINISHED");
      if (lobby() || playing) {
        if (playing)
          keys(room, "id", "mode", "state", "revision", "expiresAtMs", "inviteCode", "settings",
              "maxContestants", "minContestants", "hostOnline", "members", "match");
        else
          keys(room, "id", "mode", "state", "revision", "expiresAtMs", "inviteCode", "settings",
              "maxContestants", "minContestants", "hostOnline", "members");
        inviteCode = code(string(room, "inviteCode"));
        settings = Settings.parse(room.getJSONObject("settings"));
      } else {
        keys(room, "id", "mode", "state", "revision", "expiresAtMs", "maxContestants",
            "minContestants", "hostOnline", "members");
        inviteCode = null;
        settings = null;
        if (hostOnline)
          throw new JSONException("Invalid terminal presence");
      }
      JSONObject self = o.getJSONObject("self");
      keys(self, "participantId", "role", "isOwner");
      selfId = id(self, "participantId");
      selfRole = string(self, "role");
      owner = bool(self, "isOwner");
      if (!Arrays.asList("CONTESTANT", "MODERATOR").contains(selfRole)
          || mode.equals("CLASSIC") && !selfRole.equals("CONTESTANT")
          || mode.equals("MODERATOR") && (owner != selfRole.equals("MODERATOR")))
        throw new JSONException("Invalid role");
      JSONObject capabilities = o.getJSONObject("capabilities");
      keys(capabilities, "lobby", "matches", "localHost");
      matchEnabled = bool(capabilities, "matches");
      if (!bool(capabilities, "lobby") || matchEnabled && !mode.equals("CLASSIC")
          || playing && !matchEnabled || bool(capabilities, "localHost"))
        throw new JSONException("Unsupported capabilities");
      JSONArray list = room.getJSONArray("members");
      List<Member> found = new ArrayList<>();
      Set<String> ids = new HashSet<>();
      int contestants = 0, moderators = 0;
      boolean own = false;
      if (list.length() > 9)
        throw new JSONException("Capacity exceeded");
      for (int i = 0; i < list.length(); i++) {
        Member member = new Member(list.getJSONObject(i));
        if (!ids.add(member.participantId))
          throw new JSONException("Duplicate seat");
        if (member.role.equals("CONTESTANT"))
          contestants++;
        else
          moderators++;
        if (member.participantId.equals(selfId)) {
          if (!member.role.equals(selfRole))
            throw new JSONException("Role mismatch");
          own = true;
        }
        found.add(member);
      }
      if ((lobby() || playing)
              && (!own || contestants > 8 || moderators != (mode.equals("MODERATOR") ? 1 : 0))
          || !lobby() && !playing && !found.isEmpty())
        throw new JSONException("Invalid roster");
      members = Collections.unmodifiableList(found);
      match = playing ? new MatchProtocol.Match(room.getJSONObject("match"), this) : null;
    }
    public boolean lobby() {
      return "LOBBY".equals(state);
    }
    public boolean hasRoom() {
      return roomId != null;
    }
    public boolean inMatch() {
      return match != null;
    }
    public Member self() {
      for (Member member : members)
        if (member.participantId.equals(selfId))
          return member;
      return null;
    }
    public boolean follows(Snapshot previous) {
      return previous == null || !Objects.equals(roomId, previous.roomId)
          || !instanceId.equals(previous.instanceId) || revision >= previous.revision;
    }
  }
}
