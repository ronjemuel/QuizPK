package com.zeke.quizpk;

import static org.junit.Assert.*;
import android.content.*;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.data.remote.AiBackendClient;
import com.zeke.quizpk.ui.screens.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.LEGACY) @ConscryptMode(ConscryptMode.Mode.OFF) @SQLiteMode(SQLiteMode.Mode.LEGACY)
public class AiFlowTest {
    private Context context;
    @Before public void setup(){HostSqliteSupport.install();context=RuntimeEnvironment.getApplication();context.deleteDatabase(QuizDatabase.NAME);
        context.getSharedPreferences("quizpk_ai_pending",0).edit().clear().commit();}
    private String save()throws Exception{try(QuizDatabase db=new QuizDatabase(context)){return db.importAi(AiQuiz.parse(AiFixtures.response("job",AiFixtures.request()),"job",AiFixtures.request()),"https://backend.test");}}
    private ActivityController<OfflineQuizActivity> play(String id){return Robolectric.buildActivity(OfflineQuizActivity.class,new Intent(context,OfflineQuizActivity.class).putExtra("ai_collection",id)).setup();}
    @Test public void generatedQuizUsesM2ButtonsReviewReportAndOfflineReplay()throws Exception{
        String id=save();
        try(ActivityController<OfflineQuizActivity> c=play(id)){
            OfflineQuizActivity a=c.get();assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.offline_container) instanceof GameplayFragment);
            assertEquals(20,a.session.config.seconds);
            for(int i=0;i<5;i++){a.findViewById(R.id.answer_options).findViewWithTag("choice_0").performClick();a.findViewById(R.id.submit_answer).performClick();a.findViewById(R.id.next_question).performClick();}
            assertEquals(500,new QuizScoring(a.session).score);assertEquals(1,a.database.history().size());
            a.findViewById(R.id.review_answers).performClick();assertEquals(5,((LinearLayout)a.findViewById(R.id.review_list)).getChildCount());
            LinearLayout first=(LinearLayout)((com.google.android.material.card.MaterialCardView)((LinearLayout)a.findViewById(R.id.review_list)).getChildAt(0)).getChildAt(0);
            assertEquals(2,first.getChildCount());assertEquals(context.getString(R.string.ai_report),((TextView)first.getChildAt(1)).getText().toString());
            first.getChildAt(1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();androidx.appcompat.app.AlertDialog report=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            findEdit(report.getWindow().getDecorView()).setText("This answer needs a factual check");report.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(a.database.reported(a.session.questions.get(0).id));
            a.getOnBackPressedDispatcher().onBackPressed();a.database.deleteAi(id);a.findViewById(R.id.play_again).performClick();
            assertNotNull(a.findViewById(R.id.question_text));assertFalse(a.session.complete());assertEquals(0,new QuizScoring(a.session).score);
        }
    }
    @Test public void aiDraftRecreationRetainsCollectionTimerAndSelections()throws Exception{
        String id=save();try(ActivityController<OfflineQuizActivity> c=play(id)){
            OfflineQuizActivity a=c.get();a.findViewById(R.id.answer_options).findViewWithTag("choice_2").performClick();String session=a.session.id;
            c.recreate();a=c.get();assertEquals(session,a.session.id);assertEquals(id,a.session.config.collectionId);assertEquals(2,a.session.selected(0));
            assertEquals(View.VISIBLE,a.findViewById(R.id.resume_question).getVisibility());
        }
    }
    @Test public void invalidTopicAndSavedListNavigationWorkInBothThemes()throws Exception{
        save();for(boolean light:new boolean[]{false,true}){
            context.getSharedPreferences("quizpk_appearance",0).edit().putBoolean("light_theme",light).commit();
            try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
                MainActivity a=c.get();a.navigateTo(R.id.nav_ai);Shadows.shadowOf(Looper.getMainLooper()).idle();
                ((EditText)a.findViewById(R.id.ai_topic)).setText("");
                assertTrue(a.findViewById(R.id.generate_quiz).isEnabled());
                a.findViewById(R.id.generate_quiz).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertNotNull(((com.google.android.material.textfield.TextInputLayout)a.findViewById(R.id.ai_topic_layout)).getError());
                a.findViewById(R.id.ai_saved_tab).performClick();assertEquals(View.VISIBLE,a.findViewById(R.id.ai_saved_panel).getVisibility());
                assertEquals(1,((androidx.recyclerview.widget.RecyclerView)a.findViewById(R.id.ai_saved_list)).getAdapter().getItemCount());
                c.recreate();assertEquals(View.VISIBLE,c.get().findViewById(R.id.ai_saved_panel).getVisibility());
            }
        }
    }
    private EditText findEdit(View root){
        if(root instanceof EditText)return (EditText)root;
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++){EditText found=findEdit(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    private View button(View root,String label){
        if(root instanceof android.widget.Button && label.contentEquals(((TextView)root).getText()))return root;
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++){View found=button(group.getChildAt(i),label);if(found!=null)return found;}}
        return null;
    }
    private <H extends androidx.recyclerview.widget.RecyclerView.ViewHolder> View bindFirst(
            androidx.recyclerview.widget.RecyclerView list, androidx.recyclerview.widget.RecyclerView.Adapter<H> adapter){
        H holder=adapter.createViewHolder(list,0);adapter.bindViewHolder(holder,0);return holder.itemView;
    }
    @Test public void savedRenameAndDeleteRequireDialogConfirmation()throws Exception{
        save();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();a.navigateTo(R.id.nav_ai);a.findViewById(R.id.ai_saved_tab).performClick();
            androidx.recyclerview.widget.RecyclerView list=a.findViewById(R.id.ai_saved_list);
            View item=bindFirst(list,list.getAdapter());
            button(item,context.getString(R.string.ai_rename)).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            androidx.appcompat.app.AlertDialog rename=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            findEdit(rename.getWindow().getDecorView()).setText("Java Collections");rename.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            try(QuizDatabase db=new QuizDatabase(context)){assertEquals("Java Collections",db.savedAi().get(0).title);}
            button(item,context.getString(R.string.ai_delete)).performClick();
            androidx.appcompat.app.AlertDialog delete=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            delete.getButton(-2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            try(QuizDatabase db=new QuizDatabase(context)){assertEquals(1,db.savedAi().size());}
            button(item,context.getString(R.string.ai_delete)).performClick();
            delete=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            delete.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(0,((androidx.recyclerview.widget.RecyclerView)a.findViewById(R.id.ai_saved_list)).getAdapter().getItemCount());
        }
    }
    @Test public void newActivityRecoversPendingRequestWithoutInventingAnotherId()throws Exception{
        AtomicReference<String> job=new AtomicReference<>();
        AiBackendClient.Transport fail=new AiBackendClient.Transport(){public String post(String a,String b,String id,String body)throws AiBackendClient.Failure{
            job.set(id);throw new AiBackendClient.Failure("NO_CONNECTION",true);}public void cancel(){}};
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            AiQuizViewModel model=new ViewModelProvider(c.get()).get(AiQuizViewModel.class);model.generate("https://backend.test","mock-token",AiFixtures.request(),fail);settle(model);
        }
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();a.navigateTo(R.id.nav_ai);AiQuizViewModel model=new ViewModelProvider(a).get(AiQuizViewModel.class);
            assertEquals("INTERRUPTED",model.state.getValue().code);assertEquals("Java Programming",((EditText)a.findViewById(R.id.ai_topic)).getText().toString());
            AiBackendClient.Transport recover=new AiBackendClient.Transport(){public String post(String x,String y,String id,String body)throws AiBackendClient.Failure{
                assertEquals(job.get(),id);try{return AiFixtures.response(id,AiFixtures.request());}catch(Exception e){throw new AiBackendClient.Failure("MALFORMED_QUESTIONS",false);}}public void cancel(){}};
            model.generate("https://backend.test","mock-token",AiFixtures.request(),recover);settle(model);assertEquals("SUCCESS",model.state.getValue().code);
        }
    }
    @Test @Config(qualifiers="w320dp-h640dp") public void compactAiFormKeepsControlsInScrollableWidth(){
        RuntimeEnvironment.setFontScale(1.6f);
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();a.navigateTo(R.id.nav_ai);View root=a.getWindow().getDecorView();float d=a.getResources().getDisplayMetrics().density;
            root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(320*d),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(640*d),View.MeasureSpec.EXACTLY));
            root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
            for(int id:new int[]{R.id.ai_topic,R.id.ai_difficulty,R.id.ai_count,R.id.ai_timer,R.id.generate_quiz}){View v=a.findViewById(id);assertTrue(v.getWidth()>0);assertTrue(v.getWidth()<=320*d);assertTrue(v.getHeight()>=48*d);}
        }
    }
    private void settle(AiQuizViewModel model)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        do{Shadows.shadowOf(Looper.getMainLooper()).idle();if(!model.state.getValue().busy)return;Thread.sleep(20);}while(System.nanoTime()<deadline);fail("Job did not settle");
    }
    @Test public void pendingJobSurvivesRecreationAndDuplicateTapsCallTransportOnce()throws Exception{
        AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AiBackendClient.Transport transport=new AiBackendClient.Transport(){public String post(String endpoint,String token,String id,String body)throws AiBackendClient.Failure{
            calls.incrementAndGet();entered.countDown();try{release.await(10,TimeUnit.SECONDS);return AiFixtures.response(id,AiFixtures.request());}catch(Exception e){throw new AiBackendClient.Failure("TIMEOUT",true);}}
            public void cancel(){release.countDown();}};
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();a.navigateTo(R.id.nav_ai);AiQuizViewModel model=new ViewModelProvider(a).get(AiQuizViewModel.class);
            assertTrue(model.generate("https://backend.test","mock-token",AiFixtures.request(),transport));assertTrue(entered.await(10,TimeUnit.SECONDS));
            assertFalse(model.generate("https://backend.test","mock-token",AiFixtures.request(),transport));
            assertFalse(a.findViewById(R.id.generate_quiz).isEnabled());c.recreate();a=c.get();
            assertSame(model,new ViewModelProvider(a).get(AiQuizViewModel.class));assertFalse(a.findViewById(R.id.generate_quiz).isEnabled());
            release.countDown();settle(model);assertEquals("SUCCESS",model.state.getValue().code);assertEquals(1,calls.get());
            assertEquals(View.VISIBLE,a.findViewById(R.id.ai_play_generated).getVisibility());
        }finally{release.countDown();}
    }
    @Test public void explicitRetryAfterNetworkFailureKeepsRequestIdAndImportsOnce()throws Exception{
        AtomicReference<String> first=new AtomicReference<>();AtomicInteger calls=new AtomicInteger();
        AiBackendClient.Transport transport=new AiBackendClient.Transport(){public String post(String a,String b,String id,String body)throws AiBackendClient.Failure{
            if(calls.getAndIncrement()==0){first.set(id);throw new AiBackendClient.Failure("NO_CONNECTION",true);}assertEquals(first.get(),id);
            try{return AiFixtures.response(id,AiFixtures.request());}catch(Exception e){throw new AiBackendClient.Failure("MALFORMED_QUESTIONS",false);}}
            public void cancel(){}};
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            AiQuizViewModel model=new ViewModelProvider(c.get()).get(AiQuizViewModel.class);
            model.generate("https://backend.test","mock-token",AiFixtures.request(),transport);settle(model);assertEquals("NO_CONNECTION",model.state.getValue().code);
            model.generate("https://backend.test","mock-token",AiFixtures.request(),transport);settle(model);assertEquals("SUCCESS",model.state.getValue().code);
        }
        try(QuizDatabase db=new QuizDatabase(context)){assertEquals(1,db.savedAi().size());}
    }
    @Test public void canceledJobDoesNotDeliverStaleSuccessAndRecoveryRetainsIdentity()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<String> job=new AtomicReference<>();
        AiBackendClient.Transport blocking=new AiBackendClient.Transport(){public String post(String a,String b,String id,String body)throws AiBackendClient.Failure{
            job.set(id);entered.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}throw new AiBackendClient.Failure("CANCELED",true);}
            public void cancel(){release.countDown();}};
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            AiQuizViewModel model=new ViewModelProvider(c.get()).get(AiQuizViewModel.class);model.generate("https://backend.test","mock-token",AiFixtures.request(),blocking);
            assertTrue(entered.await(10,TimeUnit.SECONDS));model.cancel();assertEquals("CANCELED",model.state.getValue().code);
            AiBackendClient.Transport recovery=new AiBackendClient.Transport(){public String post(String a,String b,String id,String body)throws AiBackendClient.Failure{
                assertEquals(job.get(),id);try{return AiFixtures.response(id,AiFixtures.request());}catch(Exception e){throw new AiBackendClient.Failure("MALFORMED_QUESTIONS",false);}}
                public void cancel(){}};
            model.generate("https://backend.test","mock-token",AiFixtures.request(),recovery);settle(model);assertEquals("SUCCESS",model.state.getValue().code);
        }finally{release.countDown();}
    }
}
