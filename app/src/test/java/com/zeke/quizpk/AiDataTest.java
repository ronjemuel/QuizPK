package com.zeke.quizpk;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.data.remote.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.LEGACY) @ConscryptMode(ConscryptMode.Mode.OFF) @SQLiteMode(SQLiteMode.Mode.LEGACY)
public class AiDataTest {
    private Context context;private QuizDatabase db;
    @Before public void setup(){HostSqliteSupport.install();context=RuntimeEnvironment.getApplication();context.deleteDatabase(QuizDatabase.NAME);db=new QuizDatabase(context);}
    @After public void close(){db.close();}
    private AiQuiz fixture() throws Exception {return AiQuiz.parse(AiFixtures.response("job",AiFixtures.request()),"job",AiFixtures.request());}
    private QuizSession completedBank(){QuizConfig config=new QuizConfig("Science","Easy",5,15,true,false);
        QuizSession s=new QuizSession(config,db.select(config,new Random(1)),1);complete(s);return s;}
    private void complete(QuizSession s){for(int i=0;i<s.questions.size();i++){s.select(s.current().correct);s.submit();s.next(100+i);}}
    @Test public void validatesTopicsAndEverySettingBoundary(){
        assertEquals("Java",new AiQuizRequest("  Java  ","Normal",10,15,"").topic);
        for(String topic:new String[]{"","  ","ab","...","x".repeat(121)}){try{new AiQuizRequest(topic,"Normal",5,15,"");fail(topic);}catch(IllegalArgumentException expected){}}
        try{new AiQuizRequest("Java","Extreme",5,15,"");fail();}catch(IllegalArgumentException expected){}
        try{new AiQuizRequest("Java","Hard",6,15,"");fail();}catch(IllegalArgumentException expected){}
        try{new AiQuizRequest("Java","Hard",5,99,"");fail();}catch(IllegalArgumentException expected){}
        try{new AiQuizRequest("Java","Hard",5,15,"x".repeat(501));fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void importerRejectsMalformedPartialDuplicateAndCoercedFields() throws Exception {
        for(int patch=0;patch<12;patch++){
            JSONObject root=new JSONObject(AiFixtures.response("job",AiFixtures.request()));JSONArray qs=root.getJSONObject("quiz").getJSONArray("questions");JSONObject q=qs.getJSONObject(0);
            switch(patch){case 0:qs.remove(0);break;case 1:q.put("text","");break;case 2:qs.getJSONObject(1).put("text",q.getString("text").toUpperCase());break;
                case 3:q.getJSONArray("options").put(1," one ");break;case 4:q.put("correctIndex",4);break;case 5:q.put("correctIndex","0");break;
                case 6:q.put("explanation","");break;case 7:q.put("topic","unrelated");break;case 8:root.put("generationId","other");break;
                case 9:q.getJSONArray("options").put(4,"Five");break;case 10:q.getJSONArray("options").put(0,42);break;case 11:root.put("schemaVersion",2);break;}
            try{AiQuiz.parse(root.toString(),"job",AiFixtures.request());fail("Patch "+patch);}catch(JSONException|IllegalArgumentException expected){}
        }
        try{AiQuiz.parse("not-json","job",AiFixtures.request());fail();}catch(JSONException expected){}
    }
    @Test public void importsAtomicallyOnceWithLocalIdsAndNoBankContamination() throws Exception {
        AiQuiz a=fixture(),b=fixture();assertNotEquals(a.id,b.id);String id=db.importAi(a,"https://backend.test");
        assertEquals(id,db.importAi(b,"https://backend.test"));assertEquals(1,db.savedAi().size());assertEquals(240,db.count(null,null));
        assertEquals(5,db.aiSession(id).questions.size());
        assertEquals("AI_GENERATED",db.aiSession(id).current().source);
        assertEquals("OFFLINE_BANK",getOrigin());
    }
    private String getOrigin(){try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT origin FROM questions LIMIT 1",null)){c.moveToFirst();return c.getString(0);}}
    @Test public void renameDeletePreserveActiveAndHistoricalSnapshotsOffline() throws Exception {
        String id=db.importAi(fixture(),"https://backend.test");db.renameAi(id,"Java Collections");
        QuizSession s=db.aiSession(id);assertEquals("Java Collections",s.config.category);complete(s);db.finish(s);
        QuizSession active=db.aiSession(id);active.select(2);active.elapse(3000);db.saveActive(active);
        db.deleteAi(id);assertEquals(0,db.savedAi().size());assertEquals(500,new QuizScoring(db.result(s.id)).score);
        db.close();db=new QuizDatabase(context);assertEquals(2,db.active().selected(0));assertEquals(17000,db.active().remaining());
        assertEquals(id,db.result(s.id).config.collectionId);assertEquals(240,db.count(null,null));
    }
    @Test public void v2UpgradePreservesBankDraftAndCompletedHistory() throws Exception {
        QuizSession old=completedBank();db.finish(old);QuizSession draft=new QuizSession(old.config,old.questions,2);db.saveActive(draft);
        SQLiteDatabase raw=db.getWritableDatabase();raw.execSQL("DROP TABLE ai_questions");raw.execSQL("DROP TABLE ai_collections");raw.execSQL("DROP TABLE question_reports");raw.execSQL("ALTER TABLE questions DROP COLUMN origin");raw.setVersion(2);
        db.close();db=new QuizDatabase(context);assertEquals(3,db.getReadableDatabase().getVersion());
        assertEquals(240,db.count(null,null));assertEquals(old.id,db.history().get(0).id);assertEquals(draft.id,db.active().id);
        assertEquals(0,db.savedAi().size());db.importAi(fixture(),"https://backend.test");assertEquals(1,db.savedAi().size());
    }
    @Test public void aiScoringReviewAndSnapshotRoundTripUseM2Rules() throws Exception {
        String id=db.importAi(fixture(),"https://backend.test");QuizSession s=db.aiSession(id);
        s.select(0);s.submit();s.next(1);s.select(1);s.submit();s.next(2);s.skip();s.next(3);s.elapse(20000);s.next(4);s.select(0);s.submit();s.next(5);
        db.finish(s);QuizSession restored=db.result(s.id);QuizScoring score=new QuizScoring(restored);
        assertEquals(200,score.score);assertEquals(40,score.accuracy);assertEquals(2,score.unanswered);assertEquals(1,score.incorrect);
        assertEquals(s.questions.get(0).explanation,restored.questions.get(0).explanation);
    }
    @Test public void reportsPersistWithoutNetworkEvenAfterCollectionDeleted() throws Exception {
        String id=db.importAi(fixture(),"https://backend.test");Question q=db.aiSession(id).current();
        db.report(q,"The answer needs checking");db.report(q,"Please check this fact");db.deleteAi(id);
        db.close();db=new QuizDatabase(context);assertTrue(db.reported(q.id));
    }
    @Test public void httpFailureMappingNeverRendersProviderOrProxyBodies() throws Exception {
        for(int status:new int[]{401,403,429,425,500,502,503,504}){
            try{AiBackendClient.decodeHttp(status,"secret upstream stack trace");fail();}catch(AiBackendClient.Failure e){assertFalse(e.getMessage().contains("secret"));assertEquals(status==425,e.retrySame);}
        }
        assertEquals("{}",AiBackendClient.decodeHttp(200,"{}"));
    }
    @Test public void mockTransportCoversAirplaneTimeoutAndBadResponse() throws Exception {
        for(String code:new String[]{"NO_CONNECTION","TIMEOUT","CANCELED","AUTH_REQUIRED","RATE_LIMITED","PROVIDER_UNAVAILABLE"}){
            AiBackendClient c=new AiBackendClient(new AiBackendClient.Transport(){public String post(String a,String b,String id,String body)throws AiBackendClient.Failure{throw new AiBackendClient.Failure(code,true);}public void cancel(){}});
            try{c.generate("https://backend.test","test","job",AiFixtures.request());fail();}catch(AiBackendClient.Failure e){assertEquals(code,e.code);}
        }
        AiBackendClient malformed=new AiBackendClient(new AiBackendClient.Transport(){
            public String post(String a,String b,String id,String body){return "not-json";}
            public void cancel(){}
        });
        try{malformed.generate("https://backend.test","test","job",AiFixtures.request());fail();}
        catch(AiBackendClient.Failure e){assertEquals("MALFORMED_QUESTIONS",e.code);assertFalse(e.retrySame);}
    }
    @Test public void rejectsCleartextAndCredentialBearingOrAmbiguousEndpoints(){
        for(String url:new String[]{"http://host.test","https://user:pass@host.test","https://host.test/path","https://host.test?token=abc","https://host.test#abc","garbage"}){
            try{BackendCredentials.validateEndpoint(url);fail(url);}catch(IllegalArgumentException expected){}
        }
        assertEquals("https://backend.test",BackendCredentials.validateEndpoint("https://backend.test/"));
    }
}
