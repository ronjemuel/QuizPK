package com.zeke.quizpk.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/** Explicit persisted choice, independent of the device's default appearance. */
public final class ThemePreferences {
    private static final String FILE = "quizpk_appearance";
    private static final String LIGHT = "light_theme";
    private ThemePreferences() { }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isLight(Context context) {
        return preferences(context).getBoolean(LIGHT, false);
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(isLight(context)
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
    }

    public static void setLight(Context context, boolean light) {
        preferences(context).edit().putBoolean(LIGHT, light).apply();
        applySavedTheme(context);
    }
}
