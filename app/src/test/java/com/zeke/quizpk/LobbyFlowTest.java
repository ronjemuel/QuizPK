package com.zeke.quizpk;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.*;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import com.zeke.quizpk.ui.screens.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;
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
public class LobbyFlowTest {
  private Context context;
  private LobbyViewModel model;
  private LobbyFixtures.Service service;
  @Before
  public void setup() {
    resetDefaultFactory();
    HostSqliteSupport.install();
    context = RuntimeEnvironment.getApplication();
    context.deleteDatabase(QuizDatabase.NAME);
    context.getSharedPreferences("quizpk_backend_login", 0).edit().clear().commit();
    context.getSharedPreferences("quizpk_appearance", 0).edit().clear().commit();
    service = new LobbyFixtures.Service();
  }
  @After
  public void resetDefaultFactory() {
    // AndroidX caches an Application in this singleton. Robolectric replaces
    // Application between sandboxes; leaving our lobby Application cached sent
    // a later unchanged M3 retry test's import into this test's temporary DB.
    org.robolectric.util.ReflectionHelpers.setStaticField(
        ViewModelProvider.AndroidViewModelFactory.class, "sInstance", null);
  }
  private ActivityController<MainActivity> activity() {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    model = new ViewModelProvider(c.get(), new ViewModelProvider.Factory() {
      @Override
      public <T extends ViewModel> T create(Class<T> type) {
        return type.cast(new LobbyViewModel((Application) context,
            ()
                -> new LobbyViewModel.Account(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN),
            service));
      }
    }).get(LobbyViewModel.class);
    c.get().navigateTo(R.id.nav_multiplayer);
    return c;
  }
  private void settle() throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    do {
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      if (!model.state.getValue().busy)
        return;
      Thread.sleep(15);
    } while (System.nanoTime() < end);
    fail("Lobby request did not settle");
  }
  private void online(MainActivity a) throws Exception {
    a.findViewById(R.id.mp_online).performClick();
    settle();
    assertTrue(model.state.getValue().connected);
  }
  private void create(MainActivity a, boolean moderator) throws Exception {
    a.findViewById(R.id.create_room).performClick();
    ((EditText) a.findViewById(R.id.mp_create_name)).setText("Alice");
    ((RadioGroup) a.findViewById(R.id.mp_mode))
        .check(moderator ? R.id.mp_moderator : R.id.mp_classic);
    a.findViewById(R.id.mp_create_submit).performClick();
    settle();
  }
  private String text(MainActivity a, int id) {
    return ((TextView) a.findViewById(id)).getText().toString();
  }
  @Test
  public void networkChoiceComesFirstAndLocalCannotStartCloudOrLan() {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_network_panel).getVisibility());
      assertFalse(a.findViewById(R.id.mp_local).isEnabled());
      assertFalse(a.findViewById(R.id.create_room).isEnabled());
      assertTrue(service.paths.isEmpty());
      assertSame(context, new ViewModelProvider(a).get(AiQuizViewModel.class).getApplication());
      a.findViewById(R.id.mp_local).performClick();
      assertTrue(service.paths.isEmpty());
    }
  }
  @Test
  public void classicCreatorCanReadyCopyShareAndCloseOnlyAfterConfirmation() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      create(a, false);
      assertEquals("CLASSIC", model.state.getValue().snapshot.mode);
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_ready).getVisibility());
      assertEquals("Contestants · 1 / 8", text(a, R.id.mp_capacity));
      a.findViewById(R.id.mp_ready).performClick();
      settle();
      assertTrue(model.state.getValue().snapshot.self().ready);
      assertEquals(context.getString(R.string.mp_not_ready), text(a, R.id.mp_ready));
      a.findViewById(R.id.mp_copy_code).performClick();
      assertEquals("ABC234",
          ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE))
              .getPrimaryClip()
              .getItemAt(0)
              .getText()
              .toString());
      a.findViewById(R.id.mp_share_code).performClick();
      Intent chooser = Shadows.shadowOf(a).getNextStartedActivity();
      Intent invitation = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
      String share = invitation.getStringExtra(Intent.EXTRA_TEXT);
      assertTrue(share.contains("ABC234"));
      assertTrue(share.contains(LobbyFixtures.ENDPOINT));
      assertFalse(share.contains(LobbyFixtures.TOKEN));
      a.findViewById(R.id.mp_leave).performClick();
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      androidx.appcompat.app.AlertDialog confirm =
          (androidx.appcompat.app.AlertDialog)
              org.robolectric.shadows.ShadowDialog.getLatestDialog();
      confirm.getButton(-2).performClick();
      assertTrue(service.exists);
      a.findViewById(R.id.mp_leave).performClick();
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      confirm = (androidx.appcompat.app.AlertDialog)
                    org.robolectric.shadows.ShadowDialog.getLatestDialog();
      confirm.getButton(-1).performClick();
      settle();
      assertFalse(service.exists);
      assertFalse(model.state.getValue().snapshot.hasRoom());
      assertEquals(1, service.posts("room.leave"));
    }
  }
  @Test
  public void moderatorIsSeparateFromEightContestantsAndCannotReadyOrAnswer() throws Exception {
    service.extras = 8;
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      create(a, true);
      assertEquals("MODERATOR", model.state.getValue().snapshot.selfRole);
      assertEquals(View.GONE, a.findViewById(R.id.mp_ready).getVisibility());
      assertFalse(model.ready(true));
      assertEquals("Contestants · 8 / 8", text(a, R.id.mp_capacity));
      assertEquals(8, ((RecyclerView) a.findViewById(R.id.mp_roster)).getAdapter().getItemCount());
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_moderator_card).getVisibility());
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_edit_settings).getVisibility());
      assertEquals(0, service.posts("room.ready"));
      assertFalse(
          service.paths.stream().anyMatch(p -> p.contains("answer") || p.contains("start")));
    }
  }
  @Test
  public void joiningValidatesLocallyThenUsesServerModeAndContestantPermissions() throws Exception {
    service.mode = "MODERATOR";
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      a.findViewById(R.id.join_room).performClick();
      a.findViewById(R.id.mp_join_submit).performClick();
      assertNotNull(((TextInputLayout) a.findViewById(R.id.mp_join_name_layout)).getError());
      assertEquals(0, service.posts("room.join"));
      ((EditText) a.findViewById(R.id.mp_join_name)).setText("Player");
      ((EditText) a.findViewById(R.id.mp_join_code)).setText("IO0000");
      a.findViewById(R.id.mp_join_submit).performClick();
      assertNotNull(((TextInputLayout) a.findViewById(R.id.mp_code_layout)).getError());
      assertEquals(0, service.posts("room.join"));
      ((EditText) a.findViewById(R.id.mp_join_code)).setText("abc234");
      a.findViewById(R.id.mp_join_submit).performClick();
      settle();
      assertEquals("CONTESTANT", model.state.getValue().snapshot.selfRole);
      assertFalse(model.state.getValue().snapshot.owner);
      assertEquals(View.GONE, a.findViewById(R.id.mp_edit_settings).getVisibility());
      assertFalse(model.settings(LobbyFixtures.settings(), 1));
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_ready).getVisibility());
    }
  }
  @Test
  public void rapidCreateAndRecreationKeepOneInFlightCommand() throws Exception {
    service.entered = new CountDownLatch(1);
    service.release = new CountDownLatch(1);
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      a.findViewById(R.id.create_room).performClick();
      ((EditText) a.findViewById(R.id.mp_create_name)).setText("Alice");
      a.findViewById(R.id.mp_create_submit).performClick();
      assertTrue(service.entered.await(5, TimeUnit.SECONDS));
      a.findViewById(R.id.mp_create_submit).performClick();
      assertEquals(1, service.posts("room.create"));
      LobbyViewModel retained = model;
      c.recreate();
      a = c.get();
      assertSame(retained, new ViewModelProvider(a).get(LobbyViewModel.class));
      settle();
      if (model.state.getValue().pending) {
        model.retry();
        settle();
      }
      assertEquals(1,
          service.bodies.stream()
              .filter(b -> b.contains("room.create"))
              .map(b -> {
                try {
                  return new JSONObject(b).getString("requestId");
                } catch (Exception e) {
                  throw new AssertionError();
                }
              })
              .distinct()
              .count());
      assertTrue(model.state.getValue().snapshot.hasRoom());
    } finally {
      service.release.countDown();
    }
  }
  @Test
  public void lostCreateReplyRetriesExactlySameBodyAndRecoversAcrossRecreation() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      service.loseReply = true;
      create(a, false);
      assertTrue(model.state.getValue().pending);
      assertFalse(a.findViewById(R.id.mp_create_submit).isEnabled());
      String first = service.bodies.get(0);
      c.recreate();
      a = c.get();
      settle();
      assertTrue(model.state.getValue().pending);
      assertEquals(1, service.posts("room.create"));
      a.findViewById(R.id.mp_retry).performClick();
      settle();
      assertFalse(model.state.getValue().pending);
      assertTrue(model.state.getValue().snapshot.hasRoom());
      assertEquals(2, service.posts("room.create"));
      assertEquals(first,
          service.bodies.stream().filter(b -> b.contains("room.create")).reduce((x, y) -> y).get());
    }
  }
  @Test
  public void processReentryRecoversMembershipWithoutCreatingAnotherRoom() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      online(c.get());
      create(c.get(), true);
    }
    long creates = service.posts("room.create");
    try (ActivityController<MainActivity> c = activity()) {
      online(c.get());
      assertTrue(model.state.getValue().snapshot.hasRoom());
      assertEquals("MODERATOR", model.state.getValue().snapshot.selfRole);
      assertEquals(creates, service.posts("room.create"));
      assertTrue(service.posts("room.heartbeat") >= 1);
    }
  }
  @Test
  public void foregroundHeartbeatStopsOnHomeAndReconnectUsesSameSeat() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      create(a, false);
      String participant = model.state.getValue().snapshot.selfId;
      // Each poll uses a real worker followed by a main-thread callback. Advance
      // one polling interval at a time so virtual time cannot skip the callback
      // that schedules the next poll (a single 16s jump made this assertion race).
      for (int interval = 0; interval < 4; interval++) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
        settle();
      }
      assertTrue(service.posts("room.heartbeat") >= 1);
      a.navigateTo(R.id.nav_home);
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      int count = service.paths.size();
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(60));
      assertEquals(count, service.paths.size());
      a.navigateTo(R.id.nav_multiplayer);
      online(a);
      assertEquals(participant, model.state.getValue().snapshot.selfId);
      assertEquals(1, service.posts("room.create"));
    }
  }
  @Test
  public void connectionLossDisablesStaleControlsAndRevocationClearsPrivateRoom() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      create(a, false);
      service.getCode = "NO_CONNECTION";
      model.retry();
      settle();
      assertFalse(model.state.getValue().connected);
      assertFalse(a.findViewById(R.id.mp_ready).isEnabled());
      assertFalse(a.findViewById(R.id.mp_copy_code).isEnabled());
      assertEquals(context.getString(R.string.mp_network_status), text(a, R.id.mp_status));
      service.getCode = "AUTH_REQUIRED";
      model.retry();
      settle();
      assertNull(model.state.getValue().snapshot);
      assertEquals("", text(a, R.id.mp_invite_code));
      assertEquals(0, ((RecyclerView) a.findViewById(R.id.mp_roster)).getAdapter().getItemCount());
      assertFalse(a.findViewById(R.id.create_room).isEnabled());
    }
  }
  @Test
  public void fullExpiredAndRateErrorsStayTypedAndDoNotFabricateMembership() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      a.findViewById(R.id.join_room).performClick();
      ((EditText) a.findViewById(R.id.mp_join_name)).setText("Player");
      ((EditText) a.findViewById(R.id.mp_join_code)).setText("ABC234");
      for (String code : new String[] {"ROOM_FULL", "ROOM_NOT_FOUND", "ROOM_RATE_LIMITED"}) {
        service.postCode = code;
        a.findViewById(R.id.mp_join_submit).performClick();
        settle();
        assertEquals(code, model.state.getValue().code);
        assertFalse(model.state.getValue().pending);
        assertTrue(
            model.state.getValue().snapshot == null || !model.state.getValue().snapshot.hasRoom());
        if (!code.equals("ROOM_RATE_LIMITED")) {
          service.postCode = null;
          model.retry();
          settle();
        }
      }
      long posts = service.posts("room.join");
      assertFalse(model.retry());
      assertFalse(a.findViewById(R.id.mp_retry).isEnabled());
      assertEquals(posts, service.posts("room.join"));
    }
  }
  @Test
  public void settingsCarryCapturedRevisionAndServerResetsReadiness() throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      online(a);
      create(a, false);
      model.ready(true);
      settle();
      long revision = model.state.getValue().snapshot.revision;
      assertTrue(model.settings(new RoomProtocol.Settings("Science", "Hard", 5, 20), revision));
      settle();
      assertFalse(model.state.getValue().snapshot.self().ready);
      JSONObject body = new JSONObject(
          service.bodies.stream().filter(b -> b.contains("room.settings")).findFirst().get());
      assertEquals(revision, body.getLong("expectedRevision"));
      assertTrue(model.settings(LobbyFixtures.settings(), revision));
      settle();
      assertEquals("REVISION_CONFLICT", model.state.getValue().code);
      assertFalse(model.state.getValue().pending);
    }
  }
  @Test
  public void credentialChangeDropsOldRoomAndPendingActionWithoutCrossIdentityReplay()
      throws Exception {
    AtomicReference<LobbyViewModel.Account> login = new AtomicReference<>(
        new LobbyViewModel.Account(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN));
    try (ActivityController<MainActivity> c =
             Robolectric.buildActivity(MainActivity.class).setup()) {
      model = new ViewModelProvider(c.get(), new ViewModelProvider.Factory() {
        @Override
        public <T extends ViewModel> T create(Class<T> type) {
          return type.cast(new LobbyViewModel((Application) context, login::get, service));
        }
      }).get(LobbyViewModel.class);
      c.get().navigateTo(R.id.nav_multiplayer);
      online(c.get());
      service.loseReply = true;
      create(c.get(), false);
      assertTrue(model.state.getValue().pending);
      login.set(new LobbyViewModel.Account("https://other.invalid", "y".repeat(64)));
      service.exists = false;
      model.reload();
      settle();
      assertFalse(model.state.getValue().pending);
      assertFalse(model.state.getValue().snapshot.hasRoom());
      assertEquals(1, service.posts("room.create"));
    }
  }
  @Test
  @Config(qualifiers = "w320dp-h640dp")
  public void compactLargeFontFormsAndBothThemeLobbiesStayScrollable() throws Exception {
    RuntimeEnvironment.setFontScale(1.6f);
    for (boolean light : new boolean[] {false, true}) {
      context.getSharedPreferences("quizpk_appearance", 0)
          .edit()
          .putBoolean("light_theme", light)
          .commit();
      service = new LobbyFixtures.Service();
      try (ActivityController<MainActivity> c = activity()) {
        MainActivity a = c.get();
        online(a);
        a.findViewById(R.id.create_room).performClick();
        measure(a);
        for (int id :
            new int[] {R.id.mp_create_name, R.id.mp_classic, R.id.mp_moderator, R.id.mp_category,
                R.id.mp_difficulty, R.id.mp_count, R.id.mp_timer, R.id.mp_create_submit})
          assertControl(a, id);
        ((EditText) a.findViewById(R.id.mp_create_name))
            .setText("A long but valid contestant name");
        a.findViewById(R.id.mp_create_submit).performClick();
        settle();
        measure(a);
        for (int id :
            new int[] {R.id.mp_copy_code, R.id.mp_share_code, R.id.mp_ready, R.id.mp_leave})
          assertControl(a, id);
        assertEquals("ABC234", text(a, R.id.mp_invite_code));
        assertEquals(View.VISIBLE, a.findViewById(R.id.mp_lobby_panel).getVisibility());
      }
    }
  }
  private void measure(MainActivity a) {
    View root = a.getWindow().getDecorView();
    float d = a.getResources().getDisplayMetrics().density;
    root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(320 * d), View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(Math.round(640 * d), View.MeasureSpec.EXACTLY));
    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
  }
  private void assertControl(MainActivity a, int id) {
    View v = a.findViewById(id);
    float d = a.getResources().getDisplayMetrics().density;
    assertTrue(v.getWidth() > 0);
    assertTrue(v.getWidth() <= 320 * d);
    assertTrue(v.getHeight() >= 48 * d);
  }
}
