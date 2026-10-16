package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.domain.*;

public final class ResultsFragment extends Fragment {
    public ResultsFragment() { super(R.layout.fragment_results); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        OfflineQuizActivity host = (OfflineQuizActivity)requireActivity();
        QuizSession s = host.session; QuizScoring score = new QuizScoring(s);
        ((TextView)view.findViewById(R.id.result_context)).setText(getString(R.string.category_difficulty, s.config.category, s.config.difficulty));
        ((TextView)view.findViewById(R.id.result_score)).setText(getString(R.string.score_points, score.score, QuizScoring.points(s.questions.size())));
        ((TextView)view.findViewById(R.id.result_metrics)).setText(getString(R.string.result_metrics,
                score.correct, score.incorrect, score.unanswered, score.accuracy, duration(s.duration())));
        view.findViewById(R.id.review_answers).setOnClickListener(v -> host.show("review"));
        view.findViewById(R.id.play_again).setOnClickListener(v -> { host.replay(s); });
        view.findViewById(R.id.results_home).setOnClickListener(v -> host.home());
    }
    public static String duration(long millis) {
        long seconds = millis / 1000;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", seconds/60, seconds%60);
    }
}
