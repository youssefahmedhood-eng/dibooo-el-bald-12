# MedSport Monitor — Technical Architecture

## 1. Module Overview

```
com.medsport (JPMS module)
├── auth/               Authentication & session lifecycle
├── controllers/        FXML-bound UI controllers (49 Java files)
│   ├── athlete/        5 controllers
│   ├── coach/          6 controllers
│   ├── medical/        5 controllers
│   └── shared/         4 controllers (MainLayout, TeamWizard, JoinTeam, NotificationController)
├── models/             Domain models (POJOs)
├── services/           Backend services (singletons)
└── views/components/   Reusable JavaFX components
```

### module-info.java (JPMS declarations)
```java
module com.medsport {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.databind;
    requires org.xerial.sqlitejdbc;
    requires org.eclipse.paho.client.mqttv3;
    requires java.net.http;
    requires java.desktop;

    opens com.medsport.auth        to javafx.fxml;
    opens com.medsport.controllers.athlete  to javafx.fxml;
    opens com.medsport.controllers.coach    to javafx.fxml;
    opens com.medsport.controllers.medical  to javafx.fxml;
    opens com.medsport.controllers.shared   to javafx.fxml;
}
```

---

## 2. Service Layer (Singletons)

### AppContext
Central registry — holds all service singletons. Controllers obtain services via:
```java
AppContext ctx = AppContext.getInstance();
DatabaseService      db        = ctx.getDatabase();
MqttService          mqtt      = ctx.getMqtt();
SessionService       sessions  = ctx.getSessions();
TeamService          teams     = ctx.getTeams();
AlertService         alerts    = ctx.getAlerts();
ChatService          chat      = ctx.getChat();
AnalyticsRestClient  analytics = ctx.getAnalytics();
```

### DatabaseService
- SQLite JDBC via `org.xerial:sqlite-jdbc`
- WAL journal mode for concurrent reads
- All queries run synchronously; callers decide threading
- Key methods:
  ```
  findUserByEmail(email)             → Optional<User>
  findAthleteProfile(userId)         → Optional<Athlete>      ← always .orElse(null)
  findAthletesByTeam(teamId)         → List<Athlete>
  findSessionsByAthlete(userId)      → List<Session>
  findActiveAlerts(teamId)           → List<Alert>
  acknowledgeAlert(alertId, userId)  → void
  findSoapNotesByAthlete(userId)     → List<SoapNote>
  saveSoapNote(note)                 → void
  findAllRestrictionsForTeam(teamId) → List<MedicalRestriction>
  saveRestriction(r)                 → void
  ```

### MqttService
- Eclipse Paho 1.2.5 (synchronous client with background reconnect thread)
- Subscribes to `medsport/{teamId}/#` on connect
- Dispatches parsed `SensorReading` objects to registered listeners
- Public API:
  ```java
  connect(teamId)
  disconnect()
  addReadingListener(Consumer<SensorReading>)
  addMqttLogListener(Consumer<String>)
  publish(topic, payload)
  isConnected()
  ```

### AnalyticsRestClient
- `java.net.http.HttpClient` (async, JDK 11+)
- Base URL: `http://localhost:8000/api`
- Jackson `ObjectMapper` for JSON serialization
- All methods return `CompletableFuture<JsonNode>` (or `CompletableFuture<byte[]>` for PDF)
- Never throws — catches all exceptions and returns `null`/empty on offline

### SessionService / TeamService / AlertService / ChatService
- Thin wrappers over `DatabaseService` with domain logic
- All run on caller thread; use `Platform.runLater()` in controllers for UI updates

---

## 3. Authentication

### Flow
```
Login FXML → LoginController → DatabaseService.findUserByEmail()
         → PasswordUtils.verify(input, hash)       ← PBKDF2-SHA256
         → SessionManager.login(user, team)
         → AppContext.connectMqtt()
         → navigate to MainLayout.fxml
```

### PasswordUtils
- Algorithm: PBKDF2WithHmacSHA256
- Iterations: 65 536
- Key length: 256 bits
- Salt: 16 bytes SecureRandom (stored as hex prefix in hash string)
- BCrypt was removed due to JPMS compatibility issues with third-party BCrypt libs

### SessionManager (singleton)
```java
getCurrentUser()    → User
getTeamId()         → String
getTeamName()       → String
getCurrentTeam()    → Team
getRole()           → User.Role   (ATHLETE | COACH | MEDICAL)
hasTeam()           → boolean
logout()
```

---

## 4. FXML Controller Map

| FXML | Controller | Role | Key fx:ids |
|------|-----------|------|-----------|
| `Login.fxml` | `auth.LoginController` | All | emailField, passwordField, bgContainer, loginButton |
| `Register.fxml` | `auth.RegisterController` | All | — |
| `MainLayout.fxml` | `shared.MainLayoutController` | All | sidebar, contentArea, chatBadge, nav* buttons |
| `athlete/Dashboard.fxml` | `athlete.DashboardController` | Athlete | hrValueLabel, hrCanvas, spo2Label, zoneBar*, speedLabel, distanceLabel |
| `athlete/Sessions.fxml` | `athlete.SessionsController` | Athlete | sessionsTable, detailPane, zoneBreakdownBox |
| `athlete/Performance.fxml` | `athlete.PerformanceController` | Athlete | radarCanvas (380×340), avgHrLabel, maxHrLabel, acrLabel, apiStatusLabel |
| `athlete/Team.fxml` | `athlete.TeamController` | Athlete | chatListView, chatInput, memberList, unreadBadge |
| `athlete/Profile.fxml` | `athlete.ProfileController` | Athlete | nameField, sportField, heightField, weightField, passwordSection |
| `coach/Dashboard.fxml` | `coach.DashboardController` | Coach | athleteGrid (FlowPane), alertFeed (VBox) |
| `coach/Session.fxml` | `coach.SessionController` | Coach | exerciseTypeCombo, zoneMinSlider, zoneMaxSlider, athleteRows (VBox) |
| `coach/Comparison.fxml` | `coach.ComparisonController` | Coach | athlete1Combo, athlete2Combo, radarCanvas (400×380) |
| `coach/TeamManagement.fxml` | `coach.TeamManagementController` | Coach | athleteTable, hrThresholdSlider, spo2ThresholdSlider |
| `coach/Simulator.fxml` | `coach.SimulatorController` | Coach | mqttStatusLabel, athleteSliders (VBox), mqttLogArea |
| `coach/Performance.fxml` | `coach.PerformanceController` | Coach | athleteCombo, radarCanvas (380×340), acrLabel, apiStatusLabel |
| `medical/Dashboard.fxml` | `medical.DashboardController` | Medical | criticalAlertFeed, athleteStatusGrid (FlowPane), restrictionFeed |
| `medical/History.fxml` | `medical.HistoryController` | Medical | notesList (ListView), subjArea, objArea, assessArea, planArea |
| `medical/Restrictions.fxml` | `medical.RestrictionsController` | Medical | athleteCombo, typeCombo, hrLimitField, returnDatePicker, restrictionList |
| `medical/Risk.fxml` | `medical.RiskController` | Medical | riskRadarCanvas, teamRiskLabel, riskCards (VBox) |
| `medical/Reports.fxml` | `medical.ReportsController` | Medical | athleteCombo, reportTypeCombo, generateProgress, apiStatusLabel |

---

## 5. MQTT Data Flow

```
Python Simulator
  └── publishes JSON payload:
      { "value": 72.5, "timestamp": "2026-01-15T10:30:00" }
      to topic: medsport/{team_id}/{athlete_id}/{sensor_type}

Mosquitto Broker :1883

MqttService.onMessageArrived()
  └── parses topic → extracts teamId, athleteId, sensorType
  └── parses payload → SensorReading(athleteId, sensorType, value, timestamp)
  └── notifies all registered Consumer<SensorReading> listeners
  └── MqttService.saveReadingToDb(reading)   ← async daemon thread

Dashboard controllers (athlete, coach, medical)
  └── Platform.runLater(() -> processReading(reading))
  └── updates UI labels / canvas
```

### Topic Format
```
medsport / {team_id} / {athlete_id} / {sensor_type}

sensor_type values:
  heart_rate | spo2 | gps_lat | gps_lon | gps_speed | gps_alt
  accel_x | accel_y | accel_z | cadence
```

### SensorType enum
```java
enum SensorType {
    HR, SPO2,
    GPS_LAT, GPS_LON, GPS_SPEED, GPS_ALT,
    ACCEL_X, ACCEL_Y, ACCEL_Z,
    CADENCE;

    static SensorType fromString(String mqttType) { /* maps snake_case → enum */ }
}
```

---

## 6. FastAPI Analytics Service

### Startup
```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

### Module layout (`python-analytics/`)
```
main.py                  FastAPI app + route registration
analysis/
  heart_zones.py         Karvonen zones + HRV (rMSSD, SDNN)
  fatigue_index.py       ACR: acute_load/chronic_load (7d / 28d rolling)
  radar_metrics.py       6-axis polygon: Endurance/Speed/Recovery/Consistency/Load/Intensity
  cardiovascular_risk.py Risk score: HR ratio, SpO₂ deficit, load spike, ECG proxy
  hrv_analysis.py        HRV time-domain (SDNN, rMSSD, pNN50)
  training_load.py       Team monotony, strain, period load
  pdf_generator.py       ReportLab PDF with SOAP notes, charts, risk matrix
```

### Endpoint Reference

**Health**
```
GET /api/health
→ { "status": "ok", "version": "1.0.0" }
```

**Heart Zones**
```
POST /api/analysis/heart-zones
Body: { "athlete_id": str, "hr_series": [float], "hr_max": float, "resting_hr": float }
→ { "zones": { "z1": float, "z2": float, ... "z5": float },
    "hrv": { "rmssd": float, "sdnn": float } }
```

**Fatigue Index**
```
POST /api/analysis/fatigue-index
Body: { "athlete_id": str, "sessions": [{ "date": str, "load_au": float }] }
→ { "acr": float, "classification": "Optimal|High|Low",
    "acute_load": float, "chronic_load": float, "training_load": float }
```

**Radar Metrics**
```
POST /api/analysis/radar-metrics
Body: { "athlete_id": str, "sessions": [{ "duration_min", "type", "hr_max", ... }] }
→ { "metrics": { "endurance": float, "speed": float, "recovery": float,
                  "consistency": float, "load": float, "intensity": float } }
```

**Cardiovascular Risk**
```
POST /api/analysis/cardiovascular-risk
Body: { "athlete_id": str, "biometrics": { "age", "hr_max", "resting_hr", ... },
        "sessions": [...] }
→ { "risk_score": float, "risk_level": "LOW|MODERATE|HIGH",
    "factors": { "hr_ratio": float, "spo2_deficit": float, ... } }
```

**Team Training Load**
```
POST /api/analysis/training-load
Body: { "team_id": str, "athletes": [{ "athlete_id", "sessions": [...] }], "period_days": int }
→ { "team_avg_load": float, "overloaded": [str], "underloaded": [str],
    "monotony": float, "strain": float }
```

**Session Summary**
```
POST /api/analysis/session-summary
Body: { "session_id": str, "sensor_data": { "hr_series": [...], "speed_series": [...], ... } }
→ { "avg_hr": float, "max_hr": float, "avg_speed": float,
    "zone_distribution": { "z1": float, ... "z5": float } }
```

**PDF Report**
```
POST /api/reports/generate-athlete-pdf
Body: {
  "athlete_id": str, "athlete_info": {}, "team_info": {}, "doctor_info": {},
  "sessions": [...], "soap_notes": [...], "restriction": {}, "risk_data": {}, "period": str
}
→ { "pdf_base64": str, "filename": str }
```
Java decodes `pdf_base64` → `byte[]` via `Base64.getDecoder().decode()`

---

## 7. Database Schema (SQLite)

```sql
-- Core identity
CREATE TABLE users (
    id            TEXT PRIMARY KEY,
    email         TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name          TEXT,
    role          TEXT NOT NULL,          -- ATHLETE | COACH | MEDICAL
    created_at    TEXT,
    last_login    TEXT
);

-- Athlete-specific biometrics
CREATE TABLE athlete_profiles (
    user_id     TEXT PRIMARY KEY REFERENCES users(id),
    sport       TEXT,
    date_of_birth TEXT,
    height_cm   REAL,
    weight_kg   REAL,
    hr_max      INTEGER,
    resting_hr  INTEGER,
    blood_type  TEXT
);

-- Team management
CREATE TABLE teams (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    sport           TEXT,
    join_code       TEXT UNIQUE,
    coach_id        TEXT REFERENCES users(id),
    hr_threshold    REAL DEFAULT 0.85,
    spo2_threshold  REAL DEFAULT 94.0,
    load_threshold  REAL DEFAULT 800.0,
    created_at      TEXT
);

CREATE TABLE team_members (
    team_id   TEXT REFERENCES teams(id),
    user_id   TEXT REFERENCES users(id),
    joined_at TEXT,
    PRIMARY KEY (team_id, user_id)
);

-- Session tracking
CREATE TABLE sessions (
    id            TEXT PRIMARY KEY,
    athlete_id    TEXT REFERENCES users(id),
    team_id       TEXT REFERENCES teams(id),
    exercise_type TEXT,
    started_at    TEXT,
    ended_at      TEXT,
    active        INTEGER DEFAULT 0,
    notes         TEXT
);

-- Raw IoMT sensor data
CREATE TABLE sensor_readings (
    id          TEXT PRIMARY KEY,
    athlete_id  TEXT REFERENCES users(id),
    session_id  TEXT REFERENCES sessions(id),
    sensor_type TEXT,
    value       REAL,
    recorded_at TEXT
);

-- Physiological alerts
CREATE TABLE alerts (
    id             TEXT PRIMARY KEY,
    athlete_id     TEXT REFERENCES users(id),
    team_id        TEXT REFERENCES teams(id),
    parameter      TEXT,
    value          REAL,
    threshold      REAL,
    severity       TEXT,    -- LOW | MEDIUM | HIGH | CRITICAL
    message        TEXT,
    acknowledged   INTEGER DEFAULT 0,
    acknowledged_by TEXT,
    created_at     TEXT
);

-- Clinical records
CREATE TABLE soap_notes (
    id          TEXT PRIMARY KEY,
    athlete_id  TEXT REFERENCES users(id),
    doctor_id   TEXT REFERENCES users(id),
    date        TEXT,
    subjective  TEXT,
    objective   TEXT,
    assessment  TEXT,
    plan        TEXT,
    is_private  INTEGER DEFAULT 0,
    created_at  TEXT
);

CREATE TABLE medical_restrictions (
    id           TEXT PRIMARY KEY,
    athlete_id   TEXT REFERENCES users(id),
    doctor_id    TEXT REFERENCES users(id),
    type         TEXT,          -- FULL | PARTIAL | HR_LIMITED
    description  TEXT,
    hr_limit     INTEGER,
    return_date  TEXT,
    status       TEXT,          -- ACTIVE | RESOLVED
    created_at   TEXT
);

CREATE TABLE medical_records (
    id             TEXT PRIMARY KEY,
    athlete_id     TEXT REFERENCES users(id),
    blood_type     TEXT,
    pathologies    TEXT,
    allergies      TEXT,
    medications    TEXT,
    last_updated   TEXT
);

-- Team communication
CREATE TABLE chat_messages (
    id         TEXT PRIMARY KEY,
    team_id    TEXT REFERENCES teams(id),
    sender_id  TEXT REFERENCES users(id),
    content    TEXT,
    sent_at    TEXT,
    read_by    TEXT    -- JSON array of user_ids
);
```

---

## 8. UI Component Architecture

### Visual Polish Components (`views/components/`)

**RippleEffect**
- Attaches `onMousePressed` handler to any `Button`
- Creates `Circle` at click coordinates, animates radius 0→maxR and opacity 0.35→0 over 400ms
- No external dependencies — pure JavaFX Timeline + Animation

**AnimatedBackground**
- Returns a `Pane` containing a `Canvas`
- `AnimationTimer` (60fps) draws:
  - Dark solid background (`#0d1117`)
  - 40px grid lines (subtle, low alpha)
  - 38 small particles (2–5px radius, theme color, alpha 0.3–0.6)
  - 4 large orbs (60–120px radius, theme color, alpha 0.05–0.10)
- Particle physics: constant drift with edge-wrapping
- Timer stopped automatically via scene detach listener

**ViewTransition**
- `slideUp(node)` — translateY 18→0 + fade, 280ms EASE_OUT
- `slideRight(node)` — translateX -14→0 + fade, 260ms EASE_OUT
- `popIn(node)` — scale 0.92→1.0 + fade, 220ms EASE_OUT
- `staggerSlideUp(List<Node>)` — 60ms delay per node
- `countUp(Label, start, end, suffix, decimals)` — 20 frames, 600ms
- `pulse(node, scale)` — bounce scale animation, 2 cycles
- `shake(node)` — horizontal ±6px, 6 cycles

### CSS Theme System

| File | Role accent | Key variables |
|------|------------|---------------|
| `base.css` | n/a | Shared dark-mode foundation, `.glass-card`, `.btn-primary`, `.btn-ghost`, list/scroll dark theming |
| `theme-athlete.css` | `#0A84FF` (blue) | `--theme-accent`, login-bg gradient, btn glow |
| `theme-coach.css` | `#00C853` (green) | Same variables, different colors |
| `theme-medical.css` | `#D32F2F` (red) | Same + `.soap-section-label` |

Themes are loaded at login via `scene.getStylesheets().add(themeCss)` after `base.css`.

---

## 9. Physiological Calculation Reference

### HRmax (Tanaka formula)
```
HRmax = 208 - 0.7 × age
```

### Heart Rate Reserve (HRR)
```
HRR = HRmax - RestingHR
```

### Karvonen Heart Rate Zones
```
Zone N lower bound = RestingHR + (HRR × zone_pct_low)
Zone N upper bound = RestingHR + (HRR × zone_pct_high)

Zone 1:  0.00 – 0.60 × HRR   (Active recovery)
Zone 2:  0.60 – 0.70 × HRR   (Aerobic base)
Zone 3:  0.70 – 0.80 × HRR   (Aerobic endurance)
Zone 4:  0.80 – 0.90 × HRR   (Lactate threshold)
Zone 5:  0.90 – 1.00 × HRR   (VO₂max)
```

### Training Monotony & Strain (Foster)
```
Daily load = duration_min × RPE
Weekly average load = mean(daily loads)
Standard deviation  = std(daily loads)
Monotony            = average / standard_deviation
Strain              = weekly_sum × monotony
```

### Acute:Chronic Workload Ratio (ACWR)
```
Acute load  = sum of loads over last 7 days
Chronic load = rolling mean of 4-week loads
ACWR        = Acute / Chronic

Sweet spot: 0.8 – 1.3
Danger zone: > 1.5 (injury risk increases)
```

---

## 10. Simulator Architecture

### Python Simulator (`python-simulator/`)

Simulates 3 athletes simultaneously, each with independent physiological state machines.

**Sensor models:**
- **Heart rate**: Ornstein-Uhlenbeck mean-reverting process, target HR determined by intensity slider (coach controllable via MQTT control topic)
- **SpO₂**: Gaussian noise around 97–99%, drops under fatigue > 0.7
- **GPS**: Geodetic random-walk on a 400m oval track; speed correlates with intensity
- **Accelerometer**: Sinusoidal stride model at cadence frequency + Gaussian noise
- **Cadence**: Correlated with speed (≈steps_per_minute derived from pace)

**Publish rate:** 1Hz per sensor per athlete = ~10 messages/sec for 3 athletes

**MQTT topics published:**
```
medsport/team-demo/athlete-{1,2,3}/heart_rate
medsport/team-demo/athlete-{1,2,3}/spo2
medsport/team-demo/athlete-{1,2,3}/gps_lat
medsport/team-demo/athlete-{1,2,3}/gps_lon
medsport/team-demo/athlete-{1,2,3}/gps_speed
medsport/team-demo/athlete-{1,2,3}/accel_{x,y,z}
medsport/team-demo/athlete-{1,2,3}/cadence
```