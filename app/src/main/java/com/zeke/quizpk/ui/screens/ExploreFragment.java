package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.zeke.quizpk.R;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.domain.QuizConfig;
import com.zeke.quizpk.data.local.QuizDatabase;

/** Bundled offline library. Counts come from the local repository. */
public final class ExploreFragment extends Fragment {
    private static final String SELECTION = "category_selection";
    private static final int[] NAMES = {R.string.general, R.string.science, R.string.history,
            R.string.geography, R.string.technology, R.string.sports, R.string.movies,
            R.string.music, R.string.literature, R.string.animals, R.string.food, R.string.space};
    private static final int[] ICONS = {R.drawable.ic_general, R.drawable.ic_science,
            R.drawable.ic_history, R.drawable.ic_geography, R.drawable.ic_chip,
            R.drawable.ic_sports, R.drawable.ic_movies, R.drawable.ic_music,
            R.drawable.ic_book, R.drawable.ic_animals, R.drawable.ic_food, R.drawable.ic_space};
    private int selected = -1;
    public ExploreFragment() { super(R.layout.fragment_explore); }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) selected = state.getInt(SELECTION, -1);
        if (selected < -1 || selected >= NAMES.length) selected = -1;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        GridLayout grid = view.findViewById(R.id.category_grid);
        grid.setColumnCount(getResources().getConfiguration().screenWidthDp >= 360 ? 3 : 2);
        int[] counts = new int[NAMES.length];
        int total;
        try (QuizDatabase db = new QuizDatabase(requireContext())) {
            total = db.count(QuizConfig.ALL, null);
            for (int i = 0; i < counts.length; i++) counts[i] = db.count(QuizConfig.CATEGORIES[i], null);
        } catch (RuntimeException error) {
            ((TextView)view.findViewById(R.id.selected_category)).setText(R.string.bank_error);
            view.findViewById(R.id.start_quiz).setEnabled(false); return;
        }
        ((TextView)view.findViewById(R.id.all_categories)).setText(getString(R.string.all_categories_count, getString(R.string.all_categories),
                getResources().getQuantityString(R.plurals.available_questions, total, total)));
        view.findViewById(R.id.all_categories).setOnClickListener(v -> { selected = -1; updateSelection(view, grid); });
        view.findViewById(R.id.start_quiz).setOnClickListener(v -> OfflineQuizActivity.open(requireContext(),
                selected < 0 ? QuizConfig.ALL : QuizConfig.CATEGORIES[selected]));
        for (int i = 0; i < NAMES.length; i++) {
            int index = i;
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.component_category, grid, false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int margin = Math.round(4 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            card.setLayoutParams(params);
            ((ImageView) card.findViewById(R.id.category_icon)).setImageResource(ICONS[i]);
            ((TextView) card.findViewById(R.id.category_name)).setText(NAMES[i]);
            ((TextView)card.findViewById(R.id.category_count)).setText(getResources().getQuantityString(R.plurals.category_questions, counts[i], counts[i]));
            card.setContentDescription(getResources().getQuantityString(R.plurals.category_count_accessibility, counts[i], getString(NAMES[i]), counts[i]));
            card.setOnClickListener(v -> {
                selected = index;
                updateSelection(view, grid);
            });
            grid.addView(card);
        }
        ViewCompat.setAccessibilityLiveRegion(view.findViewById(R.id.selected_category),
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        updateSelection(view, grid);
    }

    private void updateSelection(View view, GridLayout grid) {
        view.findViewById(R.id.all_categories).setSelected(selected < 0);
        androidx.core.view.ViewCompat.setStateDescription(view.findViewById(R.id.all_categories),
                selected < 0 ? getString(R.string.category_selected, getString(R.string.all_categories)) : null);
        for (int i = 0; i < grid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) grid.getChildAt(i);
            boolean active = i == selected;
            card.setSelected(active);
            card.setStrokeColor(getResources().getColor(active ? R.color.primary
                    : R.color.outline, requireContext().getTheme()));
            ViewCompat.setStateDescription(card, active
                    ? getString(R.string.category_selected, getString(NAMES[i])) : null);
        }
        ((TextView) view.findViewById(R.id.selected_category)).setText(selected < 0
                ? getString(R.string.category_selected, getString(R.string.all_categories))
                : getString(R.string.category_selected, getString(NAMES[selected])));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SELECTION, selected);
        super.onSaveInstanceState(outState);
    }
}
