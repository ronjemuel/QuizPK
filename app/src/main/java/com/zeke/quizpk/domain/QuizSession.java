package com.zeke.quizpk.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/** Single idempotent answer gate. Time is foreground elapsed time, never wall clock. */
public final class QuizSession {
    public enum Outcome { PENDING, CORRECT, INCORRECT, SKIPPED, TIMEOUT }
    public final String id;
    public final QuizConfig config;
    public final List<Question> questions;
    public final long startedAt;
    private final int[] selected;
    private final Outcome[] outcomes;
    private final long[] answerMillis;
    private int index;
    private long duration, completedAt;
    public QuizSession(QuizConfig config, List<Question> questions, long startedAt) {
        this(UUID.randomUUID().toString(), config, questions, startedAt);
    }
    private QuizSession(String id, QuizConfig config, List<Question> questions, long startedAt) {
        if (questions.size() != config.count) throw new IllegalArgumentException("Question count");
        HashSet<String> ids = new HashSet<>();
        for (Question q : questions) {
            if (!ids.add(q.id)) throw new IllegalArgumentException("Duplicate question");
        }
        this.id = id; this.config = config; this.startedAt = startedAt;
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
        selected = new int[questions.size()]; outcomes = new Outcome[questions.size()];
        answerMillis = new long[questions.size()];
        java.util.Arrays.fill(selected, -1);
        java.util.Arrays.fill(outcomes, Outcome.PENDING);
    }
    public int index() { return index; }
    public Question current() { return questions.get(index); }
    public int selected(int i) { return selected[i]; }
    public Outcome outcome(int i) { return outcomes[i]; }
    public long answerMillis(int i) { return answerMillis[i]; }
    public boolean complete() { return completedAt != 0; }
    public long completedAt() { return completedAt; }
    public long duration() { return duration; }
    public boolean pending() { return !complete() && outcomes[index] == Outcome.PENDING; }
    public long remaining() { return Math.max(0, config.seconds * 1000L - answerMillis[index]); }
    public boolean select(int choice) {
        if (!pending() || choice < 0 || choice > 3) return false;
        selected[index] = choice; return true;
    }
    /** Called only while visible. Feedback reading counts in total duration, not answer time. */
    public void elapse(long millis) {
        if (complete() || millis < 0) return;
        duration += millis;
        if (pending()) {
            answerMillis[index] = Math.min(config.seconds * 1000L, answerMillis[index] + millis);
            if (remaining() == 0) outcomes[index] = Outcome.TIMEOUT;
        }
    }
    public boolean submit() {
        if (!pending() || selected[index] < 0) return false;
        outcomes[index] = selected[index] == current().correct ? Outcome.CORRECT : Outcome.INCORRECT;
        return true;
    }
    public boolean skip() {
        if (!pending()) return false;
        outcomes[index] = Outcome.SKIPPED; selected[index] = -1; return true;
    }
    public boolean next(long now) {
        if (complete() || pending()) return false;
        if (index + 1 == questions.size()) completedAt = Math.max(1, now);
        else index++;
        return true;
    }
    public JSONObject toJson() throws JSONException {
        JSONArray qs = new JSONArray(), answers = new JSONArray();
        for (int i = 0; i < questions.size(); i++) {
            qs.put(questions.get(i).toJson());
            answers.put(new JSONObject().put("selected", selected[i])
                    .put("outcome", outcomes[i].name()).put("millis", answerMillis[i]));
        }
        return new JSONObject().put("format", 1).put("id", id).put("config", config.toJson())
                .put("questions", qs).put("answers", answers).put("index", index)
                .put("startedAt", startedAt).put("completedAt", completedAt).put("duration", duration);
    }
    public static QuizSession fromJson(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        if (o.getInt("format") != 1) throw new JSONException("Unsupported session format");
        List<Question> qs = new ArrayList<>();
        JSONArray q = o.getJSONArray("questions"), a = o.getJSONArray("answers");
        for (int i = 0; i < q.length(); i++) qs.add(Question.fromJson(q.getJSONObject(i)));
        QuizSession s = new QuizSession(o.getString("id"), QuizConfig.fromJson(o.getJSONObject("config")),
                qs, o.getLong("startedAt"));
        s.index = o.getInt("index"); s.duration = o.getLong("duration");
        s.completedAt = o.getLong("completedAt");
        if (a.length() != qs.size() || s.index < 0 || s.index >= qs.size())
            throw new JSONException("Invalid session state");
        for (int i = 0; i < a.length(); i++) {
            JSONObject ans = a.getJSONObject(i);
            s.selected[i] = ans.getInt("selected");
            s.outcomes[i] = Outcome.valueOf(ans.getString("outcome"));
            s.answerMillis[i] = ans.getLong("millis");
        }
        return s;
    }
}
