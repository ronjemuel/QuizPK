package com.zeke.quizpk;

import com.zeke.quizpk.domain.AiQuizRequest;
import org.json.*;

final class AiFixtures {
    static AiQuizRequest request(){return new AiQuizRequest("Java Programming","Hard",5,20,"");}
    static String response(String id,AiQuizRequest request) throws JSONException {
        JSONArray questions=new JSONArray();
        for(int i=0;i<request.count;i++)questions.put(new JSONObject().put("text","Which Java fact is number "+i+"?")
                .put("options",new JSONArray(new String[]{"One","Two","Three","Four"})).put("correctIndex",0)
                .put("explanation","One is the correct choice in this deterministic fixture.").put("topic",request.topic).put("difficulty",request.difficulty));
        return new JSONObject().put("schemaVersion",1).put("generationId",id).put("quiz",new JSONObject()
                .put("topic",request.topic).put("difficulty",request.difficulty).put("questionCount",request.count).put("timerSeconds",request.seconds)
                .put("createdAt",1234567890000L).put("origin","AI_GENERATED").put("model","mock-model").put("questions",questions)).toString();
    }
}
