package com.zeke.quizpk.data.local;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.zeke.quizpk.domain.*;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small bundled bank, atomic session snapshots; all access is through this repository. */
public final class QuizDatabase extends SQLiteOpenHelper implements QuestionRepository {
    public static final String NAME = "quizpk_offline.db";
    public static final int VERSION = 3;
    private final Context context;
    public QuizDatabase(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
        this.context = context.getApplicationContext();
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE questions (id TEXT PRIMARY KEY, category TEXT NOT NULL, difficulty TEXT NOT NULL, prompt TEXT NOT NULL UNIQUE, a TEXT NOT NULL, b TEXT NOT NULL, c TEXT NOT NULL, d TEXT NOT NULL, correct INTEGER NOT NULL CHECK(correct BETWEEN 0 AND 3), explanation TEXT NOT NULL, source TEXT NOT NULL)");
        db.execSQL("CREATE INDEX question_filter ON questions(category, difficulty)");
        db.execSQL("CREATE TABLE sessions (id TEXT PRIMARY KEY, completed_at INTEGER NOT NULL, category TEXT NOT NULL, difficulty TEXT NOT NULL, score INTEGER NOT NULL, accuracy INTEGER NOT NULL, question_count INTEGER NOT NULL, duration INTEGER NOT NULL, snapshot TEXT NOT NULL)");
        createDrafts(db);
        createAiTables(db);
        try (InputStream input = context.getAssets().open("questions.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            JSONArray seed = new JSONArray(bytes.toString(StandardCharsets.UTF_8.name()));
            for (int i = 0; i < seed.length(); i++) {
                Question q = Question.fromJson(seed.getJSONObject(i));
                ContentValues v = new ContentValues();
                v.put("id", q.id); v.put("category", q.category); v.put("difficulty", q.difficulty);
                v.put("prompt", q.text);
                for (int j = 0; j < 4; j++) v.put(new String[]{"a", "b", "c", "d"}[j], q.options.get(j));
                v.put("correct", q.correct); v.put("explanation", q.explanation); v.put("source", q.source);
                db.insertOrThrow("questions", null, v);
            }
        } catch (Exception e) { throw new IllegalStateException("Could not initialize question bank", e); }
    }
    private static void createDrafts(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE active_session (slot INTEGER PRIMARY KEY CHECK(slot=1), session_id TEXT NOT NULL, snapshot TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 catalogue/history -> v2 resumable sessions. Never drop user history.
        if (oldVersion == 1 && newVersion >= 2) createDrafts(db);
        if (oldVersion < 3 && newVersion >= 3) createAiTables(db);
    }
    private static void createAiTables(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE questions ADD COLUMN origin TEXT NOT NULL DEFAULT 'OFFLINE_BANK'");
        db.execSQL("CREATE TABLE ai_collections (id TEXT PRIMARY KEY, import_key TEXT NOT NULL UNIQUE, title TEXT NOT NULL, topic TEXT NOT NULL, difficulty TEXT NOT NULL, timer INTEGER NOT NULL, created_at INTEGER NOT NULL, question_count INTEGER NOT NULL, origin TEXT NOT NULL DEFAULT 'AI_GENERATED', model TEXT NOT NULL)");
        db.execSQL("CREATE TABLE ai_questions (id TEXT PRIMARY KEY, collection_id TEXT NOT NULL REFERENCES ai_collections(id) ON DELETE CASCADE, position INTEGER NOT NULL, snapshot TEXT NOT NULL, UNIQUE(collection_id,position))");
        db.execSQL("CREATE TABLE question_reports (question_id TEXT PRIMARY KEY, reported_at INTEGER NOT NULL, reason TEXT NOT NULL, snapshot TEXT NOT NULL)");
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    /** One atomic import per backend endpoint + generation ID, even after Activity/process loss. */
    public String importAi(AiQuiz quiz, String endpoint) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            String key=endpoint+"|"+quiz.generationId;
            try(Cursor c=db.rawQuery("SELECT id FROM ai_collections WHERE import_key=?",new String[]{key})) {
                if(c.moveToFirst()) { db.setTransactionSuccessful();return c.getString(0); }
            }
            db.execSQL("INSERT INTO ai_collections(id,import_key,title,topic,difficulty,timer,created_at,question_count,model) VALUES (?,?,?,?,?,?,?,?,?)",
                    new Object[]{quiz.id,key,quiz.topic,quiz.topic,quiz.difficulty,quiz.seconds,quiz.createdAt,quiz.questions.size(),quiz.model});
            for(int i=0;i<quiz.questions.size();i++) {
                Question q=quiz.questions.get(i);
                db.execSQL("INSERT INTO ai_questions(id,collection_id,position,snapshot) VALUES (?,?,?,?)",new Object[]{q.id,quiz.id,i,q.toJson().toString()});
            }
            db.setTransactionSuccessful();return quiz.id;
        } catch(JSONException e) { throw new IllegalStateException("Could not import quiz"); }
        finally { db.endTransaction(); }
    }
    public List<SavedAiQuiz> savedAi() {
        List<SavedAiQuiz> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM ai_collections ORDER BY created_at DESC,id",null)) {
            while(c.moveToNext()) result.add(saved(c));
        }
        return result;
    }
    private static SavedAiQuiz saved(Cursor c) {
        return new SavedAiQuiz(str(c,"id"),str(c,"title"),str(c,"topic"),str(c,"difficulty"),c.getInt(c.getColumnIndexOrThrow("question_count")),c.getInt(c.getColumnIndexOrThrow("timer")),c.getLong(c.getColumnIndexOrThrow("created_at")));
    }
    public QuizSession aiSession(String id) {
        SavedAiQuiz collection;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM ai_collections WHERE id=?",new String[]{id})) {
            if(!c.moveToFirst()) throw new IllegalStateException("Saved quiz was deleted");collection=saved(c);
        }
        List<Question> questions=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT snapshot FROM ai_questions WHERE collection_id=? ORDER BY position",new String[]{id})) {
            while(c.moveToNext()) questions.add(Question.fromJson(new org.json.JSONObject(c.getString(0))));
        } catch(JSONException e) { throw new IllegalStateException("Saved questions are invalid"); }
        return new QuizSession(new QuizConfig(collection.title,collection.difficulty,collection.count,collection.seconds,true,false,id),questions,System.currentTimeMillis());
    }
    public void renameAi(String id,String title) {
        ContentValues v=new ContentValues();v.put("title",AiQuizRequest.clean(title,3,120));
        if(getWritableDatabase().update("ai_collections",v,"id=?",new String[]{id})!=1)throw new IllegalStateException("Saved quiz missing");
    }
    /** History and active sessions contain independent question snapshots and survive deletion. */
    public void deleteAi(String id) { getWritableDatabase().delete("ai_collections","id=?",new String[]{id}); }
    public void report(Question q,String reason) {
        ContentValues v=new ContentValues();v.put("question_id",q.id);v.put("reported_at",System.currentTimeMillis());
        v.put("reason",AiQuizRequest.clean(reason,3,500));
        try {v.put("snapshot",q.toJson().toString());}catch(JSONException e){throw new IllegalStateException("Report encoding failed");}
        getWritableDatabase().insertWithOnConflict("question_reports",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean reported(String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM question_reports WHERE question_id=?",new String[]{id})) {return c.moveToFirst();}
    }
    private String[] filterArgs(String category, String difficulty) {
        List<String> args = new ArrayList<>();
        if (category != null && !QuizConfig.ALL.equals(category)) args.add(category);
        if (difficulty != null) args.add(difficulty);
        return args.toArray(new String[0]);
    }
    private String filter(String category, String difficulty) {
        List<String> terms = new ArrayList<>();
        if (category != null && !QuizConfig.ALL.equals(category)) terms.add("category=?");
        if (difficulty != null) terms.add("difficulty=?");
        return terms.isEmpty() ? null : android.text.TextUtils.join(" AND ", terms);
    }
    @Override public int count(String category, String difficulty) {
        String where = filter(category, difficulty);
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM questions"
                + (where == null ? "" : " WHERE " + where), filterArgs(category, difficulty))) {
            c.moveToFirst(); return c.getInt(0);
        }
    }
    @Override public List<Question> select(QuizConfig config, Random random) {
        List<Question> qs = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("questions", null,
                filter(config.category, config.difficulty), filterArgs(config.category, config.difficulty),
                null, null, "id ASC")) {
            while (c.moveToNext()) qs.add(new Question(str(c,"id"), str(c,"category"), str(c,"difficulty"),
                    str(c,"prompt"), Arrays.asList(str(c,"a"),str(c,"b"),str(c,"c"),str(c,"d")),
                    c.getInt(c.getColumnIndexOrThrow("correct")), str(c,"explanation"), str(c,"source")));
        }
        if (qs.size() < config.count) throw new IllegalArgumentException("Only " + qs.size() + " matching questions available");
        if (config.randomize) Collections.shuffle(qs, random);
        return new ArrayList<>(qs.subList(0, config.count));
    }
    private static String str(Cursor c, String name) { return c.getString(c.getColumnIndexOrThrow(name)); }
    private static String json(QuizSession s) {
        try { return s.toJson().toString(); }
        catch (JSONException e) { throw new IllegalStateException("Could not encode session", e); }
    }
    private static QuizSession decode(String text) {
        try { return QuizSession.fromJson(text); }
        catch (JSONException | IllegalArgumentException e) { throw new IllegalStateException("Saved quiz is invalid", e); }
    }
    public void saveActive(QuizSession session) {
        if (session.complete()) { finish(session); return; }
        ContentValues v = new ContentValues(); v.put("slot", 1); v.put("session_id", session.id); v.put("snapshot", json(session));
        if (getWritableDatabase().insertWithOnConflict("active_session", null, v, SQLiteDatabase.CONFLICT_REPLACE) == -1)
            throw new IllegalStateException("Could not save active quiz");
    }
    public QuizSession active() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT snapshot FROM active_session WHERE slot=1", null)) {
            return c.moveToFirst() ? decode(c.getString(0)) : null;
        }
    }
    public void discardActive() { getWritableDatabase().delete("active_session", "slot=1", null); }
    public void finish(QuizSession session) {
        if (!session.complete()) throw new IllegalStateException("Quiz is not complete");
        QuizScoring score = new QuizScoring(session);
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.execSQL("INSERT OR IGNORE INTO sessions (id, completed_at, category, difficulty, score, accuracy, question_count, duration, snapshot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{session.id, session.completedAt(), session.config.category, session.config.difficulty, score.score, score.accuracy, session.questions.size(), session.duration(), json(session)});
            db.delete("active_session", "session_id=?", new String[]{session.id}); db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public QuizSession result(String id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT snapshot FROM sessions WHERE id=?", new String[]{id})) {
            return c.moveToFirst() ? decode(c.getString(0)) : null;
        }
    }
    public List<QuizSession> history() {
        List<QuizSession> history = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT snapshot FROM sessions ORDER BY completed_at DESC, id", null)) {
            while (c.moveToNext()) history.add(decode(c.getString(0)));
        }
        return history;
    }
}
