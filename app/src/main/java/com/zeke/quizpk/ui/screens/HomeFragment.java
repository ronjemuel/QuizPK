package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zeke.quizpk.MainActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.ui.components.ModeCard;

public final class HomeFragment extends Fragment {
    public HomeFragment() { super(R.layout.fragment_home); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        MainActivity activity = (MainActivity) requireActivity();
        view.findViewById(R.id.browse_categories).setOnClickListener(v ->
                activity.navigateTo(R.id.nav_explore));
        view.findViewById(R.id.view_profile).setOnClickListener(v ->
                activity.navigateTo(R.id.nav_profile));
        ModeCard.bind(view.findViewById(R.id.offline_card), R.drawable.ic_play,
                R.string.offline_quiz, R.string.offline_description, R.string.offline_status,
                v -> activity.navigateTo(R.id.nav_explore));
        ModeCard.bind(view.findViewById(R.id.ai_card), R.drawable.ic_sparkles,
                R.string.ai_quiz, R.string.ai_description, R.string.backend_required,
                v -> activity.navigateTo(R.id.nav_ai));
        ModeCard.bind(view.findViewById(R.id.multiplayer_card), R.drawable.ic_players,
                R.string.multiplayer, R.string.multiplayer_description, R.string.multiplayer_hint,
                v -> activity.navigateTo(R.id.nav_multiplayer));
    }
    @Override public void onResume() {
        super.onResume();
        View resume = requireView().findViewById(R.id.home_resume);
        if (resume == null) return;
        try (com.zeke.quizpk.data.local.QuizDatabase db = new com.zeke.quizpk.data.local.QuizDatabase(requireContext())) {
            resume.setVisibility(db.active() == null ? View.GONE : View.VISIBLE);
            resume.setOnClickListener(v -> com.zeke.quizpk.OfflineQuizActivity.resume(requireContext()));
        } catch (RuntimeException e) { resume.setVisibility(View.GONE); }
    }
}
