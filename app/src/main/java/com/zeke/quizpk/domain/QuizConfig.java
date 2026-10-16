package com.zeke.quizpk.domain;

import java.util.Arrays;
import org.json.JSONObject;
import org.json.JSONException;

public final class QuizConfig {
    public static final String ALL = "All Categories";
    public static final String[] CATEGORIES = {"General", "Science", "History", "Geography",
            "Technology", "Sports", "Movies", "Music", "Literature", "Animals", "Food", "Space"};
    public static final String[] DIFFICULTIES = {"Easy", "Normal", "Hard", "Expert"};
    public final String category, difficulty, collectionId;
    public final int count, seconds;
    public final boolean explanations, randomize;
    public QuizConfig(String category, String difficulty, int count, int seconds,
                      boolean explanations, boolean randomize) {
        this(category,difficulty,count,seconds,explanations,randomize,null);
    }
    public QuizConfig(String category, String difficulty, int count, int seconds,
                      boolean explanations, boolean randomize, String collectionId) {
        if ((collectionId == null && (!ALL.equals(category) && !Arrays.asList(CATEGORIES).contains(category)))
                || !Arrays.asList(DIFFICULTIES).contains(difficulty)
                || !Arrays.asList(5, 10, 15, 20).contains(count)
                || !Arrays.asList(10, 15, 20, 30).contains(seconds)) {
            throw new IllegalArgumentException("Unsupported quiz configuration");
        }
        this.category = category; this.difficulty = difficulty; this.count = count;
        this.collectionId = collectionId; this.seconds = seconds; this.explanations = explanations; this.randomize = randomize;
    }
    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("category", category).put("difficulty", difficulty)
                .put("count", count).put("seconds", seconds).put("explanations", explanations)
                .put("randomize", randomize).put("collectionId", collectionId);
    }
    public static QuizConfig fromJson(JSONObject o) throws JSONException {
        return new QuizConfig(o.getString("category"), o.getString("difficulty"),
                o.getInt("count"), o.getInt("seconds"), o.getBoolean("explanations"),
                o.getBoolean("randomize"), o.isNull("collectionId") ? null : o.optString("collectionId", null));
    }
}
