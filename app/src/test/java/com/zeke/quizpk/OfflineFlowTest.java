package com.zeke.quizpk;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.ui.screens.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.LEGACY) @ConscryptMode(ConscryptMode.Mode.OFF)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
public class OfflineFlowTest {
    private Context context;
    @Before public void reset() {
        HostSqliteSupport.install();
        context=RuntimeEnvironment.getApplication(); context.deleteDatabase(QuizDatabase.NAME);
        context.getSharedPreferences("quizpk_appearance",Context.MODE_PRIVATE).edit().clear().commit();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    private ActivityController<OfflineQuizActivity> open() {
        return Robolectric.buildActivity(OfflineQuizActivity.class,
                new Intent(context,OfflineQuizActivity.class).putExtra("category","Science")).setup();
    }
    private void clickChoice(OfflineQuizActivity a,int choice) {
        assertTrue(a.findViewById(R.id.answer_options).findViewWithTag("choice_"+choice).performClick());
    }
    private void selectRadio(OfflineQuizActivity a,int group,String label) {
        RadioGroup g=a.findViewById(group);
        for(int i=0;i<g.getChildCount();i++) if(label.equals(g.getChildAt(i).getTag())) {
            g.check(g.getChildAt(i).getId()); return;
        }
        fail("No radio "+label);
    }
    @Test public void categoryCountsAndSelectionOpenConfiguration() {
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); a.navigateTo(R.id.nav_explore);
            android.widget.GridLayout grid=a.findViewById(R.id.category_grid);
            assertEquals(12,grid.getChildCount()); grid.getChildAt(1).performClick();
            assertTrue(((TextView)grid.getChildAt(1).findViewById(R.id.category_count)).getText().toString().contains("20"));
            a.findViewById(R.id.start_quiz).performClick();
            Intent intent=Shadows.shadowOf(a).getNextStartedActivity();
            assertEquals(OfflineQuizActivity.class.getName(),intent.getComponent().getClassName());
            assertEquals("Science",intent.getStringExtra("category"));
        }
    }
    @Test public void configurationExplainsShortageAndOffersValidAlternatives() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); selectRadio(a,R.id.count_options,"20");
            assertFalse(a.findViewById(R.id.begin_quiz).isEnabled());
            assertEquals(View.VISIBLE,a.findViewById(R.id.use_available).getVisibility());
            assertEquals(View.VISIBLE,a.findViewById(R.id.use_mix).getVisibility());
            a.findViewById(R.id.use_available).performClick(); assertTrue(a.findViewById(R.id.begin_quiz).isEnabled());
            selectRadio(a,R.id.count_options,"20"); a.findViewById(R.id.use_mix).performClick();
            assertEquals(QuizConfig.ALL,a.category); assertTrue(a.findViewById(R.id.begin_quiz).isEnabled());
        }
    }
    @Test public void completeQuizThroughButtonsPersistsResultsAndReview() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.offline_container) instanceof GameplayFragment);
            assertEquals("",((TextView)a.findViewById(R.id.answer_explanation)).getText().toString());
            assertFalse(a.findViewById(R.id.submit_answer).isEnabled());
            String id=a.session.id;
            for(int i=0;i<5;i++) {
                String before=((TextView)a.findViewById(R.id.question_text)).getText().toString();
                clickChoice(a,a.session.current().correct); a.findViewById(R.id.submit_answer).performClick();
                assertEquals(QuizSession.Outcome.CORRECT,a.session.outcome(i));
                assertEquals(View.GONE,a.findViewById(R.id.submit_answer).getVisibility());
                assertEquals(View.VISIBLE,a.findViewById(R.id.answer_feedback).getVisibility());
                assertFalse(((TextView)a.findViewById(R.id.answer_explanation)).getText().toString().isEmpty());
                a.findViewById(R.id.next_question).performClick();
                if(i<4) assertNotEquals(before,((TextView)a.findViewById(R.id.question_text)).getText().toString());
            }
            assertTrue(a.session.complete()); assertEquals(500,new QuizScoring(a.session).score);
            assertEquals(1,a.database.history().size()); assertNull(a.database.active());
            a.findViewById(R.id.review_answers).performClick();
            assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.offline_container) instanceof ReviewFragment);
            assertEquals(5,((android.widget.LinearLayout)a.findViewById(R.id.review_list)).getChildCount());
            a.getOnBackPressedDispatcher().onBackPressed();
            assertEquals(id,a.session.id); assertNotNull(a.findViewById(R.id.play_again));
            a.findViewById(R.id.play_again).performClick(); assertNotNull(a.findViewById(R.id.begin_quiz));
        }
        try(QuizDatabase db=new QuizDatabase(context)) {
            assertEquals(1,db.history().size()); assertEquals(500,new QuizScoring(db.history().get(0)).score);
        }
    }
    @Test public void timerTimeoutAndSkipAreUnansweredWithoutDuplicateScoring() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            clickChoice(a,a.session.current().correct);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(16,TimeUnit.SECONDS);
            assertEquals(QuizSession.Outcome.TIMEOUT,a.session.outcome(0));
            assertEquals(View.VISIBLE,a.findViewById(R.id.next_question).getVisibility());
            a.session.submit(); assertEquals(0,new QuizScoring(a.session).score);
            a.findViewById(R.id.next_question).performClick();
            a.findViewById(R.id.skip_question).performClick();
            assertEquals(QuizSession.Outcome.SKIPPED,a.session.outcome(1));
            assertEquals(0,new QuizScoring(a.session).score);
        }
    }
    @Test public void recreationRetainsSelectionTimerAndRequiresExplicitResume() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            clickChoice(a,1); Shadows.shadowOf(Looper.getMainLooper()).idleFor(2,TimeUnit.SECONDS);
            String id=a.session.id; c.recreate(); a=c.get();
            assertEquals(id,a.session.id); assertEquals(1,a.session.selected(0));
            long time=a.session.remaining();
            assertEquals(View.VISIBLE,a.findViewById(R.id.resume_question).getVisibility());
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(20,TimeUnit.SECONDS);
            assertEquals(time,a.session.remaining()); assertTrue(a.session.pending());
            a.findViewById(R.id.resume_question).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1,TimeUnit.SECONDS);
            assertTrue(a.session.remaining()<time);
        }
    }
    @Test public void backgroundPausePersistsAndResumeDoesNotSpendBackgroundTime() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            c.pause().stop(); long time=a.session.remaining();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(60,TimeUnit.SECONDS);
            c.restart().start().resume().postResume();
            assertEquals(time,a.session.remaining()); assertFalse(a.findViewById(R.id.submit_answer).isEnabled());
            assertEquals(View.VISIBLE,a.findViewById(R.id.resume_question).getVisibility());
            assertEquals(a.session.id,a.database.active().id);
        }
    }
    @Test public void resultsRecreationAndProfileHistorySurviveActivityRelaunch() {
        String id;
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            for(int i=0;i<5;i++) { a.findViewById(R.id.skip_question).performClick(); a.findViewById(R.id.next_question).performClick(); }
            id=a.session.id; c.recreate(); a=c.get(); assertEquals(id,a.session.id);
            assertEquals(5,new QuizScoring(a.session).unanswered); assertNotNull(a.findViewById(R.id.result_metrics));
        }
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a=c.get(); a.navigateTo(R.id.nav_profile);
            androidx.recyclerview.widget.RecyclerView list=a.findViewById(R.id.history_list);
            assertEquals(1,list.getAdapter().getItemCount()); assertEquals(View.GONE,a.findViewById(R.id.history_empty).getVisibility());
        }
        try(ActivityController<OfflineQuizActivity> c=Robolectric.buildActivity(OfflineQuizActivity.class,
                new Intent(context,OfflineQuizActivity.class).putExtra("result",id)).setup()) {
            assertEquals(id,c.get().session.id); assertNotNull(c.get().findViewById(R.id.review_answers));
        }
    }
    @Test public void configurationRecreationRetainsOptionsInLightTheme() {
        context.getSharedPreferences("quizpk_appearance",Context.MODE_PRIVATE).edit().putBoolean("light_theme",true).commit();
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); selectRadio(a,R.id.difficulty_options,"Expert");
            selectRadio(a,R.id.timer_options,"30"); c.recreate(); a=c.get();
            assertTrue(com.zeke.quizpk.ui.theme.ThemePreferences.isLight(a));
            assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_NO, a.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK);
            RadioGroup level=a.findViewById(R.id.difficulty_options), timer=a.findViewById(R.id.timer_options);
            assertEquals("Expert",level.findViewById(level.getCheckedRadioButtonId()).getTag());
            assertEquals("30",timer.findViewById(timer.getCheckedRadioButtonId()).getTag());
        }
    }
    @Test public void twentyQuestionMixHandlesWrongAnswersDoubleTapsAndHiddenExplanations() {
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.category=QuizConfig.ALL;
            selectRadio(a,R.id.difficulty_options,"Expert"); selectRadio(a,R.id.count_options,"20");
            ((android.widget.CompoundButton)a.findViewById(R.id.show_explanations)).setChecked(false);
            a.findViewById(R.id.begin_quiz).performClick();
            assertEquals(20,a.session.questions.size());
            for(int i=0;i<20;i++) {
                assertEquals("",((TextView)a.findViewById(R.id.answer_feedback)).getText().toString());
                for(int j=0;j<4;j++) assertFalse(((TextView)a.findViewById(R.id.answer_options)
                        .findViewWithTag("choice_"+j)).getText().toString().contains("✓"));
                if(i%4<2) {
                    clickChoice(a,(a.session.current().correct+(i%4))%4);
                    a.findViewById(R.id.submit_answer).performClick();
                    int score=new QuizScoring(a.session).score;
                    a.findViewById(R.id.submit_answer).performClick();
                    assertEquals(score,new QuizScoring(a.session).score);
                    assertEquals(i%4==0?QuizSession.Outcome.CORRECT:QuizSession.Outcome.INCORRECT,a.session.outcome(i));
                } else a.findViewById(R.id.skip_question).performClick();
                assertEquals(View.GONE,a.findViewById(R.id.answer_explanation).getVisibility());
                a.findViewById(R.id.next_question).performClick();
            }
            QuizScoring result=new QuizScoring(a.session);
            assertEquals(500,result.score); assertEquals(5,result.correct); assertEquals(5,result.incorrect);
            assertEquals(10,result.unanswered); assertEquals(25,result.accuracy);
            a.findViewById(R.id.review_answers).performClick();
            assertEquals(20,((android.widget.LinearLayout)a.findViewById(R.id.review_list)).getChildCount());
            a.getOnBackPressedDispatcher().onBackPressed(); a.findViewById(R.id.results_home).performClick();
            Intent home=Shadows.shadowOf(a).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(),home.getComponent().getClassName());
            assertTrue(home.getBooleanExtra("open_home",false)); assertTrue(a.isFinishing());
        }
    }
    @Test public void leaveSavesDraftAndNewActivityResumesSameQuiz() {
        String id; long remaining;
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); a.findViewById(R.id.begin_quiz).performClick();
            clickChoice(a,2); Shadows.shadowOf(Looper.getMainLooper()).idleFor(2,TimeUnit.SECONDS);
            a.getOnBackPressedDispatcher().onBackPressed();
            androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)
                    org.robolectric.shadows.ShadowDialog.getLatestDialog();
            id=a.session.id; remaining=a.session.remaining();
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            // AlertDialog dispatches its action through a queued Handler message.
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(a.isFinishing()); assertEquals(0,a.database.history().size());
            assertEquals(remaining,a.database.active().remaining());
        }
        try(ActivityController<OfflineQuizActivity> c=Robolectric.buildActivity(OfflineQuizActivity.class,
                new Intent(context,OfflineQuizActivity.class).putExtra("resume",true)).setup()) {
            assertEquals(id,c.get().session.id); assertEquals(2,c.get().session.selected(0));
            // Opening via Resume starts foreground timing; a queued tick may consume 1 ms.
            assertTrue(c.get().session.remaining()<=remaining);
            assertTrue(c.get().session.remaining()>0);
        }
    }
    @Test @Config(qualifiers="w320dp-h640dp") public void compactLayoutKeepsConfigurationTouchTargetsUsable() {
        RuntimeEnvironment.setFontScale(1.6f);
        try(ActivityController<OfflineQuizActivity> c=open()) {
            OfflineQuizActivity a=c.get(); View root=a.getWindow().getDecorView();
            float density=a.getResources().getDisplayMetrics().density;
            root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(320*density),View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(Math.round(640*density),View.MeasureSpec.EXACTLY));
            root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
            for(int id:new int[]{R.id.count_options,R.id.timer_options}) {
                RadioGroup group=a.findViewById(id);
                for(int i=0;i<4;i++) {
                    View child=group.getChildAt(i);
                    assertTrue(child.getWidth()>=48*density); assertTrue(child.getHeight()>=48*density);
                    assertTrue(child.getRight()<=group.getWidth());
                }
            }
        }
    }
}
