package com.zeke.quizpk.domain;

import org.json.*;
import java.util.*;

/** Validated import boundary. IDs are generated locally; correctIndex is zero-based. */
public final class AiQuiz {
    public final String id, generationId, topic, difficulty, model;
    public final int seconds;
    public final long createdAt;
    public final List<Question> questions;
    private AiQuiz(String generationId,AiQuizRequest request,String model,long createdAt,List<Question> questions,String id) {
        this.generationId=generationId;this.id=id;topic=request.topic;difficulty=request.difficulty;
        seconds=request.seconds;this.model=model;this.createdAt=createdAt;this.questions=Collections.unmodifiableList(questions);
    }
    private static int integer(JSONObject o,String name) throws JSONException {
        Object value=o.get(name); if (!(value instanceof Integer) && !(value instanceof Long)) throw new JSONException("Integer required");
        long n=((Number)value).longValue();if(n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new JSONException("Integer range");return (int)n;
    }
    private static String string(JSONObject o,String key,int min,int max) throws JSONException {
        Object value=o.get(key);if(!(value instanceof String))throw new JSONException("Text required");
        return AiQuizRequest.clean((String)value,min,max);
    }
    public static AiQuiz parse(String json,String expectedId,AiQuizRequest request) throws JSONException {
        if(json.length()>180000)throw new JSONException("Response too large");
        JSONObject root=new JSONObject(json), quiz=root.getJSONObject("quiz");
        if(integer(root,"schemaVersion")!=1 || !expectedId.equals(root.getString("generationId"))
                || !"AI_GENERATED".equals(quiz.getString("origin")) || !request.topic.equals(quiz.getString("topic"))
                || !request.difficulty.equals(quiz.getString("difficulty")) || integer(quiz,"questionCount")!=request.count
                || integer(quiz,"timerSeconds")!=request.seconds) throw new JSONException("Metadata mismatch");
        String model=string(quiz,"model",1,120);Object date=quiz.get("createdAt");
        if(!(date instanceof Long)&&!(date instanceof Integer))throw new JSONException("Date required");
        long created=((Number)date).longValue(); if(created<=0)throw new JSONException("Date range");
        JSONArray items=quiz.getJSONArray("questions"); if(items.length()!=request.count)throw new JSONException("Question count");
        String id=UUID.randomUUID().toString();List<Question> questions=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<items.length();i++) {
            JSONObject q=items.getJSONObject(i);String text=string(q,"text",5,600),explanation=string(q,"explanation",10,1000);
            if(!request.topic.equals(q.getString("topic"))||!request.difficulty.equals(q.getString("difficulty"))
                    || !seen.add(AiQuizRequest.normalized(text)) || AiQuizRequest.normalized(text).equals(AiQuizRequest.normalized(explanation))) throw new JSONException("Invalid question metadata or repetition");
            JSONArray choices=q.getJSONArray("options");if(choices.length()!=4)throw new JSONException("Four choices required");
            List<String> options=new ArrayList<>();Set<String> distinct=new HashSet<>();
            for(int j=0;j<4;j++) {Object value=choices.get(j);if(!(value instanceof String))throw new JSONException("Choice type");
                String option=AiQuizRequest.clean((String)value,1,240);if(!distinct.add(AiQuizRequest.normalized(option)))throw new JSONException("Duplicate choices");options.add(option);}
            questions.add(new Question(UUID.randomUUID().toString(),request.topic,request.difficulty,text,options,integer(q,"correctIndex"),explanation,"AI_GENERATED"));
        }
        return new AiQuiz(expectedId,request,model,created,questions,id);
    }
}
