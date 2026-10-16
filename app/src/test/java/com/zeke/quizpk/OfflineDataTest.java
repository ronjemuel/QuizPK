package com.zeke.quizpk;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.domain.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.*;

/** Real Android SQLite helper and domain logic in a SDK-34 JVM model. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.LEGACY) @ConscryptMode(ConscryptMode.Mode.OFF)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
public class OfflineDataTest {
    private Context context;
    private QuizDatabase db;
    @Before public void setup() {
        HostSqliteSupport.install();
        context=RuntimeEnvironment.getApplication(); context.deleteDatabase(QuizDatabase.NAME);
        db=new QuizDatabase(context);
    }
    @After public void close() { db.close(); }
    private QuizConfig config(String category, String level, int count, boolean random) {
        return new QuizConfig(category,level,count,15,true,random);
    }
    private QuizSession session() {
        QuizConfig config=config("General","Easy",5,false);
        return new QuizSession(config,db.select(config,new Random(1)),1000);
    }
    private void finishAllCorrect(QuizSession s) {
        for(int i=0;i<s.questions.size();i++) {
            s.elapse(1000); s.select(s.current().correct); assertTrue(s.submit());
            assertTrue(s.next(2000+i));
        }
    }
    @Test public void seedHasAllCategoriesLevelsAndValidDistinctOptions() {
        assertEquals(240,db.count(QuizConfig.ALL,null));
        Set<String> ids=new HashSet<>(), prompts=new HashSet<>();
        for(String cat:QuizConfig.CATEGORIES) {
            assertEquals(20,db.count(cat,null));
            for(String level:QuizConfig.DIFFICULTIES) {
                assertEquals(5,db.count(cat,level));
                for(Question q:db.select(config(cat,level,5,false),new Random(1))) {
                    assertTrue(ids.add(q.id)); assertTrue(prompts.add(q.text.toLowerCase(Locale.ROOT)));
                    assertEquals(4,new HashSet<>(q.options).size()); assertTrue(q.source.startsWith("https://"));
                    assertFalse(q.explanation.isEmpty()); assertTrue(q.correct>=0 && q.correct<4);
                }
            }
        }
        assertEquals(240,ids.size());
    }
    @Test public void initializationIsIdempotentAfterDatabaseReopen() {
        db.count(null,null); db.close(); db=new QuizDatabase(context);
        assertEquals(240,db.count(null,null));
    }
    @Test public void filteringReturnsOnlyMatchingCategoryAndDifficulty() {
        for(Question q:db.select(config("Science","Hard",5,false),new Random(2))) {
            assertEquals("Science",q.category); assertEquals("Hard",q.difficulty);
        }
        assertEquals(60,db.count(QuizConfig.ALL,"Expert"));
    }
    @Test public void randomSelectionNeverRepeatsAndVariesAcrossSeeds() {
        QuizConfig c=config(QuizConfig.ALL,"Normal",20,true);
        Set<String> orders=new HashSet<>();
        for(int n=0;n<15;n++) {
            Set<String> ids=new HashSet<>(); StringBuilder order=new StringBuilder();
            for(Question q:db.select(c,new Random(n))) { assertTrue(ids.add(q.id)); order.append(q.id); }
            assertEquals(20,ids.size()); orders.add(order.toString());
        }
        assertTrue(orders.size()>1);
    }
    @Test public void nonRandomOrderIsStable() {
        QuizConfig c=config(QuizConfig.ALL,"Easy",20,false);
        List<Question> a=db.select(c,new Random(1)), b=db.select(c,new Random(2));
        for(int i=0;i<a.size();i++) assertEquals(a.get(i).id,b.get(i).id);
    }
    @Test(expected=IllegalArgumentException.class) public void insufficientPoolIsRejected() {
        db.select(config("Science","Expert",10,true),new Random());
    }
    @Test public void selectionAloneDoesNotScoreAndDuplicateSubmissionIsRejected() {
        QuizSession s=session(); s.select(s.current().correct);
        assertEquals(0,new QuizScoring(s).score); assertTrue(s.submit());
        assertFalse(s.submit()); assertFalse(s.skip()); assertFalse(s.select(0));
        assertEquals(100,new QuizScoring(s).score);
    }
    @Test public void incorrectAnswerScoresZero() {
        QuizSession s=session(); s.select((s.current().correct+1)%4); assertTrue(s.submit());
        assertEquals(QuizSession.Outcome.INCORRECT,s.outcome(0));
        assertEquals(1,new QuizScoring(s).incorrect); assertEquals(0,new QuizScoring(s).score);
    }
    @Test public void skipDiscardsUnsubmittedChoiceAndCannotScore() {
        QuizSession s=session(); s.select(s.current().correct); assertTrue(s.skip());
        assertEquals(-1,s.selected(0)); assertFalse(s.submit());
        assertEquals(QuizSession.Outcome.SKIPPED,s.outcome(0));
    }
    @Test public void timeoutLocksEvenPreviouslySelectedCorrectChoice() {
        QuizSession s=session(); s.select(s.current().correct); s.elapse(15000);
        assertEquals(0,s.remaining()); assertEquals(QuizSession.Outcome.TIMEOUT,s.outcome(0));
        assertFalse(s.submit()); assertFalse(s.skip()); assertEquals(0,new QuizScoring(s).score);
    }
    @Test public void answeredQuestionTimerStopsButForegroundDurationIncludesFeedback() {
        QuizSession s=session(); s.elapse(2000); s.select(s.current().correct); s.submit(); s.elapse(3000);
        assertEquals(2000,s.answerMillis(0)); assertEquals(13000,s.remaining()); assertEquals(5000,s.duration());
    }
    @Test public void cannotAdvancePendingQuestionOrFinishTwice() {
        QuizSession s=session(); assertFalse(s.next(1)); finishAllCorrect(s);
        assertTrue(s.complete()); assertFalse(s.next(99999)); assertFalse(s.submit());
        assertEquals(500,new QuizScoring(s).score);
    }
    @Test public void mixedOutcomesHaveAccurateScoreAccuracyAndDuration() {
        QuizSession s=session();
        s.elapse(1000); s.select(s.current().correct); s.submit(); s.next(2);
        s.elapse(2000); s.select((s.current().correct+1)%4); s.submit(); s.next(3);
        s.elapse(3000); s.skip(); s.next(4);
        s.elapse(15000); s.next(5);
        s.elapse(1000); s.select(s.current().correct); s.submit(); s.next(6);
        QuizScoring score=new QuizScoring(s);
        assertEquals(2,score.correct); assertEquals(1,score.incorrect); assertEquals(2,score.unanswered);
        assertEquals(200,score.score); assertEquals(40,score.accuracy); assertEquals(22000,s.duration());
    }
    @Test public void draftReopenRestoresQuestionChoiceTimerAndOrder() {
        QuizSession s=session(); s.elapse(3200); s.select(2); db.saveActive(s); db.close(); db=new QuizDatabase(context);
        QuizSession loaded=db.active(); assertNotNull(loaded); assertEquals(s.id,loaded.id);
        assertEquals(11800,loaded.remaining()); assertEquals(2,loaded.selected(0)); assertTrue(loaded.pending());
        for(int i=0;i<5;i++) assertEquals(s.questions.get(i).id,loaded.questions.get(i).id);
    }
    @Test public void completionPersistsSnapshotsOnceAcrossDatabaseReopen() {
        QuizSession s=session(); db.saveActive(s); finishAllCorrect(s); db.finish(s); db.finish(s);
        assertNull(db.active()); db.close(); db=new QuizDatabase(context);
        assertEquals(1,db.history().size()); QuizSession saved=db.result(s.id);
        assertNotNull(saved); assertEquals(500,new QuizScoring(saved).score);
        assertEquals(s.completedAt(),saved.completedAt()); assertEquals(5000,saved.duration());
        for(int i=0;i<5;i++) assertEquals(s.questions.get(i).correct,saved.selected(i));
    }
    @Test public void oldResultDoesNotEraseDifferentActiveQuiz() {
        QuizSession old=session(); finishAllCorrect(old); db.finish(old);
        QuizSession active=session(); db.saveActive(active); db.finish(old);
        assertEquals(active.id,db.active().id);
    }
    @Test public void discardLeavesCompletedHistoryIntact() {
        QuizSession old=session(); finishAllCorrect(old); db.finish(old);
        db.saveActive(session()); db.discardActive(); assertNull(db.active()); assertEquals(1,db.history().size());
    }
    @Test public void versionOneToTwoMigrationPreservesBankAndHistory() {
        QuizSession s=session(); finishAllCorrect(s); db.finish(s);
        SQLiteDatabase raw=db.getWritableDatabase(); raw.execSQL("DROP TABLE active_session"); raw.execSQL("DROP TABLE ai_questions"); raw.execSQL("DROP TABLE ai_collections"); raw.execSQL("DROP TABLE question_reports"); raw.execSQL("ALTER TABLE questions DROP COLUMN origin"); raw.setVersion(1);
        db.close(); db=new QuizDatabase(context);
        assertEquals(240,db.count(null,null)); assertEquals(1,db.history().size()); assertNull(db.active());
        QuizSession active=session(); db.saveActive(active); assertEquals(active.id,db.active().id);
        assertEquals(3,db.getReadableDatabase().getVersion());
    }
    @Test(expected=IllegalArgumentException.class) public void sessionRejectsDuplicateQuestionIds() {
        QuizSession s=session(); List<Question> duplicate=new ArrayList<>(s.questions);
        duplicate.set(1,duplicate.get(0)); new QuizSession(s.config,duplicate,1);
    }
}
