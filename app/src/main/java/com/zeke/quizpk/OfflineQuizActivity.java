package com.zeke.quizpk;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.ui.screens.*;
import com.zeke.quizpk.ui.theme.ThemePreferences;
import java.util.Random;

/** Immersive offline flow within the existing application. No bottom tabs while timed. */
public final class OfflineQuizActivity extends AppCompatActivity {
    public QuizDatabase database;
    public QuizSession session;
    public String category = QuizConfig.ALL;
    private String screen = "config";
    private boolean storageFailed;
    public static void open(Context context, String category) {
        context.startActivity(new Intent(context, OfflineQuizActivity.class).putExtra("category", category));
    }
    public static void openAi(Context context, String collectionId) {
        context.startActivity(new Intent(context, OfflineQuizActivity.class).putExtra("ai_collection",collectionId));
    }
    public static void resume(Context context) {
        context.startActivity(new Intent(context, OfflineQuizActivity.class).putExtra("resume", true));
    }
    public static void review(Context context, String id) {
        context.startActivity(new Intent(context, OfflineQuizActivity.class).putExtra("result", id));
    }
    @Override protected void onCreate(Bundle saved) {
        ThemePreferences.applySavedTheme(this); super.onCreate(saved);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        database = new QuizDatabase(this);
        try {
            category = saved != null ? saved.getString("category", QuizConfig.ALL)
                    : getIntent().getStringExtra("category");
            if (category == null) category = QuizConfig.ALL;
            if (saved != null) {
                screen = saved.getString("screen", "config");
                String result = saved.getString("session_id");
                session = "play".equals(screen) ? database.active()
                        : result == null ? null : database.result(result);
            } else if (getIntent().hasExtra("result")) {
                session = database.result(getIntent().getStringExtra("result")); screen = "results";
            } else if (getIntent().getBooleanExtra("resume", false)) {
                session = database.active(); screen = "play";
            }
            if (!"config".equals(screen) && session == null) screen = "config";

        } catch (RuntimeException e) {
            setContentView(R.layout.activity_offline); storageError(e); return;
        }
        setContentView(R.layout.activity_offline);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.offline_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return WindowInsetsCompat.CONSUMED;
        });
        boolean light = ThemePreferences.isLight(this);
        WindowCompat.getInsetsController(getWindow(), findViewById(R.id.offline_root)).setAppearanceLightStatusBars(light);
        WindowCompat.getInsetsController(getWindow(), findViewById(R.id.offline_root)).setAppearanceLightNavigationBars(light);
        MaterialToolbar bar = findViewById(R.id.offline_toolbar);
        bar.setNavigationOnClickListener(v -> leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leave(); }
        });
        if (getSupportFragmentManager().findFragmentById(R.id.offline_container) == null) {
            if ("config".equals(screen) && getIntent().hasExtra("ai_collection")) startAiQuiz(getIntent().getStringExtra("ai_collection"));
            else show(screen);
        }
    }

    public void startAiQuiz(String collectionId) {
        try {
            if(database.active()!=null) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.saved_quiz_title).setMessage(R.string.replace_quiz_body)
                        .setPositiveButton(R.string.resume_quiz,(d,w)->{session=database.active();show("play");})
                        .setNegativeButton(R.string.replace_quiz,(d,w)->createAiQuiz(collectionId))
                        .setNeutralButton(android.R.string.cancel,(d,w)->finish()).setOnCancelListener(d->finish()).show();
            } else createAiQuiz(collectionId);
        } catch(RuntimeException e){storageError(e);}
    }
    private void createAiQuiz(String id) {
        try {session=database.aiSession(id);category=session.config.category;database.saveActive(session);show("play");}
        catch(RuntimeException e){storageError(e);}
    }
    public void replay(QuizSession previous) {
        if(previous.config.collectionId==null) {category=previous.config.category;show("config");}
        else {
            // Replay from the persisted result even when its original collection was deleted.
            try {QuizSession replay=new QuizSession(previous.config,previous.questions,System.currentTimeMillis());
                if(database.active()!=null) {
                    new MaterialAlertDialogBuilder(this).setTitle(R.string.saved_quiz_title).setMessage(R.string.replace_quiz_body)
                            .setPositiveButton(R.string.resume_quiz,(d,w)->{session=database.active();show("play");})
                            .setNegativeButton(R.string.replace_quiz,(d,w)->{try{session=replay;database.saveActive(session);show("play");}catch(RuntimeException e){storageError(e);}})
                            .setNeutralButton(android.R.string.cancel,(d,w)->{session=previous;}).setOnCancelListener(d->session=previous).show();
                } else {session=replay;database.saveActive(session);show("play");}
            } catch(RuntimeException e){storageError(e);}
        }
    }
    public void startQuiz(QuizConfig config) {
        try {
            if (database.active() != null) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.saved_quiz_title)
                        .setMessage(R.string.replace_quiz_body)
                        .setPositiveButton(R.string.resume_quiz, (d, w) -> {
                            session = database.active(); show("play");
                        }).setNegativeButton(R.string.replace_quiz, (d, w) -> createQuiz(config))
                        .setNeutralButton(android.R.string.cancel, null).show();
            } else createQuiz(config);
        } catch (RuntimeException e) { storageError(e); }
    }
    private void createQuiz(QuizConfig config) {
        try {
            session = new QuizSession(config, database.select(config, new Random()), System.currentTimeMillis());
            database.saveActive(session); show("play");
        } catch (RuntimeException e) { storageError(e); }
    }
    public void show(String destination) {
        screen = destination;
        ((MaterialToolbar)findViewById(R.id.offline_toolbar)).setTitle(session!=null && session.config.collectionId!=null ? R.string.ai_quiz : R.string.offline_quiz);
        Fragment fragment;
        if ("play".equals(screen)) fragment = new GameplayFragment();
        else if ("results".equals(screen)) fragment = new ResultsFragment();
        else if ("review".equals(screen)) fragment = new ReviewFragment();
        else fragment = new QuizConfigFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.offline_container, fragment).commitNow();
    }
    public boolean persist() {
        if (storageFailed) return false;
        try { if (session != null) database.saveActive(session); return true; }
        catch (RuntimeException e) { storageError(e); return false; }
    }
    private void leave() {
        if ("play".equals(screen) && session != null && !session.complete()) {
            GameplayFragment game = (GameplayFragment) getSupportFragmentManager().findFragmentById(R.id.offline_container);
            if (game != null) game.pauseQuiz();
            new MaterialAlertDialogBuilder(this).setTitle(R.string.leave_quiz)
                    .setMessage(R.string.leave_quiz_body)
                    .setPositiveButton(R.string.save_exit, (d,w) -> finish())
                    .setNegativeButton(R.string.discard_quiz, (d,w) -> {
                        try { database.discardActive(); session = null; finish(); }
                        catch (RuntimeException e) { storageError(e); }
                    })
                    .setNeutralButton(R.string.continue_quiz, (d,w) -> { if (game != null) game.resumeQuiz(); })
                    .setOnCancelListener(d -> { if (game != null) game.resumeQuiz(); }).show();
        } else if ("review".equals(screen)) show("results");
        else finish();
    }
    public void home() {
        startActivity(new Intent(this, MainActivity.class).putExtra("open_home", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    public void storageError(RuntimeException error) {
        if (storageFailed) return;
        storageFailed = true;
        android.util.Log.e("QuizPK", "Local storage operation failed");
        Fragment game = getSupportFragmentManager().findFragmentById(R.id.offline_container);
        if (game instanceof GameplayFragment) ((GameplayFragment)game).pauseQuiz();
        new MaterialAlertDialogBuilder(this).setTitle(R.string.storage_error)
                .setMessage(R.string.storage_error_body).setPositiveButton(android.R.string.ok, (d,w) -> finish()).show();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (session != null) { persist(); out.putString("session_id", session.id); }
        out.putString("screen", screen); out.putString("category", category);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        super.onDestroy(); if (database != null) database.close();
    }
}
