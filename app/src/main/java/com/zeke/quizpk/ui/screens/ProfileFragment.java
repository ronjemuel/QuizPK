package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.card.MaterialCardView;
import com.zeke.quizpk.MainActivity;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.domain.*;
import java.util.List;
import java.text.DateFormat;
import java.util.Date;

public final class ProfileFragment extends Fragment {
    public ProfileFragment() { super(R.layout.fragment_profile); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        view.findViewById(R.id.profile_settings).setOnClickListener(v -> ((MainActivity)requireActivity()).openSettings());
        view.findViewById(R.id.profile_resume).setOnClickListener(v -> OfflineQuizActivity.resume(requireContext()));
    }
    @Override public void onResume() {
        super.onResume(); View view = requireView();
        try (QuizDatabase db = new QuizDatabase(requireContext())) {
            List<QuizSession> history = db.history();
            view.findViewById(R.id.history_empty).setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.profile_resume).setVisibility(db.active() != null ? View.VISIBLE : View.GONE);
            int points = 0; for (QuizSession s : history) points += new QuizScoring(s).score;
            ((TextView)view.findViewById(R.id.profile_summary)).setText(history.isEmpty()
                    ? getString(R.string.profile_body) : getString(R.string.history_summary, history.size(), points));
            RecyclerView list = view.findViewById(R.id.history_list);
            list.setLayoutManager(new LinearLayoutManager(requireContext())); list.setAdapter(new HistoryAdapter(history));
        } catch (RuntimeException error) {
            ((TextView)view.findViewById(R.id.profile_summary)).setText(R.string.bank_error);
        }
    }
    private final class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
        private final List<QuizSession> sessions;
        HistoryAdapter(List<QuizSession> sessions) { this.sessions = sessions; }
        @NonNull @Override public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            MaterialCardView card = new MaterialCardView(parent.getContext());
            TextView text = new TextView(parent.getContext()); text.setTextAppearance(R.style.Text_QuizPK_Body);
            int pad = Math.round(16 * getResources().getDisplayMetrics().density);
            text.setPadding(pad,pad,pad,pad); card.addView(text); card.setClickable(true); card.setFocusable(true);
            RecyclerView.LayoutParams p = new RecyclerView.LayoutParams(-1,-2); p.bottomMargin = pad; card.setLayoutParams(p);
            return new HistoryHolder(card,text);
        }
        @Override public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
            QuizSession s = sessions.get(position); QuizScoring score = new QuizScoring(s);
            holder.text.setText(getString(R.string.history_item, s.config.category, s.config.difficulty,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(s.completedAt())),
                    score.correct, s.questions.size(), score.accuracy, ResultsFragment.duration(s.duration())));
            holder.itemView.setOnClickListener(v -> OfflineQuizActivity.review(requireContext(), s.id));
        }
        @Override public int getItemCount() { return sessions.size(); }
    }
    private static final class HistoryHolder extends RecyclerView.ViewHolder {
        final TextView text;
        HistoryHolder(View card, TextView text) { super(card); this.text = text; }
    }
}
