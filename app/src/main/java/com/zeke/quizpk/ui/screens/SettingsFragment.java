package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zeke.quizpk.R;
import com.zeke.quizpk.ui.theme.ThemePreferences;

public final class SettingsFragment extends Fragment {
    public SettingsFragment() { super(R.layout.fragment_settings); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        RadioGroup choices = view.findViewById(R.id.theme_choices);
        // Initialize before listening to avoid a recreation loop.
        choices.check(ThemePreferences.isLight(requireContext())
                ? R.id.theme_light : R.id.theme_dark);
        choices.setOnCheckedChangeListener((group, checkedId) -> {
            boolean light = checkedId == R.id.theme_light;
            if (light != ThemePreferences.isLight(requireContext())) {
                ThemePreferences.setLight(requireContext(), light);
            }
        });
    }
}
