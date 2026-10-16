package com.zeke.quizpk.domain;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/** Immutable local bank item. Future remote player payloads must omit the answer key. */
public final class Question {
    public final String id, category, difficulty, text, explanation, source;
    public final List<String> options;
    public final int correct;
    public Question(String id, String category, String difficulty, String text,
                    List<String> options, int correct, String explanation, String source) {
        if (id.isEmpty() || text.trim().isEmpty() || options.size() != 4
                || correct < 0 || correct > 3 || new HashSet<>(options).size() != 4
                || options.stream().anyMatch(s -> s.trim().isEmpty()) || source.isEmpty()) {
            throw new IllegalArgumentException("Invalid question " + id);
        }
        this.id = id; this.category = category; this.difficulty = difficulty;
        this.text = text; this.options = Collections.unmodifiableList(new ArrayList<>(options));
        this.correct = correct; this.explanation = explanation; this.source = source;
    }
    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("category", category)
                .put("difficulty", difficulty).put("text", text)
                .put("options", new JSONArray(options)).put("correct", correct)
                .put("explanation", explanation).put("source", source);
    }
    public static Question fromJson(JSONObject o) throws JSONException {
        List<String> choices = new ArrayList<>();
        JSONArray a = o.getJSONArray("options");
        for (int i = 0; i < a.length(); i++) choices.add(a.getString(i));
        return new Question(o.getString("id"), o.getString("category"),
                o.getString("difficulty"), o.getString("text"), choices,
                o.getInt("correct"), o.optString("explanation"), o.getString("source"));
    }
}
