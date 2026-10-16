package com.zeke.quizpk;

import static org.junit.Assert.*;

import android.app.Application;
import android.os.*;
import android.view.View;
import android.widget.TextView;
import androidx.lifecycle.*;
import com.zeke.quizpk.multiplayer.*;
import com.zeke.quizpk.ui.screens.LobbyViewModel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
public class ClassicFlowTest {
  private final ClassicFixtures service = new ClassicFixtures();
  private LobbyViewModel model;
  @Before
  public void setup() {
    reset();
    HostSqliteSupport.install();
  }
  @After
  public void cleanup() {
    reset();
  }
  private void reset() {
    org.robolectric.util.ReflectionHelpers.setStaticField(
        ViewModelProvider.AndroidViewModelFactory.class, "sInstance", null);
  }
  private ActivityController<MainActivity> activity() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    model = new ViewModelProvider(c.get(), new ViewModelProvider.Factory() {
      public <T extends ViewModel> T create(Class<T> type) {
        return type.cast(new LobbyViewModel((Application) RuntimeEnvironment.getApplication(),
            ()
                -> new LobbyViewModel.Account(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN),
            service));
      }
    }).get(LobbyViewModel.class);
    c.get().navigateTo(R.id.nav_multiplayer);
    c.get().findViewById(R.id.mp_online).performClick();
    settle();
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
  private void live(MainActivity a) throws Exception {
    a.findViewById(R.id.mp_start_match).performClick();
    settle();
    service.phase = "ROUND_LIVE";
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
    settle();
    model.retry();
    settle();
  }
  @Test
  public void classicCommandsCarryOnlyRoundChoiceAndHostRevision() throws Exception {
    RoomProtocol.Command start =
        RoomProtocol.startMatch(LobbyFixtures.ROOM, 7, LobbyFixtures.TIME, LobbyFixtures.id(99));
    assertEquals(7, new JSONObject(start.body).getInt("expectedRevision"));
    assertTrue(start.path.endsWith("/match/start"));
    RoomProtocol.Command answer = RoomProtocol.answer(
        LobbyFixtures.ROOM, LobbyFixtures.id(50), 3, LobbyFixtures.TIME, LobbyFixtures.id(98));
    JSONObject payload = new JSONObject(answer.body).getJSONObject("payload");
    assertEquals(2, payload.length());
    assertTrue(payload.has("roundId"));
    assertTrue(payload.has("option"));
    assertThrows(IllegalArgumentException.class,
        ()
            -> RoomProtocol.answer(LobbyFixtures.ROOM, LobbyFixtures.id(50), 4, LobbyFixtures.TIME,
                LobbyFixtures.id(98)));
  }
  @Test
  public void liveParserRejectsUnrevealedKeysPrivateGradesUnknownFieldsAndInvalidRoster()
      throws Exception {
    JSONObject original =
        ClassicFixtures.game("ROUND_LIVE", true, null, 3, LobbyFixtures.TIME + 3000);
    assertNull(new RoomProtocol.Snapshot(original.toString()).match.round.reveal);
    for (int patch = 0; patch < 4; patch++) {
      JSONObject o = new JSONObject(original.toString()),
                 r = o.getJSONObject("room").getJSONObject("match").getJSONObject("round");
      if (patch == 0)
        r.getJSONObject("question").put("correctIndex", 2);
      if (patch == 1)
        r.put("reveal", new JSONObject().put("correctIndex", 2));
      if (patch == 2)
        r.put("privateAnswers", new JSONArray());
      if (patch == 3)
        o.getJSONObject("room")
            .getJSONObject("match")
            .getJSONArray("standings")
            .getJSONObject(0)
            .put("participantId", LobbyFixtures.id(999));
      assertThrows(Exception.class, () -> new RoomProtocol.Snapshot(o.toString()));
    }
  }
  @Test
  public void hostStartPreparesWithoutQuestionThenLiveLocksOneSubmissionUntilServerReveal()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      MainActivity a = c.get();
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_start_match).getVisibility());
      assertTrue(a.findViewById(R.id.mp_start_match).isEnabled());
      a.findViewById(R.id.mp_start_match).performClick();
      settle();
      assertEquals(View.VISIBLE, a.findViewById(R.id.mp_match_panel).getVisibility());
      assertEquals(View.GONE, a.findViewById(R.id.match_option_a).getVisibility());
      service.phase = "ROUND_LIVE";
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
      settle();
      model.retry();
      settle();
      assertEquals(View.VISIBLE, a.findViewById(R.id.match_option_a).getVisibility());
      a.findViewById(R.id.match_option_a).performClick();
      a.findViewById(R.id.match_submit).performClick();
      a.findViewById(R.id.match_submit).performClick();
      settle();
      assertEquals(1, service.answerBodies.size());
      assertFalse(a.findViewById(R.id.match_submit).isEnabled());
      assertEquals(View.GONE, a.findViewById(R.id.match_reveal).getVisibility());
      assertTrue(
          model.state.getValue().snapshot.match.standings.stream().allMatch(p -> p.score == 0));
      service.phase = "ROUND_REVEALED";
      model.retry();
      settle();
      assertEquals(View.VISIBLE, a.findViewById(R.id.match_reveal).getVisibility());
      assertTrue(((TextView) a.findViewById(R.id.match_reveal))
              .getText()
              .toString()
              .contains("SERVER_REVEALED_EXPLANATION"));
    }
  }
  @Test
  public void contestantCannotStartAndReconnectRecoversAnAcceptedAnswerWithoutResubmission()
      throws Exception {
    service.owner = false;
    service.phase = "ROUND_LIVE";
    service.ownOption = 1;
    try (ActivityController<MainActivity> c = activity()) {
      assertFalse(model.startMatch());
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_match_panel).getVisibility());
      assertFalse(c.get().findViewById(R.id.match_submit).isEnabled());
      model.deactivate();
      model.activate();
      settle();
      assertEquals(Integer.valueOf(1), model.state.getValue().snapshot.match.round.ownOption);
      assertTrue(service.answerBodies.isEmpty());
    }
  }
  @Test
  public void uncertainAnswerRetryUsesTheExactEnvelopeEvenAfterFreshAcceptedSnapshot()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      live(c.get());
      service.loseAnswer = true;
      c.get().findViewById(R.id.match_option_b).performClick();
      c.get().findViewById(R.id.match_submit).performClick();
      settle();
      assertTrue(model.state.getValue().pending);
      assertFalse(c.get().findViewById(R.id.match_option_c).isEnabled());
      model.deactivate();
      model.activate();
      settle();
      assertEquals(Integer.valueOf(1), model.state.getValue().snapshot.match.round.ownOption);
      assertTrue(model.retry());
      settle();
      assertEquals(2, service.answerBodies.size());
      assertEquals(service.answerBodies.get(0), service.answerBodies.get(1));
      assertFalse(model.state.getValue().pending);
    }
  }
  @Test
  public void selectedChoiceSurvivesRotationWithoutSubmittingAndOfflineControlsFailClosed()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      live(c.get());
      c.get().findViewById(R.id.match_option_d).performClick();
      c.recreate();
      settle();
      assertTrue(c.get().findViewById(R.id.match_option_d).isSelected());
      assertTrue(service.answerBodies.isEmpty());
      service.error = "NO_CONNECTION";
      model.retry();
      settle();
      assertFalse(c.get().findViewById(R.id.match_submit).isEnabled());
      assertFalse(model.answer(3));
      service.error = null;
      model.retry();
      settle();
      assertTrue(c.get().findViewById(R.id.match_submit).isEnabled());
    }
  }
  @Test
  public void deadlineLocksControlsAndFinalResultsDismissOnlyAfterConfirmedServerResponse()
      throws Exception {
    try (ActivityController<MainActivity> c = activity()) {
      live(c.get());
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16));
      settle();
      assertFalse(model.answer(0));
      service.phase = "MATCH_FINISHED";
      model.retry();
      settle();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.match_done).getVisibility());
      assertTrue(((TextView) c.get().findViewById(R.id.match_phase))
              .getText()
              .toString()
              .contains("Tied winners"));
      c.get().findViewById(R.id.match_done).performClick();
      settle();
      assertEquals(View.VISIBLE, c.get().findViewById(R.id.mp_entry_panel).getVisibility());
    }
  }
  @Test
  public void classicHttpsPathsOptInWithoutAllowingRevealEndpointsOrQueryCredentials()
      throws Exception {
    LobbyProtocolTest.Connection connection =
        new LobbyProtocolTest.Connection(new java.net.URL(LobbyFixtures.ENDPOINT));
    com.zeke.quizpk.data.remote.RoomBackendClient.HttpsTransport transport =
        new com.zeke.quizpk.data.remote.RoomBackendClient.HttpsTransport(url -> connection);
    String path = "/api/rooms/" + LobbyFixtures.ROOM + "/match/answer";
    assertEquals(
        "{}", transport.request(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "POST", path, "{}"));
    assertEquals("classic-v1", connection.getRequestProperty("X-QuizPK-Features"));
    for (String bad :
        List.of(path + "?token=private", "/api/rooms/" + LobbyFixtures.ROOM + "/match/reveal"))
      assertThrows(com.zeke.quizpk.data.remote.RoomBackendClient.Failure.class,
          () -> transport.request(LobbyFixtures.ENDPOINT, LobbyFixtures.TOKEN, "GET", bad, null));
  }
  @Test
  public void bothThemesKeepLongChoicesScrollableAtNarrowWidthAndLargeFont() throws Exception {
    android.content.res.Resources resources = RuntimeEnvironment.getApplication().getResources();
    android.content.res.Configuration original =
        new android.content.res.Configuration(resources.getConfiguration());
    try {
      android.content.res.Configuration large = new android.content.res.Configuration(original);
      large.fontScale = 1.3f;
      resources.updateConfiguration(large, resources.getDisplayMetrics());
      for (boolean light : List.of(false, true)) {
        com.zeke.quizpk.ui.theme.ThemePreferences.setLight(
            RuntimeEnvironment.getApplication(), light);
        service.phase = "LOBBY";
        try (ActivityController<MainActivity> c = activity()) {
          live(c.get());
          View decor = c.get().getWindow().getDecorView();
          ((TextView) c.get().findViewById(R.id.match_question))
              .setText("A long server question ".repeat(20));
          for (int id : new int[] {R.id.match_option_a, R.id.match_option_b, R.id.match_option_c,
                   R.id.match_option_d})
            ((TextView) c.get().findViewById(id))
                .setText("A choice that wraps across several lines ".repeat(5));
          float density = resources.getDisplayMetrics().density;
          decor.measure(
              View.MeasureSpec.makeMeasureSpec(Math.round(320 * density), View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(
                  Math.round(640 * density), View.MeasureSpec.EXACTLY));
          decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
          View button = c.get().findViewById(R.id.match_option_a);
          assertTrue(button.getWidth() > 0);
          assertTrue(button.getWidth() <= 320 * density);
          assertTrue(button.getHeight() >= 48 * density);
          assertTrue(((androidx.core.widget.NestedScrollView) c.get().findViewById(R.id.mp_scroll))
                  .canScrollVertically(1));
          assertTrue(service.answerBodies.isEmpty());
        }
      }
    } finally {
      resources.updateConfiguration(original, resources.getDisplayMetrics());
      com.zeke.quizpk.ui.theme.ThemePreferences.setLight(
          RuntimeEnvironment.getApplication(), false);
    }
  }
}
