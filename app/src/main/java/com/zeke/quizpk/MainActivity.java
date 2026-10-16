
package com.zeke.quizpk;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.zeke.quizpk.databinding.ActivityMainBinding;
import com.zeke.quizpk.ui.screens.AiQuizFragment;
import com.zeke.quizpk.ui.screens.ExploreFragment;
import com.zeke.quizpk.ui.screens.HomeFragment;
import com.zeke.quizpk.ui.screens.MultiplayerFragment;
import com.zeke.quizpk.ui.screens.ProfileFragment;
import com.zeke.quizpk.ui.screens.SettingsFragment;
import com.zeke.quizpk.ui.theme.ThemePreferences;

/** Native app shell. Offline gameplay is isolated from future online services. */
public final class MainActivity extends AppCompatActivity {
    private static final String SELECTED_TAB = "selected_tab";
    private static final String SETTINGS_TAG = "settings";
    private ActivityMainBinding binding;
    private int selectedTab = R.id.nav_home;
    private OnBackPressedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureInsets();
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(SELECTED_TAB, R.id.nav_home);
        }
        if (binding.bottomNavigation.getMenu().findItem(selectedTab) == null) {
            selectedTab = R.id.nav_home;
        }
        binding.bottomNavigation.setSelectedItemId(selectedTab);
        MenuItemCompat.setContentDescription(
                binding.bottomNavigation.getMenu().findItem(R.id.nav_multiplayer),
                getString(R.string.navigate_multiplayer));
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            showDestination(item.getItemId());
            return true;
        });
        MenuItem settingsItem = binding.toolbar.getMenu().add(R.string.settings);
        settingsItem.setIcon(R.drawable.ic_settings);
        MenuItemCompat.setContentDescription(settingsItem, getString(R.string.open_settings));
        settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            openSettings();
            return true;
        });
        binding.toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isSettingsVisible()) {
                    getSupportFragmentManager().popBackStackImmediate();
                } else {
                    navigateTo(R.id.nav_home);
                }
                updateChrome();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateChrome);
        if (getSupportFragmentManager().findFragmentById(R.id.screen_container) == null) {
            showDestination(selectedTab);
        }
        updateChrome();
    }

    private void configureInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            // The root owns these insets, preventing bottom-bar double padding.
            return WindowInsetsCompat.CONSUMED;
        });
        boolean light = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(getWindow(), binding.appRoot)
                .setAppearanceLightStatusBars(light);
        WindowCompat.getInsetsController(getWindow(), binding.appRoot)
                .setAppearanceLightNavigationBars(light);
        ViewCompat.requestApplyInsets(binding.appRoot);
    }

    public void navigateTo(int destination) {
        binding.bottomNavigation.setSelectedItemId(destination);
    }

    private void showDestination(int destination) {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        Fragment current = manager.findFragmentById(R.id.screen_container);
        if (current == null || !Integer.toString(destination).equals(current.getTag())) {
            Fragment next;
            if (destination == R.id.nav_explore) next = new ExploreFragment();
            else if (destination == R.id.nav_ai) next = new AiQuizFragment();
            else if (destination == R.id.nav_multiplayer) next = new MultiplayerFragment();
            else if (destination == R.id.nav_profile) next = new ProfileFragment();
            else {
                next = new HomeFragment();
                destination = R.id.nav_home;
            }
            manager.beginTransaction().setReorderingAllowed(true)
                    .replace(R.id.screen_container, next, Integer.toString(destination))
                    .commitNow();
        }
        selectedTab = destination;
        updateChrome();
    }

    public void openSettings() {
        if (isSettingsVisible() || getSupportFragmentManager().isStateSaved()) return;
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .replace(R.id.screen_container, new SettingsFragment(), SETTINGS_TAG)
                .addToBackStack(SETTINGS_TAG).commit();
        // Make a rapid second tap observe the existing Settings screen.
        getSupportFragmentManager().executePendingTransactions();
    }

    private boolean isSettingsVisible() {
        return getSupportFragmentManager().findFragmentById(R.id.screen_container)
                instanceof SettingsFragment;
    }

    private void updateChrome() {
        if (binding == null) return;
        boolean settings = isSettingsVisible();
        binding.bottomNavigation.setVisibility(settings ? View.GONE : View.VISIBLE);
        // Drawable overloads accept null; resource overloads try to load resource 0.
        binding.toolbar.setNavigationIcon(settings
                ? AppCompatResources.getDrawable(this, R.drawable.ic_back) : null);
        binding.toolbar.setNavigationContentDescription(R.string.back);
        binding.toolbar.setLogo(settings
                ? null : AppCompatResources.getDrawable(this, R.drawable.ic_quizpk));
        binding.toolbar.getMenu().setGroupVisible(0, !settings);
        int title = R.string.app_name;
        if (settings) title = R.string.settings;
        else if (selectedTab == R.id.nav_explore) title = R.string.explore;
        else if (selectedTab == R.id.nav_ai) title = R.string.ai_quiz;
        else if (selectedTab == R.id.nav_multiplayer) title = R.string.multiplayer;
        else if (selectedTab == R.id.nav_profile) title = R.string.profile;
        binding.toolbar.setTitle(title);
        if (backCallback != null) {
            backCallback.setEnabled(settings || selectedTab != R.id.nav_home);
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intent.getBooleanExtra("open_home", false)) navigateTo(R.id.nav_home);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
