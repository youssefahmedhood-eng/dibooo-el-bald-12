-- MedSport Monitor — SQLite Database Schema
-- Version 1.0.0

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

-- ============================================================
-- USERS & AUTHENTICATION
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          TEXT PRIMARY KEY,
    email       TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role        TEXT NOT NULL CHECK(role IN ('ATHLETE', 'COACH', 'MEDICAL')),
    name        TEXT NOT NULL,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    last_login  TEXT
);

CREATE TABLE IF NOT EXISTS athlete_profiles (
    user_id     TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    birth_date  TEXT,
    sex         TEXT CHECK(sex IN ('M', 'F', 'OTHER')),
    sport       TEXT,
    height_cm   REAL,
    weight_kg   REAL,
    hr_max      INTEGER,
    resting_hr  INTEGER,
    photo_path  TEXT
);

CREATE TABLE IF NOT EXISTS coach_profiles (
    user_id         TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    sport           TEXT,
    years_experience INTEGER,
    institution     TEXT
);

CREATE TABLE IF NOT EXISTS medical_profiles (
    user_id         TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    specialty       TEXT,
    license_number  TEXT,
    institution     TEXT
);

-- ============================================================
-- TEAMS
-- ============================================================

CREATE TABLE IF NOT EXISTS teams (
    id                      TEXT PRIMARY KEY,
    name                    TEXT NOT NULL,
    sport                   TEXT NOT NULL,
    category                TEXT CHECK(category IN ('PROFESSIONAL','AMATEUR','UNIVERSITY','YOUTH')),
    description             TEXT,
    logo_path               TEXT,
    primary_color           TEXT DEFAULT '#0A84FF',
    secondary_color         TEXT DEFAULT '#00D4FF',
    coach_id                TEXT NOT NULL REFERENCES users(id),
    join_code               TEXT UNIQUE NOT NULL,
    hr_max_reference        INTEGER DEFAULT 190,
    spo2_alert_threshold    REAL DEFAULT 93.0,
    hr_alert_threshold      REAL DEFAULT 95.0,
    weekly_load_threshold   REAL DEFAULT 800.0,
    emergency_protocol      TEXT DEFAULT 'Call 112 immediately. Do not move the athlete.',
    created_at              TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS team_members (
    team_id     TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_team TEXT NOT NULL CHECK(role_in_team IN ('ATHLETE','COACH','MEDICAL')),
    joined_at   TEXT NOT NULL DEFAULT (datetime('now')),
    is_active   INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (team_id, user_id)
);

-- ============================================================
-- TRAINING SESSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS training_sessions (
    id              TEXT PRIMARY KEY,
    team_id         TEXT REFERENCES teams(id),
    coach_id        TEXT REFERENCES users(id),
    start_time      TEXT,
    end_time        TEXT,
    exercise_type   TEXT NOT NULL,
    target_zone_min INTEGER DEFAULT 2,
    target_zone_max INTEGER DEFAULT 4,
    notes           TEXT,
    status          TEXT NOT NULL DEFAULT 'PLANNED'
                    CHECK(status IN ('PLANNED','ACTIVE','COMPLETED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS session_participants (
    session_id      TEXT NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    athlete_id      TEXT NOT NULL REFERENCES users(id),
    actual_start    TEXT,
    actual_end      TEXT,
    PRIMARY KEY (session_id, athlete_id)
);

-- ============================================================
-- SENSOR DATA
-- ============================================================

CREATE TABLE IF NOT EXISTS sensor_readings (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    athlete_id  TEXT NOT NULL REFERENCES users(id),
    session_id  TEXT REFERENCES training_sessions(id),
    timestamp   TEXT NOT NULL,
    sensor_type TEXT NOT NULL
                CHECK(sensor_type IN ('hr','spo2','gps_lat','gps_lon','gps_speed',
                                      'accel_x','accel_y','accel_z','cadence','gps_alt')),
    value       REAL NOT NULL,
    unit        TEXT,
    quality     REAL DEFAULT 1.0
);

CREATE INDEX IF NOT EXISTS idx_sensor_athlete_time
    ON sensor_readings(athlete_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_sensor_session
    ON sensor_readings(session_id, sensor_type);

-- ============================================================
-- ALERTS
-- ============================================================

CREATE TABLE IF NOT EXISTS alerts (
    id                  TEXT PRIMARY KEY,
    athlete_id          TEXT NOT NULL REFERENCES users(id),
    team_id             TEXT REFERENCES teams(id),
    session_id          TEXT REFERENCES training_sessions(id),
    timestamp           TEXT NOT NULL DEFAULT (datetime('now')),
    type                TEXT NOT NULL,
    severity            TEXT NOT NULL CHECK(severity IN ('CRITICAL','WARNING','ATTENTION')),
    parameter           TEXT NOT NULL,
    value               REAL NOT NULL,
    threshold           REAL NOT NULL,
    acknowledged        INTEGER NOT NULL DEFAULT 0,
    acknowledged_by     TEXT REFERENCES users(id),
    acknowledged_at     TEXT
);

CREATE INDEX IF NOT EXISTS idx_alerts_team_ack
    ON alerts(team_id, acknowledged, timestamp);

-- ============================================================
-- MEDICAL RECORDS
-- ============================================================

CREATE TABLE IF NOT EXISTS medical_records (
    id          TEXT PRIMARY KEY,
    athlete_id  TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_id   TEXT NOT NULL REFERENCES users(id),
    field_type  TEXT NOT NULL
                CHECK(field_type IN ('pathology','allergy','medication',
                                     'emergency_contact','blood_type')),
    field_value TEXT NOT NULL,
    severity    TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS soap_notes (
    id              TEXT PRIMARY KEY,
    athlete_id      TEXT NOT NULL REFERENCES users(id),
    doctor_id       TEXT NOT NULL REFERENCES users(id),
    session_id      TEXT REFERENCES training_sessions(id),
    date            TEXT NOT NULL DEFAULT (date('now')),
    subjective      TEXT,
    objective       TEXT,
    assessment      TEXT,
    plan            TEXT,
    attachment_path TEXT,
    is_private      INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS medical_restrictions (
    id          TEXT PRIMARY KEY,
    athlete_id  TEXT NOT NULL REFERENCES users(id),
    doctor_id   TEXT NOT NULL REFERENCES users(id),
    type        TEXT NOT NULL CHECK(type IN ('FULL','PARTIAL','HR_LIMITED')),
    hr_limit    INTEGER,
    description TEXT NOT NULL,
    start_date  TEXT NOT NULL DEFAULT (date('now')),
    return_date TEXT,
    status      TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active','resolved'))
);

-- ============================================================
-- CHAT
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id          TEXT PRIMARY KEY,
    team_id     TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    sender_id   TEXT NOT NULL REFERENCES users(id),
    message     TEXT NOT NULL,
    timestamp   TEXT NOT NULL DEFAULT (datetime('now')),
    is_read     INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chat_team_time
    ON chat_messages(team_id, timestamp);

-- ============================================================
-- PERSONAL RECORDS
-- ============================================================

CREATE TABLE IF NOT EXISTS personal_records (
    athlete_id  TEXT NOT NULL REFERENCES users(id),
    record_type TEXT NOT NULL,
    value       REAL NOT NULL,
    unit        TEXT,
    achieved_at TEXT NOT NULL DEFAULT (datetime('now')),
    session_id  TEXT REFERENCES training_sessions(id),
    PRIMARY KEY (athlete_id, record_type)
);

-- ============================================================
-- APP SETTINGS (key-value store)
-- ============================================================

CREATE TABLE IF NOT EXISTS app_settings (
    key     TEXT PRIMARY KEY,
    value   TEXT NOT NULL
);

INSERT OR IGNORE INTO app_settings VALUES ('schema_version', '1.0.0');
INSERT OR IGNORE INTO app_settings VALUES ('app_name', 'MedSport Monitor');
