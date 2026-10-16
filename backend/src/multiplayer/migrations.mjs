// Separate migration ledger: M3 users, jobs, throttle and PRAGMA user_version stay intact.
export function migrateMultiplayer(db) {
  db.exec('PRAGMA foreign_keys=ON; BEGIN IMMEDIATE');
  try {
    db.exec('CREATE TABLE IF NOT EXISTS mp_schema(version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL)');
    const version = db.prepare('SELECT max(version) AS v FROM mp_schema').get().v || 0;
    if (version > 2) throw Error('UNSUPPORTED_ROOM_SCHEMA');
    if (version < 1) {
      db.exec([
        "CREATE TABLE mp_rooms(id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE CHECK(length(code)=6), mode TEXT NOT NULL CHECK(mode IN ('CLASSIC','MODERATOR')), owner_id TEXT NOT NULL REFERENCES users(id), settings TEXT NOT NULL, state TEXT NOT NULL DEFAULT 'LOBBY' CHECK(state IN ('LOBBY','CLOSED','EXPIRED')), revision INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, terminal_at INTEGER)",
        "CREATE TABLE mp_members(room_id TEXT NOT NULL REFERENCES mp_rooms(id) ON DELETE CASCADE, user_id TEXT NOT NULL REFERENCES users(id), participant_id TEXT NOT NULL UNIQUE, role TEXT NOT NULL CHECK(role IN ('CONTESTANT','MODERATOR')), display_name TEXT NOT NULL, ready INTEGER NOT NULL DEFAULT 0, joined_at INTEGER NOT NULL, left_at INTEGER, last_seen INTEGER NOT NULL, online INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(room_id,user_id))",
        "CREATE UNIQUE INDEX mp_single_moderator ON mp_members(room_id) WHERE role='MODERATOR' AND left_at IS NULL",
        'CREATE INDEX mp_members_user ON mp_members(user_id,left_at)',
        'CREATE INDEX mp_expiry ON mp_rooms(state,expires_at)',
        'CREATE TABLE mp_requests(user_id TEXT NOT NULL REFERENCES users(id), request_id TEXT NOT NULL, fingerprint TEXT NOT NULL, response TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY(user_id,request_id))',
        'CREATE TABLE mp_limits(key TEXT PRIMARY KEY, bucket INTEGER NOT NULL, count INTEGER NOT NULL, expires_at INTEGER NOT NULL)'
      ].join(';'));
      db.prepare('INSERT INTO mp_schema VALUES (1,?)').run(Date.now());
    }
    if (version < 2) {
      db.exec(`
        CREATE TABLE mp_matches(room_id TEXT PRIMARY KEY REFERENCES mp_rooms(id) ON DELETE CASCADE,
          phase TEXT NOT NULL CHECK(phase IN ('ROUND_PREPARED','ROUND_LIVE','ROUND_REVEALED','MATCH_FINISHED')),
          round_index INTEGER NOT NULL, question_count INTEGER NOT NULL, started_at INTEGER NOT NULL,
          finished_at INTEGER, finish_reason TEXT);
        CREATE TABLE mp_match_players(room_id TEXT NOT NULL REFERENCES mp_matches(room_id) ON DELETE CASCADE,
          user_id TEXT NOT NULL REFERENCES users(id), participant_id TEXT NOT NULL UNIQUE,
          display_name TEXT NOT NULL, score INTEGER NOT NULL DEFAULT 0, correct_count INTEGER NOT NULL DEFAULT 0,
          fastest_wins INTEGER NOT NULL DEFAULT 0, correct_elapsed INTEGER NOT NULL DEFAULT 0,
          dismissed_at INTEGER, PRIMARY KEY(room_id,user_id));
        CREATE TABLE mp_rounds(id TEXT PRIMARY KEY, room_id TEXT NOT NULL REFERENCES mp_matches(room_id) ON DELETE CASCADE,
          position INTEGER NOT NULL, state TEXT NOT NULL CHECK(state IN ('QUEUED','ROUND_PREPARED','ROUND_LIVE','ROUND_REVEALED','CANCELLED')),
          question_json TEXT NOT NULL, started_at INTEGER, deadline INTEGER, reveal_until INTEGER,
          UNIQUE(room_id,position));
        CREATE TABLE mp_answer_keys(round_id TEXT PRIMARY KEY REFERENCES mp_rounds(id) ON DELETE CASCADE,
          correct_index INTEGER NOT NULL CHECK(correct_index BETWEEN 0 AND 3), explanation TEXT NOT NULL);
        CREATE TABLE mp_answers(round_id TEXT NOT NULL REFERENCES mp_rounds(id) ON DELETE CASCADE,
          participant_id TEXT NOT NULL REFERENCES mp_match_players(participant_id) ON DELETE CASCADE,
          selected_option INTEGER NOT NULL CHECK(selected_option BETWEEN 0 AND 3), accepted_at INTEGER NOT NULL,
          elapsed_ms INTEGER NOT NULL, sequence INTEGER NOT NULL, correct INTEGER, points INTEGER,
          PRIMARY KEY(round_id,participant_id), UNIQUE(round_id,sequence));
        CREATE INDEX mp_match_users ON mp_match_players(user_id,dismissed_at);
        CREATE INDEX mp_match_finished ON mp_matches(phase,finished_at);
      `);
      db.prepare('INSERT INTO mp_schema VALUES (2,?)').run(Date.now());
    }
    db.exec('COMMIT');
  } catch (e) { db.exec('ROLLBACK'); throw e; }
}
