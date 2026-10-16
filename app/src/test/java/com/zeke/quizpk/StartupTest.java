package com.zeke.quizpk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.widget.RadioGroup;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.zeke.quizpk.ui.screens.HomeFragment;
import com.zeke.quizpk.ui.screens.ExploreFragment;
import com.zeke.quizpk.ui.screens.AiQuizFragment;
import com.zeke.quizpk.ui.screens.MultiplayerFragment;
import com.zeke.quizpk.ui.screens.ProfileFragment;
import com.zeke.quizpk.ui.screens.SettingsFragment;
import com.zeke.quizpk.ui.theme.ThemePreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

/** Android 14 JVM regression; this is not a physical-device/emulator test. */
@org.robolectric.annotation.SQLiteMode(org.robolectric.annotation.SQLiteMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class StartupTest {
    @Before
    public void resetAppearance() {
        HostSqliteSupport.install();
        RuntimeEnvironment.getApplication()
                .getSharedPreferences("quizpk_appearance", Context.MODE_PRIVATE)
                .edit().clear().commit();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Test
    public void coldStartInflatesHomeAndBranding() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertTrue(activity.getSupportFragmentManager()
                    .findFragmentById(R.id.screen_container) instanceof HomeFragment);
            assertNotNull(activity.findViewById(R.id.browse_categories));
            MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
            assertNotNull(toolbar.getLogo());
            assertNull(toolbar.getNavigationIcon());
            assertFalse(activity.isFinishing());
        }
    }
    @Test
    public void homeCardsAndAllTabsNavigateToNativeScreens() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertTrue(activity.findViewById(R.id.offline_card).performClick());
            assertDestination(activity, ExploreFragment.class);
            activity.navigateTo(R.id.nav_home);
            assertTrue(activity.findViewById(R.id.ai_card).performClick());
            assertDestination(activity, AiQuizFragment.class);
            activity.navigateTo(R.id.nav_home);
            assertTrue(activity.findViewById(R.id.multiplayer_card).performClick());
            assertDestination(activity, MultiplayerFragment.class);
            assertFalse(activity.findViewById(R.id.create_room).isEnabled());
            activity.navigateTo(R.id.nav_home);
            assertTrue(activity.findViewById(R.id.view_profile).performClick());
            assertDestination(activity, ProfileFragment.class);
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertDestination(activity, HomeFragment.class);
        }
    }

    @Test
    public void settingsClearsLogoAndBackRestoresProfileWithoutZeroLookup() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.navigateTo(R.id.nav_profile);
            assertTrue(activity.findViewById(R.id.profile_settings).performClick());
            assertDestination(activity, SettingsFragment.class);
            MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
            assertNull(toolbar.getLogo());
            assertNotNull(toolbar.getNavigationIcon());
            activity.openSettings();
            assertEquals(1, activity.getSupportFragmentManager().getBackStackEntryCount());
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertDestination(activity, ProfileFragment.class);
            assertNotNull(toolbar.getLogo());
            assertNull(toolbar.getNavigationIcon());
        }
    }

    @Test
    public void settingsMenuOpensAndBackReturnsHome() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
            assertTrue(toolbar.getMenu().performIdentifierAction(0, 0));
            assertDestination(activity, SettingsFragment.class);
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertDestination(activity, HomeFragment.class);
        }
    }

    @Test
    public void lightAndDarkChoicesPersistAcrossActivityRelaunches() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.openSettings();
            // CompoundButton toggles even when performClick returns false (no OnClickListener).
            activity.findViewById(R.id.theme_light).performClick();
            // Finish AppCompat's queued recreation before closing this controller.
            ShadowLooper.idleMainLooper();
            activity = controller.get();
            assertDestination(activity, SettingsFragment.class);
            assertEquals(Configuration.UI_MODE_NIGHT_NO,
                    activity.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            assertTrue(ThemePreferences.isLight(activity));
        }
        // Reset process-global delegate state; the next activity must load the stored choice.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertTrue(ThemePreferences.isLight(activity));
            assertEquals(Configuration.UI_MODE_NIGHT_NO,
                    activity.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            activity.openSettings();
            RadioGroup choices = activity.findViewById(R.id.theme_choices);
            assertEquals(R.id.theme_light, choices.getCheckedRadioButtonId());
            activity.findViewById(R.id.theme_dark).performClick();
            ShadowLooper.idleMainLooper();
            activity = controller.get();
            assertDestination(activity, SettingsFragment.class);
            assertEquals(Configuration.UI_MODE_NIGHT_YES,
                    activity.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            assertFalse(ThemePreferences.isLight(activity));
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertFalse(ThemePreferences.isLight(activity));
            assertEquals(Configuration.UI_MODE_NIGHT_YES,
                    activity.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK);
            activity.openSettings();
            RadioGroup choices = activity.findViewById(R.id.theme_choices);
            assertEquals(R.id.theme_dark, choices.getCheckedRadioButtonId());
        }
    }

    @Test
    public void repeatedLaunchAndWarmLifecycleReturnToHome() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try (ActivityController<MainActivity> controller =
                         Robolectric.buildActivity(MainActivity.class).setup()) {
                MainActivity activity = controller.get();
                assertDestination(activity, HomeFragment.class);
                controller.pause().stop().restart().start().resume().postResume();
                assertDestination(activity, HomeFragment.class);
                assertFalse(activity.isFinishing());
            }
        }
    }

    @Test
    public void recreationRestoresSettingsAndItsPreviousDestination() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.navigateTo(R.id.nav_explore);
            activity.openSettings();
            controller.recreate();
            activity = controller.get();
            assertDestination(activity, SettingsFragment.class);
            MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
            assertNull(toolbar.getLogo());
            assertNotNull(toolbar.getNavigationIcon());
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertDestination(activity, ExploreFragment.class);
            assertNotNull(toolbar.getLogo());
            assertNull(toolbar.getNavigationIcon());
        }
    }

    private static void assertDestination(MainActivity activity,
                                           Class<? extends Fragment> destination) {
        assertTrue(destination.isInstance(activity.getSupportFragmentManager()
                .findFragmentById(R.id.screen_container)));
    }
}
