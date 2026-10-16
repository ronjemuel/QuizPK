package com.zeke.quizpk.domain;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public final class AiQuizRequest {
    public final String topic, difficulty, instructions;
    public final int count, seconds;
    public AiQuizRequest(String topic, String difficulty, int count, int seconds, String instructions) {
        this.topic = clean(topic,3,120); this.instructions = clean(instructions,0,500);
        if (this.topic.codePoints().noneMatch(Character::isLetterOrDigit)) throw new IllegalArgumentException("INVALID_TOPIC");
        if (!Arrays.asList(QuizConfig.DIFFICULTIES).contains(difficulty)
                || !Arrays.asList(5,10,15,20).contains(count) || !Arrays.asList(10,15,20,30).contains(seconds))
            throw new IllegalArgumentException("INVALID_SETTINGS");
        this.difficulty=difficulty; this.count=count; this.seconds=seconds;
    }
    public static String clean(String text,int min,int max) {
        if (text==null || text.trim().length()<min || text.trim().length()>max
                || text.matches("(?s).*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*")) throw new IllegalArgumentException("INVALID_TEXT");
        return text.trim();
    }
    public static String normalized(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
    }
    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("topic",topic).put("difficulty",difficulty).put("questionCount",count)
                .put("timerSeconds",seconds).put("additionalInstructions",instructions);
    }
    public static AiQuizRequest fromJson(JSONObject o) throws JSONException {
        return new AiQuizRequest(o.getString("topic"),o.getString("difficulty"),o.getInt("questionCount"),o.getInt("timerSeconds"),o.optString("additionalInstructions"));
    }
}
