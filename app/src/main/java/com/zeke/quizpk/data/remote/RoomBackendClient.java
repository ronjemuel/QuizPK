package com.zeke.quizpk.data.remote;

import com.zeke.quizpk.multiplayer.RoomProtocol;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import org.json.*;

/** Individual-token HTTPS adapter; redirects, cleartext and unbounded responses are refused. */
public final class RoomBackendClient {
  public static final class Failure extends Exception {
    public final String code;
    public final boolean uncertain;
    public final long retryAfterMs;
    public Failure(String code, boolean uncertain) {
      this(code, uncertain, 0);
    }
    public Failure(String code, boolean uncertain, long delay) {
      super(code);
      this.code = code;
      this.uncertain = uncertain;
      retryAfterMs = delay;
    }
  }
  public interface Transport {
    String request(String endpoint, String token, String method, String path, String body)
        throws Failure;
    void cancel();
  }
  public interface ConnectionFactory {
    HttpsURLConnection open(URL url) throws IOException;
  }
  public static final class HttpsTransport implements Transport {
    private final ConnectionFactory factory;
    private volatile HttpsURLConnection active;
    private volatile boolean canceled;
    public HttpsTransport() {
      this(url -> (HttpsURLConnection) url.openConnection());
    }
    public HttpsTransport(ConnectionFactory factory) {
      this.factory = factory;
    }
    @Override
    public String request(String endpoint, String token, String method, String path, String body)
        throws Failure {
      boolean writing = method.equals("POST");
      try {
        if (canceled || Thread.currentThread().isInterrupted())
          throw new Failure("INTERRUPTED", writing);
        String origin = BackendCredentials.validateEndpoint(endpoint);
        if (!token.matches("[A-Za-z0-9_-]{43,128}")
            || !path.matches("/api/rooms(?:/(?:current|join|[0-9a-f-]{36}(?:/"
                             + "(?:heartbeat|leave|ready|settings|match/(?:start|answer)))?))?"))
          throw new Failure("AUTH_REQUIRED", false);
        HttpsURLConnection c = factory.open(new URL(origin + path));
        active = c;
        if (canceled || Thread.currentThread().isInterrupted())
          throw new Failure("INTERRUPTED", writing);
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(method);
        c.setUseCaches(false);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("X-QuizPK-Features", "classic-v1");
        if (writing) {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          if (bytes.length > 4096)
            throw new Failure("INVALID_COMMAND", false);
          c.setDoOutput(true);
          c.setRequestProperty("Content-Type", "application/json");
          c.setFixedLengthStreamingMode(bytes.length);
          try (OutputStream out = c.getOutputStream()) {
            out.write(bytes);
          }
        }
        int status = c.getResponseCode();
        String response;
        InputStream stream = status == 200 ? c.getInputStream() : c.getErrorStream();
        if (stream == null)
          response = "";
        else
          try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int n;
            while ((n = in.read(buffer)) != -1) {
              if (canceled || Thread.currentThread().isInterrupted())
                throw new Failure("INTERRUPTED", writing);
              if (out.size() + n > 32768)
                throw new Failure("MALFORMED_RESPONSE", writing);
              out.write(buffer, 0, n);
            }
            response = out.toString("UTF-8");
          }
        return decodeHttp(status, response, c.getHeaderField("Retry-After"), writing);
      } catch (Failure e) {
        throw e;
      } catch (SSLException e) {
        throw new Failure("SECURE_CONNECTION_FAILED", writing);
      } catch (SocketTimeoutException e) {
        throw new Failure("TIMEOUT", writing);
      } catch (IOException e) {
        throw new Failure(canceled ? "INTERRUPTED" : "NO_CONNECTION", writing);
      } catch (RuntimeException e) {
        throw new Failure("SETUP_REQUIRED", false);
      } finally {
        HttpsURLConnection c = active;
        if (c != null)
          c.disconnect();
        active = null;
      }
    }
    @Override
    public void cancel() {
      canceled = true;
      HttpsURLConnection c = active;
      if (c != null)
        c.disconnect();
    }
  }
  private static final Set<String> CODES = new HashSet<>(
      Arrays.asList("AUTH_REQUIRED", "FORBIDDEN", "ROOM_NOT_FOUND", "NOT_FOUND", "ROOM_FULL",
          "ALREADY_IN_ROOM", "HOST_UNAVAILABLE", "HEARTBEAT_REQUIRED", "REVISION_CONFLICT",
          "IDEMPOTENCY_CONFLICT", "COMMAND_EXPIRED", "ROOM_UNAVAILABLE", "ROOM_RATE_LIMITED",
          "ROOM_LIMIT", "INVALID_COMMAND", "INVALID_MODE", "INVALID_SETTINGS", "INVALID_REVISION",
          "PROTOCOL_UNSUPPORTED", "CODE_UNAVAILABLE", "SERVER_UNAVAILABLE", "HTTPS_REQUIRED",
          "CLASSIC_UNAVAILABLE", "TOO_FEW_CONTESTANTS", "PLAYERS_NOT_READY", "INSUFFICIENT_QUESTIONS",
          "MATCH_IN_PROGRESS", "ROUND_NOT_LIVE", "ANSWER_TOO_LATE", "ANSWER_ALREADY_ACCEPTED", "STALE_ROUND", "UPDATE_REQUIRED"));
  public static String decodeHttp(int status, String text, String retryAfter, boolean writing)
      throws Failure {
    if (status == 200)
      return text;
    String code = status == 401 ? "AUTH_REQUIRED"
        : status == 403         ? "FORBIDDEN"
        : status == 404         ? "ROOMS_UNAVAILABLE"
        : status == 429         ? "ROOM_RATE_LIMITED"
                                : "SERVER_UNAVAILABLE";
    try {
      JSONObject o = RoomProtocol.json(text);
      if (o.opt("protocolVersion") instanceof Integer && o.getInt("protocolVersion") == 1
          && CODES.contains(o.getJSONObject("error").getString("code")))
        code = o.getJSONObject("error").getString("code");
    } catch (Exception ignored) { /* Never render response bodies. */
    }
    if (code.equals("NOT_FOUND"))
      code = "ROOMS_UNAVAILABLE";
    long delay = 0;
    if (status == 429) {
      delay = 60000;
      try {
        delay = Math.min(300000, Math.max(60000, Long.parseLong(retryAfter) * 1000));
      } catch (Exception ignored) {
      }
    }
    throw new Failure(code, writing && (status >= 500 || status >= 300 && status < 400), delay);
  }
  private final Transport transport;
  private final String endpoint, token;
  public RoomBackendClient(String endpoint, String token, Transport transport) {
    this.endpoint = BackendCredentials.validateEndpoint(endpoint);
    this.token = token;
    this.transport = transport;
  }
  private RoomProtocol.Snapshot get(String path) throws Failure {
    try {
      return new RoomProtocol.Snapshot(transport.request(endpoint, token, "GET", path, null));
    } catch (JSONException | IllegalArgumentException e) {
      throw new Failure("MALFORMED_RESPONSE", false);
    }
  }
  public RoomProtocol.Snapshot current() throws Failure {
    return get("/api/rooms/current");
  }
  public RoomProtocol.Snapshot room(String id) throws Failure {
    if (!RoomProtocol.uuid(id))
      throw new Failure("INVALID_COMMAND", false);
    RoomProtocol.Snapshot snapshot = get("/api/rooms/" + id);
    if (!id.equals(snapshot.roomId))
      throw new Failure("MALFORMED_RESPONSE", false);
    return snapshot;
  }
  public String execute(RoomProtocol.Command command) throws Failure {
    try {
      return RoomProtocol.receipt(
          transport.request(endpoint, token, "POST", command.path, command.body), command);
    } catch (JSONException | IllegalArgumentException e) {
      throw new Failure("MALFORMED_RESPONSE", true);
    }
  }
  public void cancel() {
    transport.cancel();
  }
}
