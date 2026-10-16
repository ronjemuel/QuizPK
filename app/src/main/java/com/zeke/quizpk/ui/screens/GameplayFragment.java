package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.domain.*;

public final class GameplayFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastTick = -1;
    private boolean paused;
    private View root;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (root == null || paused) return;
            boolean wasPending = quiz().pending(); syncClock(); updateTimer();
            if (wasPending && !quiz().pending()) { host().persist(); render(); }
            if (quiz().pending()) handler.postDelayed(this, 100);
        }
    };
    public GameplayFragment() { super(R.layout.fragment_gameplay); }
    private OfflineQuizActivity host() { return (OfflineQuizActivity) requireActivity(); }
    private QuizSession quiz() { return host().session; }
    @Override public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved); paused = saved != null;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        root = view;
        view.findViewById(R.id.submit_answer).setOnClickListener(v -> {
            if (paused) return; syncClock();
            if (quiz().submit() || !quiz().pending()) { handler.removeCallbacks(tick); host().persist(); render(); }
        });
        view.findViewById(R.id.skip_question).setOnClickListener(v -> {
            if (paused) return; syncClock(); quiz().skip(); handler.removeCallbacks(tick); host().persist(); render();
        });
        view.findViewById(R.id.next_question).setOnClickListener(v -> {
            if (paused) return; syncClock();
            if (!quiz().next(System.currentTimeMillis())) return;
            if (!host().persist()) return;
            if (quiz().complete()) host().show("results");
            else { render(); handler.removeCallbacks(tick); handler.post(tick); }
        });
        view.findViewById(R.id.resume_question).setOnClickListener(v -> resumeQuiz());
        render();
    }
    private int color(int id) { return getResources().getColor(id, requireContext().getTheme()); }
    private void render() {
        if (root == null || quiz() == null) return;
        QuizSession s = quiz(); boolean pending = s.pending();
        ((TextView)root.findViewById(R.id.question_progress)).setText(getString(R.string.question_progress, s.index()+1, s.questions.size()));
        ProgressBar progress = root.findViewById(R.id.quiz_progress);
        progress.setMax(s.questions.size()); progress.setProgress(s.index()+1);
        ((TextView)root.findViewById(R.id.question_category)).setText(getString(R.string.category_difficulty, s.current().category, s.config.difficulty));
        ((TextView)root.findViewById(R.id.question_text)).setText(s.current().text);
        LinearLayout choices = root.findViewById(R.id.answer_options); choices.removeAllViews();
        for (int i = 0; i < 4; i++) {
            int choice = i; boolean selected = s.selected(s.index()) == i;
            MaterialButton button = new MaterialButton(requireContext());
            button.setTag("choice_"+i); button.setTextSize(16); button.setAllCaps(false);
            button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            button.setMinHeight(Math.round(64 * getResources().getDisplayMetrics().density));
            button.setCornerRadius(Math.round(16 * getResources().getDisplayMetrics().density));
            button.setStrokeWidth(Math.round(getResources().getDisplayMetrics().density));
            button.setStrokeColor(ColorStateList.valueOf(color(selected ? R.color.primary : R.color.outline)));
            int textColor = R.color.text_primary;
            String label = getString(R.string.option_label, Character.toString((char)('A'+i)), s.current().options.get(i));
            if (!pending && i == s.current().correct) { label += getString(R.string.correct_marker); textColor = R.color.success; }
            else if (!pending && selected) { label += getString(R.string.selected_marker); textColor = R.color.error; }
            button.setText(label); button.setTextColor(ColorStateList.valueOf(color(textColor)));
            button.setBackgroundTintList(ColorStateList.valueOf(color(selected ? R.color.surface_variant : R.color.surface)));
            button.setEnabled(pending && !paused);
            button.setOnClickListener(v -> {
                syncClock(); if (quiz().select(choice)) host().persist(); render();
            });
            choices.addView(button, new LinearLayout.LayoutParams(-1, -2));
        }
        root.findViewById(R.id.resume_question).setVisibility(paused ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.submit_answer).setVisibility(pending ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.submit_answer).setEnabled(!paused && s.selected(s.index()) >= 0);
        root.findViewById(R.id.skip_question).setVisibility(pending ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.skip_question).setEnabled(!paused);
        root.findViewById(R.id.next_question).setVisibility(!pending ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.next_question).setEnabled(!paused);
        ((TextView)root.findViewById(R.id.next_question)).setText(s.index()+1 == s.questions.size() ? R.string.view_results : R.string.next_question);
        TextView feedback = root.findViewById(R.id.answer_feedback), explanation = root.findViewById(R.id.answer_explanation);
        feedback.setVisibility(pending ? View.GONE : View.VISIBLE);
        explanation.setVisibility(!pending && s.config.explanations && !s.current().explanation.isEmpty() ? View.VISIBLE : View.GONE);
        // No answer key or explanation is bound to visible/accessibility text before reveal.
        feedback.setText(pending ? "" : getString(outcomeText(s.outcome(s.index()))));
        feedback.setTextColor(color(s.outcome(s.index()) == QuizSession.Outcome.CORRECT ? R.color.success : R.color.text_primary));
        explanation.setText(pending ? "" : s.current().explanation);
        updateTimer();
    }
    public static int outcomeText(QuizSession.Outcome outcome) {
        switch (outcome) {
            case CORRECT: return R.string.answer_correct;
            case INCORRECT: return R.string.answer_incorrect;
            case SKIPPED: return R.string.answer_skipped;
            case TIMEOUT: return R.string.answer_timeout;
            default: return R.string.answer_pending;
        }
    }
    private void updateTimer() {
        if (root == null) return;
        ((TextView)root.findViewById(R.id.countdown)).setText(getString(R.string.seconds_left, (quiz().remaining()+999)/1000));
        ((TextView)root.findViewById(R.id.timer_status)).setText(paused ? R.string.timer_paused
                : quiz().pending() ? R.string.timer_running : R.string.timer_stopped);
    }
    private void syncClock() {
        if (paused || lastTick < 0 || quiz() == null) return;
        long now = SystemClock.elapsedRealtime(); quiz().elapse(Math.max(0, now-lastTick)); lastTick = now;
    }
    public void pauseQuiz() {
        syncClock(); paused = true; lastTick = -1; handler.removeCallbacks(tick);
        if (quiz() != null && !quiz().complete()) host().persist(); render();
    }
    public void resumeQuiz() {
        paused = false; lastTick = SystemClock.elapsedRealtime(); render(); handler.removeCallbacks(tick); handler.post(tick);
    }
    @Override public void onResume() {
        super.onResume();
        if (!paused) resumeQuiz(); else render();
    }
    @Override public void onPause() { pauseQuiz(); super.onPause(); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { pauseQuiz(); super.onSaveInstanceState(state); }
    @Override public void onDestroyView() { handler.removeCallbacks(tick); root = null; super.onDestroyView(); }
}
