package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.domain.QuizConfig;

public final class QuizConfigFragment extends Fragment {
    private String difficulty = "Easy";
    private int count = 5, seconds = 15;
    private boolean explanations = true, randomize = true;
    public QuizConfigFragment() { super(R.layout.fragment_quiz_config); }
    private OfflineQuizActivity host() { return (OfflineQuizActivity) requireActivity(); }
    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            difficulty = state.getString("difficulty", "Easy"); count = state.getInt("count",5);
            seconds = state.getInt("seconds",15); explanations = state.getBoolean("explanations",true);
            randomize = state.getBoolean("randomize",true);
        }
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        options(view.findViewById(R.id.difficulty_options), QuizConfig.DIFFICULTIES, difficulty);
        options(view.findViewById(R.id.count_options), new String[]{"5","10","15","20"}, Integer.toString(count));
        options(view.findViewById(R.id.timer_options), new String[]{"10","15","20","30"}, Integer.toString(seconds));
        RadioGroup difficulties = view.findViewById(R.id.difficulty_options);
        RadioGroup counts = view.findViewById(R.id.count_options), timers = view.findViewById(R.id.timer_options);
        difficulties.setOnCheckedChangeListener((g,id) -> { difficulty = (String) g.findViewById(id).getTag(); update(view); });
        counts.setOnCheckedChangeListener((g,id) -> { count = Integer.parseInt((String)g.findViewById(id).getTag()); update(view); });
        timers.setOnCheckedChangeListener((g,id) -> seconds = Integer.parseInt((String)g.findViewById(id).getTag()));
        SwitchMaterial explain = view.findViewById(R.id.show_explanations), shuffle = view.findViewById(R.id.randomize_questions);
        explain.setChecked(explanations); shuffle.setChecked(randomize);
        explain.setOnCheckedChangeListener((b, checked) -> explanations = checked);
        shuffle.setOnCheckedChangeListener((b, checked) -> randomize = checked);
        view.findViewById(R.id.begin_quiz).setOnClickListener(v -> host().startQuiz(new QuizConfig(
                host().category, difficulty, count, seconds, explanations, randomize)));
        view.findViewById(R.id.use_available).setOnClickListener(v -> {
            int available = host().database.count(host().category, difficulty);
            for (int n : new int[]{20,15,10,5}) if (n <= available) { count = n; break; }
            for (int i = 0; i < counts.getChildCount(); i++) {
                RadioButton button = (RadioButton)counts.getChildAt(i);
                if (Integer.toString(count).equals(button.getTag())) counts.check(button.getId());
            }
            update(view);
        });
        view.findViewById(R.id.use_mix).setOnClickListener(v -> { host().category = QuizConfig.ALL; update(view); });
        update(view);
    }
    private void options(RadioGroup group, String[] labels, String checked) {
        boolean horizontal = group.getOrientation() == LinearLayout.HORIZONTAL;
        for (String label : labels) {
            MaterialRadioButton b = new MaterialRadioButton(requireContext());
            b.setId(View.generateViewId()); b.setTag(label); b.setText(label); b.setTextSize(horizontal ? 14 : 16);
            b.setMinHeight(dp(64)); b.setMinWidth(0);
            b.setButtonDrawable((android.graphics.drawable.Drawable)null);
            b.setGravity(android.view.Gravity.CENTER);
            b.setBackgroundResource(R.drawable.bg_answer);
            if (!horizontal) {
                b.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_difficulty,0,0,0);
                b.setCompoundDrawablePadding(dp(12));
                b.setGravity(android.view.Gravity.CENTER_VERTICAL);
            }
            b.setPadding(dp(horizontal ? 4 : 16),dp(8),dp(horizontal ? 4 : 16),dp(8));
            b.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
            RadioGroup.LayoutParams p = new RadioGroup.LayoutParams(horizontal ? 0 : -1, -2);
            if (horizontal) p.weight = 1;
            p.setMargins(dp(2),dp(4),dp(2),dp(4));
            group.addView(b, p); if (label.equals(checked)) group.check(b.getId());
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void update(View view) {
        int available = host().database.count(host().category, difficulty);
        ((TextView)view.findViewById(R.id.config_category)).setText(host().category);
        ((TextView)view.findViewById(R.id.available_count)).setText(getString(R.string.available_for_difficulty, available, difficulty));
        boolean valid = available >= count;
        view.findViewById(R.id.begin_quiz).setEnabled(valid);
        ((TextView)view.findViewById(R.id.config_limit)).setText(valid
                ? getResources().getQuantityString(R.plurals.ready_count, count, count) : getResources().getQuantityString(R.plurals.insufficient_questions, available, available, count));
        view.findViewById(R.id.use_available).setVisibility(!valid && available >= 5 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.use_mix).setVisibility(!valid && !QuizConfig.ALL.equals(host().category) ? View.VISIBLE : View.GONE);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        out.putString("difficulty",difficulty); out.putInt("count",count); out.putInt("seconds",seconds);
        out.putBoolean("explanations",explanations); out.putBoolean("randomize",randomize);
        super.onSaveInstanceState(out);
    }
}
