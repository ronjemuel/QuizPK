package com.zeke.quizpk;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;
import android.os.*;
import android.view.View;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.*;
import androidx.lifecycle.*;
import androidx.recyclerview.widget.RecyclerView;
import com.zeke.quizpk.data.remote.RoomBackendClient;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import com.zeke.quizpk.ui.screens.LobbyViewModel;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
public class LobbyRefreshTest {
  private LobbyViewModel model;
  private final LobbyFixtures.Service service = new LobbyFixtures.Service();
  private final AtomicBoolean hold = new AtomicBoolean();
  private CountDownLatch entered, released;
  private volatile boolean ignoreCanceledRead;
  private volatile String authority = LobbyFixtures.INSTANCE;
  private volatile long clockShift;
  private final AtomicReference<LobbyViewModel.Account> login = new AtomicReference<>(
      new LobbyViewModel.Account(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN));
  @Before
  public void setup() {
    resetFactory();
    HostSqliteSupport.install();
    RuntimeEnvironment.getApplication()
        .getSharedPreferences("quizpk_appearance", 0)
        .edit()
        .clear()
        .commit();
  }
  @After
  public void cleanup() {
    if (released != null)
      released.countDown();
    resetFactory();
  }
  private void resetFactory() {
    org.robolectric.util.ReflectionHelpers.setStaticField(
        ViewModelProvider.AndroidViewModelFactory.class, "sInstance", null);
  }
  private ActivityController<MainActivity> activity() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    model = new ViewModelProvider(c.get(), new ViewModelProvider.Factory() {
      @Override
      public <T extends ViewModel> T create(Class<T> type) {
        return type.cast(new LobbyViewModel((Application) RuntimeEnvironment.getApplication(),
            login::get, () -> new RoomBackendClient.Transport() {
              final RoomBackendClient.Transport delegate = service.create();
              @Override
              public String request(String e, String t, String method, String path, String body)
                  throws RoomBackendClient.Failure {
                String response = delegate.request(e, t, method, path, body);
                if (method.equals("GET")) {
                  try {
                    JSONObject json = new JSONObject(response);
                    json.getJSONObject("authority").put("instanceId", authority);
                    json.put("serverTimeMs", LobbyFixtures.TIME + clockShift);
                    response = json.toString();
                  } catch (Exception ex) {
                    throw new AssertionError(ex);
                  }
                }
                if (method.equals("GET") && hold.compareAndSet(true, false)) {
                  entered.countDown();
                  long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
                  for (;;) {
                    try {
                      if (!released.await(
                              Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS))
                        throw new AssertionError("held read timed out");
                      break;
                    } catch (InterruptedException ex) {
                      if (!ignoreCanceledRead) {
                        Thread.currentThread().interrupt();
                        break;
                      }
                    }
                  }
                }
                return response;
              }
              @Override
              public void cancel() {
                delegate.cancel();
              }
            }));
      }
    }).get(LobbyViewModel.class);
    c.get().navigateTo(R.id.nav_multiplayer);
    c.get().findViewById(R.id.mp_online).performClick();
    settle();
    c.get().findViewById(R.id.create_room).performClick();
    return c;
  }
  private void settle() throws Exception {
    long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    while (System.nanoTime() < until) {
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      if (!model.state.getValue().busy)
        return;
      Thread.sleep(10);
    }
    fail("request did not settle");
  }
  private void heldPoll() throws Exception {
    entered = new CountDownLatch(1);
    released = new CountDownLatch(1);
    hold.set(true);
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
    assertTrue(entered.await(5, TimeUnit.SECONDS));
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    assertTrue(model.state.getValue().busy);
  }
  @Test
  public void aBackgroundPollMustNotDisableAnEditorOrMoveItsCursor() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      EditText name = c.get().findViewById(R.id.mp_create_name);
      name.setText("Typing my name");
      name.requestFocus();
      name.setSelection(7);
      heldPoll();
      try {
        assertTrue("Background GET disabled the focused editor", name.isEnabled());
        assertTrue(name.hasFocus());
        assertEquals(7, name.getSelectionStart());
        assertEquals("Typing my name", name.getText().toString());
      } finally {
        released.countDown();
      }
      settle();
    }
  }
  @Test
  public void anUnconfirmedCachedRoomMustNotNavigateAwayFromTheForm() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      EditText name = c.get().findViewById(R.id.mp_create_name);
      name.setText("My unfinished name");
      name.setSelection(3);
      // A lifecycle/connection publication can retain a prior room. It is not
      // a newly confirmed membership transition and must not drive navigation.
      org.robolectric.util.ReflectionHelpers.setField(model, "snapshot",
          new RoomProtocol.Snapshot(
              LobbyFixtures.snapshot(true, "CLASSIC", true, false, 0, 1).toString()));
      model.deactivate();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_create_panel).getVisibility());
      assertEquals(View.GONE, c.get().findViewById(R.id.mp_lobby_panel).getVisibility());
      assertEquals("My unfinished name", name.getText().toString());
      assertEquals(3, name.getSelectionStart());
    }
  }
  private EditText edit(MainActivity a, int id, String text, int start, int end) {
    EditText field = a.findViewById(id);
    field.setText(text);
    field.requestFocus();
    field.setSelection(start, end);
    return field;
  }
  private void measure(MainActivity a) {
    View root = a.getWindow().getDecorView();
    float density = a.getResources().getDisplayMetrics().density;
    root.measure(
        View.MeasureSpec.makeMeasureSpec(Math.round(320 * density), View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(Math.round(640 * density), View.MeasureSpec.EXACTLY));
    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
  }
  private void assertForm(MainActivity a, int panel) {
    assertEquals(View.VISIBLE, a.findViewById(panel).getVisibility());
    assertEquals(View.GONE, a.findViewById(R.id.mp_lobby_panel).getVisibility());
  }
  @Test
  public void continuousThirtySecondsOfComposingTextSurviveSixSlowPolls() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      measure(a);
      EditText name = edit(a, R.id.mp_create_name, "Draft name", 5, 5);
      InputConnection ime = name.onCreateInputConnection(new EditorInfo());
      assertNotNull(ime);
      NestedScrollView scroll = a.findViewById(R.id.mp_scroll);
      scroll.scrollTo(0, 100);
      String expected = "Draft name";
      for (int second = 1; second <= 30; second++) {
        int cursor = name.getSelectionStart();
        String letter = String.valueOf((char) ('a' + second % 26));
        expected = expected.substring(0, cursor) + letter + expected.substring(cursor);
        name.getText().insert(cursor, letter);
        name.setSelection(cursor + 1);
        ime.setComposingRegion(cursor, cursor + 1);
        int oldScroll = scroll.getScrollY();
        if (second % 5 == 0) {
          entered = new CountDownLatch(1);
          released = new CountDownLatch(1);
          hold.set(true);
        }
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        if (second % 5 == 0) {
          assertTrue(entered.await(5, TimeUnit.SECONDS));
          assertTrue(name.isEnabled());
          assertTrue(name.hasFocus());
          measure(a);
          assertEquals(oldScroll, scroll.getScrollY());
          released.countDown();
          settle();
        }
        assertEquals(expected, name.getText().toString());
        assertEquals(cursor + 1, name.getSelectionStart());
        assertEquals(cursor, BaseInputConnection.getComposingSpanStart(name.getText()));
        assertForm(a, R.id.mp_create_panel);
      }
      assertEquals(0, service.posts("room.create"));
      assertTrue(
          service.paths.stream().filter(p -> p.equals("GET /api/rooms/current")).count() >= 7);
    }
  }
  @Test
  public void switchingJoinFieldsAndKeyboardInsetsPreservesBothSelections() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      a.findViewById(R.id.mp_create_back).performClick();
      a.findViewById(R.id.join_room).performClick();
      measure(a);
      EditText name = edit(a, R.id.mp_join_name, "My own name", 2, 5);
      EditText code = edit(a, R.id.mp_join_code, "ABC234", 1, 3);
      for (int height : new int[] {280, 0, 320, 0}) {
        ViewCompat.dispatchApplyWindowInsets(a.findViewById(R.id.app_root),
            new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, height))
                .setVisible(WindowInsetsCompat.Type.ime(), height > 0)
                .build());
        heldPoll();
        assertTrue(code.hasFocus());
        assertTrue(code.isEnabled());
        assertEquals("My own name", name.getText().toString());
        assertEquals(2, name.getSelectionStart());
        assertEquals(5, name.getSelectionEnd());
        assertEquals("ABC234", code.getText().toString());
        assertEquals(1, code.getSelectionStart());
        assertEquals(3, code.getSelectionEnd());
        assertForm(a, R.id.mp_join_panel);
        released.countDown();
        settle();
      }
      name.requestFocus();
      name.setSelection(4);
      heldPoll();
      assertTrue(name.hasFocus());
      assertEquals(4, name.getSelectionStart());
      released.countDown();
      settle();
      assertEquals(0, service.posts("room.join"));
    }
  }
  @Test
  public void reconnectKeepsDraftSettingsAndCursorInsteadOfReinitializingThePage()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      EditText name = edit(a, R.id.mp_create_name, "Unfinished draft", 3, 7);
      ((RadioGroup) a.findViewById(R.id.mp_mode)).check(R.id.mp_moderator);
      ((Spinner) a.findViewById(R.id.mp_category)).setSelection(1);
      ((Spinner) a.findViewById(R.id.mp_count)).setSelection(3);
      service.getCode = "NO_CONNECTION";
      model.retry();
      settle();
      assertTrue(name.isEnabled());
      assertFalse(a.findViewById(R.id.mp_create_submit).isEnabled());
      assertForm(a, R.id.mp_create_panel);
      service.getCode = null;
      model.retry();
      settle();
      assertEquals("Unfinished draft", name.getText().toString());
      assertTrue(name.hasFocus());
      assertEquals(3, name.getSelectionStart());
      assertEquals(7, name.getSelectionEnd());
      assertEquals(
          R.id.mp_moderator, ((RadioGroup) a.findViewById(R.id.mp_mode)).getCheckedRadioButtonId());
      assertEquals(1, ((Spinner) a.findViewById(R.id.mp_category)).getSelectedItemPosition());
      assertEquals("20", ((Spinner) a.findViewById(R.id.mp_count)).getSelectedItem());
      assertForm(a, R.id.mp_create_panel);
      assertEquals(0, service.posts("room.create"));
    }
  }
  @Test
  public void recreationDuringReadPreservesNameRangeModeAndSettings() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      edit(a, R.id.mp_create_name, "My selected draft", 2, 8);
      ((RadioGroup) a.findViewById(R.id.mp_mode)).check(R.id.mp_moderator);
      ((Spinner) a.findViewById(R.id.mp_difficulty)).setSelection(2);
      ((Spinner) a.findViewById(R.id.mp_timer)).setSelection(3);
      heldPoll();
      c.recreate();
      released.countDown();
      settle();
      a = c.get();
      EditText name = a.findViewById(R.id.mp_create_name);
      assertEquals("My selected draft", name.getText().toString());
      assertEquals(2, name.getSelectionStart());
      assertEquals(8, name.getSelectionEnd());
      assertTrue(name.hasFocus());
      assertForm(a, R.id.mp_create_panel);
      assertEquals(
          R.id.mp_moderator, ((RadioGroup) a.findViewById(R.id.mp_mode)).getCheckedRadioButtonId());
      assertEquals("Hard", ((Spinner) a.findViewById(R.id.mp_difficulty)).getSelectedItem());
      assertEquals("30", ((Spinner) a.findViewById(R.id.mp_timer)).getSelectedItem());
    }
  }
  @Test
  public void settingsBackRestoresDestroyedViewWithoutLosingFocusOrScroll() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      measure(a);
      edit(a, R.id.mp_create_name, "Back stack draft", 4, 6);
      ((Spinner) a.findViewById(R.id.mp_category)).setSelection(2);
      NestedScrollView scroll = a.findViewById(R.id.mp_scroll);
      scroll.scrollTo(0, 120);
      int before = scroll.getScrollY();
      a.openSettings();
      a.getOnBackPressedDispatcher().onBackPressed();
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      measure(a);
      settle();
      EditText name = a.findViewById(R.id.mp_create_name);
      assertEquals("Back stack draft", name.getText().toString());
      assertEquals(4, name.getSelectionStart());
      assertEquals(6, name.getSelectionEnd());
      assertTrue(name.hasFocus());
      assertEquals(2, ((Spinner) a.findViewById(R.id.mp_category)).getSelectedItemPosition());
      assertEquals(before, ((NestedScrollView) a.findViewById(R.id.mp_scroll)).getScrollY());
      assertForm(a, R.id.mp_create_panel);
    }
  }
  @Test
  public void savedFragmentDraftRestoresWithAFreshActivityAndModel() throws Exception {
    Fragment.SavedState saved;
    LobbyViewModel first;
    try (ActivityController<MainActivity> c = activity()) {
      first = model;
      edit(c.get(), R.id.mp_create_name, "Cold draft", 1, 4);
      ((Spinner) c.get().findViewById(R.id.mp_count)).setSelection(0);
      saved = c.get().getSupportFragmentManager().saveFragmentInstanceState(
          c.get().getSupportFragmentManager().findFragmentById(R.id.screen_container));
    }
    try (ActivityController<MainActivity> c = activity()) {
      assertNotSame(first, model);
      com.zeke.quizpk.ui.screens.MultiplayerFragment fragment =
          new com.zeke.quizpk.ui.screens.MultiplayerFragment();
      fragment.setInitialSavedState(saved);
      c.get()
          .getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.screen_container, fragment, Integer.toString(R.id.nav_multiplayer))
          .commitNow();
      settle();
      measure(c.get());
      EditText name = c.get().findViewById(R.id.mp_create_name);
      assertEquals("Cold draft", name.getText().toString());
      assertEquals(1, name.getSelectionStart());
      assertEquals(4, name.getSelectionEnd());
      assertEquals("5", ((Spinner) c.get().findViewById(R.id.mp_count)).getSelectedItem());
      assertForm(c.get(), R.id.mp_create_panel);
    }
  }
  @Test
  public void lateCanceledResponseCannotReplaceNewCredentialState() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      EditText name = edit(c.get(), R.id.mp_create_name, "New account draft", 5, 5);
      ignoreCanceledRead = true;
      service.exists = true;
      heldPoll();
      login.set(new LobbyViewModel.Account("https://new.invalid", "y".repeat(64)));
      model.reload();
      service.exists = false;
      released.countDown();
      settle();
      assertEquals("https://new.invalid", model.endpoint());
      assertFalse(model.state.getValue().snapshot.hasRoom());
      assertForm(c.get(), R.id.mp_create_panel);
      assertEquals("New account draft", name.getText().toString());
      assertEquals(5, name.getSelectionStart());
    }
  }
  private void create(MainActivity a) throws Exception {
    edit(a, R.id.mp_create_name, "Draft creator", 4, 4);
    a.findViewById(R.id.mp_create_submit).performClick();
    settle();
    assertEquals(View.VISIBLE, a.findViewById(R.id.mp_lobby_panel).getVisibility());
  }
  @Test
  public void oldRevisionAndRetiredAuthorityCannotResurrectPriorState() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      create(c.get());
      service.revision = 20;
      model.retry();
      settle();
      long latest = model.state.getValue().snapshot.revision;
      service.revision = latest - 5;
      model.retry();
      settle();
      assertEquals("STALE_SNAPSHOT", model.state.getValue().code);
      assertEquals(latest, model.state.getValue().snapshot.revision);
      authority = LobbyFixtures.id(99);
      service.revision = latest + 3;
      model.retry();
      settle();
      assertEquals(authority, model.state.getValue().snapshot.instanceId);
      authority = LobbyFixtures.INSTANCE;
      model.retry();
      settle();
      assertEquals("STALE_SNAPSHOT", model.state.getValue().code);
      assertEquals(LobbyFixtures.id(99), model.state.getValue().snapshot.instanceId);
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_lobby_panel).getVisibility());
    }
  }
  @Test
  public void missingReadWaitsForAuthenticatedCurrentBeforeLeavingLobby() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      create(c.get());
      service.getCode = "ROOM_NOT_FOUND";
      model.retry();
      settle();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_lobby_panel).getVisibility());
      assertFalse(c.get().findViewById(R.id.mp_ready).isEnabled());
      service.getCode = null;
      service.exists = false;
      model.retry();
      settle();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_entry_panel).getVisibility());
      assertEquals(View.GONE, c.get().findViewById(R.id.mp_lobby_panel).getVisibility());
    }
  }
  @Test
  public void rosterChangesUseRowNotificationsAndValidServerTransitionsStillNavigate()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      create(c.get());
      RecyclerView list = c.get().findViewById(R.id.mp_roster);
      int[] changed = {0, 0};
      list.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
          changed[0]++;
        }
        @Override
        public void onItemRangeChanged(int start, int count, Object payload) {
          changed[1] += count;
        }
      });
      model.ready(true);
      settle();
      assertEquals(0, changed[0]);
      assertTrue(changed[1] > 0);
      model.leave();
      settle();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_entry_panel).getVisibility());
    }
  }
  @Test
  public void joinRecreationRestoresTheSecondEditorInsteadOfDefaultingToTheName() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      c.get().findViewById(R.id.mp_create_back).performClick();
      c.get().findViewById(R.id.join_room).performClick();
      edit(c.get(), R.id.mp_join_name, "Join draft", 1, 4);
      edit(c.get(), R.id.mp_join_code, "ABC234", 2, 4);
      c.recreate();
      settle();
      measure(c.get());
      EditText code = c.get().findViewById(R.id.mp_join_code);
      assertTrue(code.hasFocus());
      assertEquals("ABC234", code.getText().toString());
      assertEquals(2, code.getSelectionStart());
      assertEquals(4, code.getSelectionEnd());
      assertEquals(
          "Join draft", ((EditText) c.get().findViewById(R.id.mp_join_name)).getText().toString());
      assertForm(c.get(), R.id.mp_join_panel);
    }
  }
  @Test
  public void aNewEditWinsOverDeferredViewRestoration() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      edit(c.get(), R.id.mp_create_name, "Saved draft", 1, 4);
      c.recreate();
      EditText name = edit(c.get(), R.id.mp_create_name, "New input wins", 2, 6);
      measure(c.get());
      settle();
      assertEquals("New input wins", name.getText().toString());
      assertEquals(2, name.getSelectionStart());
      assertEquals(6, name.getSelectionEnd());
      assertTrue(name.hasFocus());
      assertForm(c.get(), R.id.mp_create_panel);
    }
  }
  @Test
  public void aValidNewRevisionDoesNotRequireWallClockToMoveForward() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      create(c.get());
      long previous = model.state.getValue().snapshot.revision;
      clockShift = -1000;
      service.revision = previous + 5;
      model.retry();
      settle();
      assertTrue(model.state.getValue().connected);
      assertTrue(model.state.getValue().snapshot.revision > previous);
    }
  }
}
