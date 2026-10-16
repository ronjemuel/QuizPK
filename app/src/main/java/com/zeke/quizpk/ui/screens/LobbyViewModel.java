package com.zeke.quizpk.ui.screens;

import android.app.Application;
import android.os.*;
import androidx.lifecycle.*;
import com.zeke.quizpk.data.remote.*;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import java.util.*;
import java.util.concurrent.*;

/**
 * Foreground lobby coordinator. Server state is never stored in the solo quiz
 * DB.
 */
public final class LobbyViewModel extends AndroidViewModel {
  public static final class Account {
    public final String endpoint;
    private final String token;
    public Account(String endpoint, String token) {
      this.endpoint = endpoint;
      this.token = token;
    }
    private boolean same(Account other) {
      return other != null && endpoint.equals(other.endpoint) && token.equals(other.token);
    }
  }
  public interface AccountSource {
    Account read();
  }
  public interface TransportFactory {
    RoomBackendClient.Transport create();
  }
  public static final class State {
    public final RoomProtocol.Snapshot snapshot;
    public final boolean busy, actionBusy, connected, pending;
    public final long confirmation;
    public final String code;
    public final long retryAtElapsedMs;
    private State(RoomProtocol.Snapshot snapshot, boolean busy, boolean actionBusy,
        boolean connected, boolean pending, String code, long retryAt, long confirmation) {
      this.snapshot = snapshot;
      this.busy = busy;
      this.actionBusy = actionBusy;
      this.connected = connected;
      this.pending = pending;
      this.code = code;
      retryAtElapsedMs = retryAt;
      this.confirmation = confirmation;
    }
    public boolean canWrite() {
      return connected && !busy && !pending && SystemClock.elapsedRealtime() >= retryAtElapsedMs;
    }
  }
  public final MutableLiveData<State> state =
      new MutableLiveData<>(new State(null, false, false, false, false, "CHOOSE_NETWORK", 0, 0));
  private final AccountSource accounts;
  private final TransportFactory transports;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private Account account;
  private RoomProtocol.Snapshot snapshot;
  private RoomProtocol.Command pending;
  private volatile RoomBackendClient client;
  private Future<?> job;
  private boolean active, busy, actionBusy, connected, recoverCurrent;
  private long confirmation, requestSequence;
  private final Set<String> retiredAuthorities = new HashSet<>();
  private int epoch;
  private String code = "CHOOSE_NETWORK";
  private long serverAt, clockAt, lastHeartbeat = -15000, retryAt, delay = 5000;
  private final Runnable poll = () -> cycle(false);
  public LobbyViewModel(Application app) {
    this(app, () -> {
      BackendCredentials login = new BackendCredentials(app);
      return new Account(login.endpoint(), login.token());
    }, RoomBackendClient.HttpsTransport::new);
  }
  public LobbyViewModel(Application app, AccountSource accounts, TransportFactory transports) {
    super(app);
    this.accounts = accounts;
    this.transports = transports;
  }
  private void publish() {
    state.setValue(new State(
        snapshot, busy, actionBusy, connected, pending != null, code, retryAt, confirmation));
  }
  private boolean accept(RoomProtocol.Snapshot next) {
    if (!next.follows(snapshot) || retiredAuthorities.contains(next.instanceId))
      return false;
    if (snapshot != null && !next.instanceId.equals(snapshot.instanceId))
      retiredAuthorities.add(snapshot.instanceId);
    snapshot = next;
    confirmation++;
    return true;
  }
  public void activate() {
    if (active)
      return;
    active = true;
    Account next;
    try {
      next = accounts.read();
      if (next.endpoint.isEmpty() || next.token.isEmpty())
        throw new IllegalStateException();
      BackendCredentials.validateEndpoint(next.endpoint);
    } catch (RuntimeException e) {
      account = null;
      snapshot = null;
      pending = null;
      retiredAuthorities.clear();
      connected = false;
      code = "SETUP_REQUIRED";
      publish();
      return;
    }
    if (!next.same(account)) {
      epoch++;
      account = next;
      snapshot = null;
      pending = null;
      retiredAuthorities.clear();
      recoverCurrent = true;
      serverAt = 0;
      lastHeartbeat = -15000;
      retryAt = 0;
    }
    code = "CONNECTING";
    connected = false;
    publish();
    cycle(true);
  }
  public void reload() {
    deactivate();
    activate();
  }
  public void deactivate() {
    active = false;
    epoch++;
    requestSequence++;
    main.removeCallbacks(poll);
    if (client != null)
      client.cancel();
    if (job != null)
      job.cancel(true);
    busy = false;
    actionBusy = false;
    connected = false;
    code = pending == null ? "PAUSED" : "COMMAND_PENDING";
    publish();
  }
  private void schedule(long wait) {
    main.removeCallbacks(poll);
    if (active && account != null)
      main.postDelayed(poll, wait);
  }
  private void cycle(boolean bootstrap) {
    if (!active || busy || account == null)
      return;
    if (SystemClock.elapsedRealtime() < retryAt) {
      schedule(retryAt - SystemClock.elapsedRealtime());
      return;
    }
    final int run = epoch;
    final long request = ++requestSequence;
    final Account login = account;
    final RoomProtocol.Snapshot previous = snapshot;
    final boolean lookupCurrent =
        bootstrap || recoverCurrent || previous == null || !previous.hasRoom();
    final boolean heartbeatDue =
        bootstrap || SystemClock.elapsedRealtime() - lastHeartbeat >= 15000;
    busy = true;
    actionBusy = false;
    publish();
    main.removeCallbacks(poll);
    job = worker.submit(() -> {
      try {
        RoomBackendClient api =
            new RoomBackendClient(login.endpoint, login.token, transports.create());
        client = api;
        RoomProtocol.Snapshot next = lookupCurrent ? api.current() : api.room(previous.roomId);
        boolean beat = false;
        if ((next.lobby() || next.inMatch() && !next.match.finished())
            && (heartbeatDue || next.self() != null && !next.self().online)) {
          api.execute(RoomProtocol.member(next.roomId, "heartbeat", false, null, 0,
              next.serverTimeMs, UUID.randomUUID().toString()));
          beat = true;
          next = api.room(next.roomId);
        }
        final RoomProtocol.Snapshot result = next;
        final boolean didBeat = beat;
        main.post(() -> {
          if (run != epoch || request != requestSequence || !active)
            return;
          if (!accept(result)) {
            fail(new RoomBackendClient.Failure("STALE_SNAPSHOT", false), false, false);
            return;
          }
          recoverCurrent = false;
          serverAt = result.serverTimeMs;
          clockAt = SystemClock.elapsedRealtime();
          if (didBeat)
            lastHeartbeat = clockAt;
          busy = false;
          actionBusy = false;
          connected = true;
          retryAt = 0;
          delay = 5000;
          code = pending != null                   ? "COMMAND_PENDING"
              : !result.hasRoom()                  ? "NO_ROOM"
              : result.lobby() || result.inMatch() ? "CONNECTED"
              : result.state.equals("CLOSED")      ? "ROOM_CLOSED"
                                                   : "ROOM_EXPIRED";
          publish();
          if (result.lobby() || result.inMatch() && !result.match.finished() || !result.hasRoom())
            schedule(result.inMatch() ? 1000 : 5000);
        });
      } catch (RoomBackendClient.Failure e) {
        main.post(() -> {
          if (run == epoch && request == requestSequence && active)
            fail(e, false, false);
        });
      } catch (Exception e) {
        main.post(() -> {
          if (run == epoch && request == requestSequence && active)
            fail(new RoomBackendClient.Failure("SERVER_UNAVAILABLE", false), false, false);
        });
      }
    });
  }
  private long authorityTime() {
    return serverAt + (SystemClock.elapsedRealtime() - clockAt);
  }
  public long serverNow() {
    return authorityTime();
  }
  public boolean freshMatchConnection() {
    return active && connected && serverAt > 0 && SystemClock.elapsedRealtime() - clockAt <= 2500;
  }
  private void cancelReadForAction() {
    if (busy && !actionBusy) {
      requestSequence++;
      if (client != null)
        client.cancel();
      if (job != null)
        job.cancel(true);
      busy = false;
      main.removeCallbacks(poll);
    }
  }
  public boolean startMatch() {
    if (!available() || snapshot == null || !snapshot.lobby() || !snapshot.owner
        || !snapshot.matchEnabled)
      return false;
    try {
      return send(RoomProtocol.startMatch(
          snapshot.roomId, snapshot.revision, authorityTime(), UUID.randomUUID().toString()));
    } catch (Exception e) {
      return false;
    }
  }
  public boolean answer(int option) {
    if (!freshMatchConnection() || actionBusy || pending != null || snapshot == null
        || !snapshot.inMatch() || !snapshot.match.phase.equals("ROUND_LIVE")
        || snapshot.match.round.ownOption != null || serverNow() >= snapshot.match.round.deadlineMs)
      return false;
    try {
      RoomProtocol.Command command = RoomProtocol.answer(snapshot.roomId, snapshot.match.round.id,
          option, authorityTime(), UUID.randomUUID().toString());
      cancelReadForAction();
      return send(command);
    } catch (Exception e) {
      return false;
    }
  }
  public String endpoint() {
    return account == null ? "" : account.endpoint;
  }
  private boolean available() {
    return active && account != null && state.getValue().canWrite() && serverAt > 0;
  }
  public boolean create(String mode, String name, RoomProtocol.Settings settings) {
    if (!available())
      return false;
    try {
      return send(
          RoomProtocol.create(mode, name, settings, authorityTime(), UUID.randomUUID().toString()));
    } catch (Exception e) {
      code = "INVALID_COMMAND";
      publish();
      return false;
    }
  }
  public boolean join(String invite, String name) {
    if (!available())
      return false;
    try {
      return send(RoomProtocol.join(invite, name, authorityTime(), UUID.randomUUID().toString()));
    } catch (Exception e) {
      code = "INVALID_COMMAND";
      publish();
      return false;
    }
  }
  public boolean ready(boolean value) {
    return member("ready", value, null, 0);
  }
  public boolean settings(RoomProtocol.Settings value, long revision) {
    return member("settings", false, value, revision);
  }
  public boolean leave() {
    return member("leave", false, null, 0);
  }
  private boolean member(
      String action, boolean ready, RoomProtocol.Settings settings, long revision) {
    if (!available() || snapshot == null
        || !(snapshot.lobby()
            || action.equals("leave") && snapshot.inMatch() && snapshot.match.finished()))
      return false;
    if (action.equals("ready") && !snapshot.selfRole.equals("CONTESTANT")
        || action.equals("settings") && !snapshot.owner)
      return false;
    try {
      return send(RoomProtocol.member(snapshot.roomId, action, ready, settings, revision,
          authorityTime(), UUID.randomUUID().toString()));
    } catch (Exception e) {
      code = "INVALID_COMMAND";
      publish();
      return false;
    }
  }
  private boolean send(RoomProtocol.Command command) {
    if (busy || !active || account == null || SystemClock.elapsedRealtime() < retryAt)
      return false;
    pending = command;
    busy = true;
    actionBusy = true;
    code = "SENDING";
    publish();
    main.removeCallbacks(poll);
    final int run = epoch;
    final long request = ++requestSequence;
    final Account login = account;
    job = worker.submit(() -> {
      boolean acknowledged = false;
      try {
        RoomBackendClient api =
            new RoomBackendClient(login.endpoint, login.token, transports.create());
        client = api;
        String room = api.execute(command);
        acknowledged = true;
        RoomProtocol.Snapshot result =
            command.type.equals("room.leave") ? api.current() : api.room(room);
        main.post(() -> {
          if (run != epoch || request != requestSequence || !active)
            return;
          pending = null;
          if (!accept(result)) {
            fail(new RoomBackendClient.Failure("STALE_SNAPSHOT", false), true, true);
            return;
          }
          recoverCurrent = false;
          serverAt = result.serverTimeMs;
          clockAt = SystemClock.elapsedRealtime();
          busy = false;
          actionBusy = false;
          connected = true;
          retryAt = 0;
          delay = 5000;
          if (command.type.equals("room.create") || command.type.equals("room.join"))
            lastHeartbeat = clockAt;
          code = result.hasRoom() ? "CONNECTED" : "LEFT_ROOM";
          publish();
          if (!result.inMatch() || !result.match.finished())
            schedule(result.inMatch() ? 1000 : 5000);
        });
      } catch (RoomBackendClient.Failure e) {
        final boolean ack = acknowledged;
        main.post(() -> {
          if (run == epoch && request == requestSequence && active)
            fail(e, true, ack);
        });
      } catch (Exception e) {
        final boolean ack = acknowledged;
        main.post(() -> {
          if (run == epoch && request == requestSequence && active)
            fail(new RoomBackendClient.Failure("SERVER_UNAVAILABLE", !ack), true, ack);
        });
      }
    });
    return true;
  }
  private void fail(RoomBackendClient.Failure failure, boolean action, boolean acknowledged) {
    busy = false;
    actionBusy = false;
    connected = false;
    code = failure.code;
    if (action && (acknowledged || !failure.uncertain))
      pending = null;
    if (code.equals("AUTH_REQUIRED") || code.equals("SETUP_REQUIRED")) {
      snapshot = null;
      pending = null;
      publish();
      return;
    }
    if (code.equals("ROOM_NOT_FOUND") || code.equals("ROOM_UNAVAILABLE")) {
      // A failed room lookup is not an authenticated no-membership snapshot.
      // Keep the disabled projection until /current confirms the transition.
      recoverCurrent = true;
    }
    if (failure.retryAfterMs > 0)
      retryAt = SystemClock.elapsedRealtime() + failure.retryAfterMs;
    publish();
    if (!code.equals("ROOMS_UNAVAILABLE"))
      schedule(Math.max(delay, failure.retryAfterMs));
    delay = Math.min(60000, delay * 2);
  }
  public boolean retry() {
    if (busy || !active || account == null || SystemClock.elapsedRealtime() < retryAt)
      return false;
    if (pending != null)
      return send(pending);
    code = "CONNECTING";
    cycle(true);
    return true;
  }
  public boolean dismissTerminal() {
    if (!busy && snapshot != null && snapshot.hasRoom() && !snapshot.lobby()) {
      snapshot = null;
      code = "NO_ROOM";
      publish();
      schedule(5000);
      return true;
    }
    return false;
  }
  @Override
  protected void onCleared() {
    deactivate();
    worker.shutdownNow();
    account = null;
    snapshot = null;
    pending = null;
  }
}
