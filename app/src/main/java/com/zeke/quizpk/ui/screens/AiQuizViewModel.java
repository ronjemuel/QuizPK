package com.zeke.quizpk.ui.screens;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.data.remote.AiBackendClient;
import java.util.UUID;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Activity-scoped job survives recreation; disk checkpoint makes explicit retries idempotent. */
public final class AiQuizViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean busy;public final String code,collectionId;
        State(boolean busy,String code,String collectionId){this.busy=busy;this.code=code;this.collectionId=collectionId;}
    }
    public final MutableLiveData<State> state=new MutableLiveData<>(new State(false,"READY",null));
    private final SharedPreferences prefs;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private AiBackendClient client;
    private int epoch;
    public AiQuizViewModel(Application application){super(application);prefs=application.getSharedPreferences("quizpk_ai_pending",Context.MODE_PRIVATE);
        if(prefs.contains("request_id"))state.setValue(new State(false,"INTERRUPTED",null));
    }
    public AiQuizRequest pending() {
        try{return AiQuizRequest.fromJson(new org.json.JSONObject(prefs.getString("request","")));}catch(Exception e){return null;}
    }
    public boolean generate(String endpoint,String token,AiQuizRequest request) {
        return generate(endpoint,token,request,new AiBackendClient.HttpsTransport());
    }
    public boolean generate(String endpoint,String token,AiQuizRequest request,AiBackendClient.Transport transport) {
        if(state.getValue().busy)return false;
        final String body,scope,id;
        try {
            body=request.toJson().toString();
            scope=endpoint+"|"+android.util.Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)),android.util.Base64.NO_WRAP);
            id=body.equals(prefs.getString("request",null))&&scope.equals(prefs.getString("scope",null))
                    ?prefs.getString("request_id",UUID.randomUUID().toString()):UUID.randomUUID().toString();
            if(!prefs.edit().putString("request",body).putString("scope",scope).putString("request_id",id).commit())throw new Exception();
        } catch(Exception e){state.setValue(new State(false,"STORAGE_ERROR",null));return false;}
        final int run=++epoch;client=new AiBackendClient(transport);final AiBackendClient active=client;
        state.setValue(new State(true,"GENERATING",null));
        worker.execute(()->{
            String collection=null,code="SUCCESS";boolean clear=false;
            try {
                AiQuiz quiz=active.generate(endpoint,token,id,request);
                try(QuizDatabase db=new QuizDatabase(getApplication())){collection=db.importAi(quiz,endpoint);}
                clear=true;
            }catch(AiBackendClient.Failure e){code=e.code;clear=!e.retrySame;}
            catch(RuntimeException e){code="STORAGE_ERROR";}
            final String result=collection,error=code;final boolean forget=clear;
            main.post(()->{if(run!=epoch)return;if(forget)prefs.edit().remove("request_id").apply();
                state.setValue(new State(false,error,result));});
        });
        return true;
    }
    public void cancel(){epoch++;if(client!=null)client.cancel();state.setValue(new State(false,"CANCELED",null));}
    @Override protected void onCleared(){cancel();worker.shutdownNow();}
}
