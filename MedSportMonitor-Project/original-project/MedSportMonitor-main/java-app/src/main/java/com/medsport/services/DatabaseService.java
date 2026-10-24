package com.medsport.services;

import com.medsport.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Singleton service for all SQLite database operations.
 * Database location: ~/Downloads/MedSport/database/medsport.db
 */
public class DatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_DIR  = System.getProperty("user.home") + "/Downloads/MedSport/database";
    private static final String DB_PATH = DB_DIR + "/medsport.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static volatile DatabaseService instance;
    private Connection connection;

    private DatabaseService() {}

    public static DatabaseService getInstance() {
        if (instance == null) {
            synchronized (DatabaseService.class) {
                if (instance == null) instance = new DatabaseService();
            }
        }
        return instance;
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    public void initialize() throws Exception {
        Files.createDirectories(Paths.get(DB_DIR));
        connection = DriverManager.getConnection(JDBC_URL);

        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }

        runScript("schema.sql");
        runScript("seed_data.sql");
        LOG.info("Database ready at {}", DB_PATH);
    }

    private void runScript(String scriptName) {
        String resourcePath = "/database/" + scriptName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Try file-system fallback
                Path p = Paths.get(DB_DIR, scriptName);
                if (!Files.exists(p)) {
                    LOG.warn("Script not found: {}", scriptName);
                    return;
                }
                executeSql(Files.readString(p));
                return;
            }
            executeSql(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.warn("Could not run script {}: {}", scriptName, e.getMessage());
        }
    }

    private void executeSql(String sql) throws SQLException {
        for (String stmt : sql.split(";")) {
            // Strip leading comment lines so chunks like "-- header\nCREATE TABLE..." are not skipped
            StringBuilder sb = new StringBuilder();
            for (String line : stmt.split("\n")) {
                if (!line.strip().startsWith("--")) sb.append(line).append("\n");
            }
            String trimmed = sb.toString().strip();
            if (!trimmed.isEmpty()) {
                try (Statement st = connection.createStatement()) {
                    st.execute(trimmed);
                }
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOG.error("Error closing database", e);
        }
    }

    public Connection getConnection() { return connection; }

    // ============================================================
    // USER OPERATIONS
    // ============================================================

    public Optional<User> findUserByEmail(String email) {
        String sql = "SELECT id, email, password_hash, role, name, created_at, last_login FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) {
            LOG.error("findUserByEmail error", e);
        }
        return Optional.empty();
    }

    public Optional<User> findUserById(String id) {
        String sql = "SELECT id, email, password_hash, role, name, created_at, last_login FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) {
            LOG.error("findUserById error", e);
        }
        return Optional.empty();
    }

    public void saveUser(User user) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO users (id, email, password_hash, role, name, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getName());
            ps.setString(6, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        }
    }

    public void updateLastLogin(String userId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET last_login = ? WHERE id = ?")) {
            ps.setString(1, LocalDateTime.now().format(DT_FMT));
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("updateLastLogin error", e);
        }
    }

    // ============================================================
    // ATHLETE PROFILE
    // ============================================================

    public void saveAthleteProfile(Athlete a) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO athlete_profiles
            (user_id, birth_date, sex, sport, height_cm, weight_kg, hr_max, resting_hr, photo_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getUserId());
            ps.setString(2, a.getBirthDate() != null ? a.getBirthDate().format(D_FMT) : null);
            ps.setString(3, a.getSex() != null ? a.getSex().name() : null);
            ps.setString(4, a.getSport());
            ps.setObject(5, a.getHeightCm());
            ps.setObject(6, a.getWeightKg());
            ps.setObject(7, a.getHrMax());
            ps.setObject(8, a.getRestingHr());
            ps.setString(9, a.getPhotoPath());
            ps.executeUpdate();
        }
    }

    public Optional<Athlete> findAthleteProfile(String userId) {
        String sql = """
            SELECT ap.*, u.name, u.email FROM athlete_profiles ap
            JOIN users u ON u.id = ap.user_id
            WHERE ap.user_id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapAthlete(rs));
            }
        } catch (SQLException e) {
            LOG.error("findAthleteProfile error", e);
        }
        return Optional.empty();
    }

    public List<Athlete> findAthletesByTeam(String teamId) {
        String sql = """
            SELECT ap.*, u.name, u.email FROM athlete_profiles ap
            JOIN users u ON u.id = ap.user_id
            JOIN team_members tm ON tm.user_id = ap.user_id
            WHERE tm.team_id = ? AND tm.role_in_team = 'ATHLETE' AND tm.is_active = 1
            ORDER BY u.name
            """;
        List<Athlete> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAthlete(rs));
            }
        } catch (SQLException e) {
            LOG.error("findAthletesByTeam error", e);
        }
        return list;
    }

    // ============================================================
    // TEAM OPERATIONS
    // ============================================================

    public void saveTeam(Team t) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO teams
            (id, name, sport, category, description, logo_path, primary_color, secondary_color,
             coach_id, join_code, hr_max_reference, spo2_alert_threshold, hr_alert_threshold,
             weekly_load_threshold, emergency_protocol, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getName());
            ps.setString(3, t.getSport());
            ps.setString(4, t.getCategory() != null ? t.getCategory().name() : null);
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getLogoPath());
            ps.setString(7, t.getPrimaryColor());
            ps.setString(8, t.getSecondaryColor());
            ps.setString(9, t.getCoachId());
            ps.setString(10, t.getJoinCode());
            ps.setInt(11, t.getHrMaxReference());
            ps.setDouble(12, t.getSpo2AlertThreshold());
            ps.setDouble(13, t.getHrAlertThreshold());
            ps.setDouble(14, t.getWeeklyLoadThreshold());
            ps.setString(15, t.getEmergencyProtocol());
            ps.setString(16, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        }
    }

    public Optional<Team> findTeamByJoinCode(String code) {
        String sql = "SELECT * FROM teams WHERE join_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTeam(rs));
            }
        } catch (SQLException e) {
            LOG.error("findTeamByJoinCode error", e);
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamByCoach(String coachId) {
        String sql = "SELECT * FROM teams WHERE coach_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTeam(rs));
            }
        } catch (SQLException e) {
            LOG.error("findTeamByCoach error", e);
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamByMember(String userId) {
        String sql = """
            SELECT t.* FROM teams t
            JOIN team_members tm ON tm.team_id = t.id
            WHERE tm.user_id = ? AND tm.is_active = 1
            ORDER BY tm.joined_at DESC LIMIT 1
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTeam(rs));
            }
        } catch (SQLException e) {
            LOG.error("findTeamByMember error", e);
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamById(String teamId) {
        String sql = "SELECT * FROM teams WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTeam(rs));
            }
        } catch (SQLException e) {
            LOG.error("findTeamById error", e);
        }
        return Optional.empty();
    }

    public void addTeamMember(String teamId, String userId, String role) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO team_members (team_id, user_id, role_in_team, joined_at, is_active)
            VALUES (?, ?, ?, ?, 1)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, userId);
            ps.setString(3, role);
            ps.setString(4, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        }
    }

    public void removeTeamMember(String teamId, String userId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE team_members SET is_active = 0 WHERE team_id = ? AND user_id = ?")) {
            ps.setString(1, teamId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    // ============================================================
    // SENSOR READINGS
    // ============================================================

    public void saveSensorReading(SensorReading r) throws SQLException {
        String sql = """
            INSERT INTO sensor_readings (athlete_id, session_id, timestamp, sensor_type, value, unit, quality)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getAthleteId());
            ps.setString(2, r.getSessionId());
            ps.setString(3, r.getTimestamp().toString());
            ps.setString(4, r.getSensorType().toDbString());
            ps.setDouble(5, r.getValue());
            ps.setString(6, r.getUnit());
            ps.setDouble(7, r.getQuality());
            ps.executeUpdate();
        }
    }

    public List<SensorReading> findReadingsByAthleteAndSession(String athleteId, String sessionId,
                                                                SensorReading.SensorType type) {
        String sql = """
            SELECT * FROM sensor_readings
            WHERE athlete_id = ? AND session_id = ? AND sensor_type = ?
            ORDER BY timestamp
            """;
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            ps.setString(2, sessionId);
            ps.setString(3, type.toDbString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSensorReading(rs));
            }
        } catch (SQLException e) {
            LOG.error("findReadings error", e);
        }
        return list;
    }

    public List<SensorReading> findRecentReadings(String athleteId, SensorReading.SensorType type, int limitSeconds) {
        String cutoff = Instant.now().minusSeconds(limitSeconds).toString();
        String sql = """
            SELECT * FROM sensor_readings
            WHERE athlete_id = ? AND sensor_type = ? AND timestamp > ?
            ORDER BY timestamp DESC LIMIT 500
            """;
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            ps.setString(2, type.toDbString());
            ps.setString(3, cutoff);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSensorReading(rs));
            }
        } catch (SQLException e) {
            LOG.error("findRecentReadings error", e);
        }
        return list;
    }

    // ============================================================
    // ALERTS
    // ============================================================

    public void saveAlert(Alert a) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO alerts
            (id, athlete_id, team_id, session_id, timestamp, type, severity, parameter, value, threshold, acknowledged)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getAthleteId());
            ps.setString(3, a.getTeamId());
            ps.setString(4, a.getSessionId());
            ps.setString(5, LocalDateTime.now().format(DT_FMT));
            ps.setString(6, a.getType());
            ps.setString(7, a.getSeverity().name());
            ps.setString(8, a.getParameter());
            ps.setDouble(9, a.getValue());
            ps.setDouble(10, a.getThreshold());
            ps.executeUpdate();
        }
    }

    public List<Alert> findActiveAlerts(String teamId) {
        String sql = """
            SELECT a.*, u.name as athlete_name FROM alerts a
            JOIN users u ON u.id = a.athlete_id
            WHERE a.team_id = ? AND a.acknowledged = 0
            ORDER BY a.severity DESC, a.timestamp DESC
            """;
        List<Alert> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAlert(rs));
            }
        } catch (SQLException e) {
            LOG.error("findActiveAlerts error", e);
        }
        return list;
    }

    public void acknowledgeAlert(String alertId, String doctorId) throws SQLException {
        String sql = """
            UPDATE alerts SET acknowledged = 1, acknowledged_by = ?, acknowledged_at = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, doctorId);
            ps.setString(2, LocalDateTime.now().format(DT_FMT));
            ps.setString(3, alertId);
            ps.executeUpdate();
        }
    }

    // ============================================================
    // TRAINING SESSIONS
    // ============================================================

    public void saveSession(Session s) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO training_sessions
            (id, team_id, coach_id, start_time, end_time, exercise_type,
             target_zone_min, target_zone_max, notes, status)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getTeamId());
            ps.setString(3, s.getCoachId());
            ps.setString(4, s.getStartTime() != null ? s.getStartTime().format(DT_FMT) : null);
            ps.setString(5, s.getEndTime() != null ? s.getEndTime().format(DT_FMT) : null);
            ps.setString(6, s.getExerciseType());
            ps.setInt(7, s.getTargetZoneMin());
            ps.setInt(8, s.getTargetZoneMax());
            ps.setString(9, s.getNotes());
            ps.setString(10, s.getStatus().name());
            ps.executeUpdate();
        }
    }

    public List<Session> findSessionsByTeam(String teamId) {
        String sql = "SELECT * FROM training_sessions WHERE team_id = ? ORDER BY start_time DESC";
        List<Session> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            LOG.error("findSessionsByTeam error", e);
        }
        return list;
    }

    public List<Session> findSessionsByAthlete(String athleteId) {
        String sql = """
            SELECT ts.* FROM training_sessions ts
            JOIN session_participants sp ON sp.session_id = ts.id
            WHERE sp.athlete_id = ? AND ts.status = 'COMPLETED'
            ORDER BY ts.start_time DESC
            """;
        List<Session> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            LOG.error("findSessionsByAthlete error", e);
        }
        return list;
    }

    public Optional<Session> findActiveSession(String teamId) {
        String sql = "SELECT * FROM training_sessions WHERE team_id = ? AND status = 'ACTIVE' LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapSession(rs));
            }
        } catch (SQLException e) {
            LOG.error("findActiveSession error", e);
        }
        return Optional.empty();
    }

    // ============================================================
    // SOAP NOTES
    // ============================================================

    public void saveSoapNote(SoapNote n) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO soap_notes
            (id, athlete_id, doctor_id, session_id, date, subjective, objective,
             assessment, plan, attachment_path, is_private, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getAthleteId());
            ps.setString(3, n.getDoctorId());
            ps.setString(4, n.getSessionId());
            ps.setString(5, n.getDate() != null ? n.getDate().format(D_FMT) : LocalDate.now().format(D_FMT));
            ps.setString(6, n.getSubjective());
            ps.setString(7, n.getObjective());
            ps.setString(8, n.getAssessment());
            ps.setString(9, n.getPlan());
            ps.setString(10, n.getAttachmentPath());
            ps.setInt(11, n.isPrivate() ? 1 : 0);
            ps.setString(12, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        }
    }

    public List<SoapNote> findSoapNotesByAthlete(String athleteId) {
        String sql = """
            SELECT sn.*, u.name as doctor_name FROM soap_notes sn
            JOIN users u ON u.id = sn.doctor_id
            WHERE sn.athlete_id = ? ORDER BY sn.date DESC
            """;
        List<SoapNote> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSoapNote(rs));
            }
        } catch (SQLException e) {
            LOG.error("findSoapNotesByAthlete error", e);
        }
        return list;
    }

    // ============================================================
    // MEDICAL RESTRICTIONS
    // ============================================================

    public void saveRestriction(MedicalRestriction r) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO medical_restrictions
            (id, athlete_id, doctor_id, type, hr_limit, description, start_date, return_date, status)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getAthleteId());
            ps.setString(3, r.getDoctorId());
            ps.setString(4, r.getType().name());
            ps.setObject(5, r.getHrLimit());
            ps.setString(6, r.getDescription());
            ps.setString(7, r.getStartDate() != null ? r.getStartDate().format(D_FMT) : null);
            ps.setString(8, r.getReturnDate() != null ? r.getReturnDate().format(D_FMT) : null);
            ps.setString(9, r.getStatus().name().toLowerCase());
            ps.executeUpdate();
        }
    }

    public Optional<MedicalRestriction> findActiveRestriction(String athleteId) {
        String sql = """
            SELECT mr.*, u.name as athlete_name FROM medical_restrictions mr
            JOIN users u ON u.id = mr.athlete_id
            WHERE mr.athlete_id = ? AND mr.status = 'active'
            ORDER BY mr.start_date DESC LIMIT 1
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRestriction(rs));
            }
        } catch (SQLException e) {
            LOG.error("findActiveRestriction error", e);
        }
        return Optional.empty();
    }

    public List<MedicalRestriction> findAllRestrictionsForTeam(String teamId) {
        String sql = """
            SELECT mr.*, u.name as athlete_name FROM medical_restrictions mr
            JOIN users u ON u.id = mr.athlete_id
            JOIN team_members tm ON tm.user_id = mr.athlete_id
            WHERE tm.team_id = ? AND tm.is_active = 1
            ORDER BY mr.status, mr.start_date DESC
            """;
        List<MedicalRestriction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRestriction(rs));
            }
        } catch (SQLException e) {
            LOG.error("findAllRestrictionsForTeam error", e);
        }
        return list;
    }

    // ============================================================
    // MEDICAL RECORDS
    // ============================================================

    public void saveMedicalRecord(MedicalRecord r) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO medical_records
            (id, athlete_id, doctor_id, field_type, field_value, severity, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getAthleteId());
            ps.setString(3, r.getDoctorId());
            ps.setString(4, r.getFieldType().name().toLowerCase());
            ps.setString(5, r.getFieldValue());
            ps.setString(6, r.getSeverity());
            String now = LocalDateTime.now().format(DT_FMT);
            ps.setString(7, r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : now);
            ps.setString(8, now);
            ps.executeUpdate();
        }
    }

    public List<MedicalRecord> findMedicalRecordsByAthlete(String athleteId) {
        String sql = "SELECT * FROM medical_records WHERE athlete_id = ? ORDER BY field_type";
        List<MedicalRecord> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMedicalRecord(rs));
            }
        } catch (SQLException e) {
            LOG.error("findMedicalRecordsByAthlete error", e);
        }
        return list;
    }

    // ============================================================
    // CHAT
    // ============================================================

    public void saveChatMessage(ChatMessage m) throws SQLException {
        String sql = "INSERT INTO chat_messages (id, team_id, sender_id, message, timestamp, is_read) VALUES (?,?,?,?,?,0)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getTeamId());
            ps.setString(3, m.getSenderId());
            ps.setString(4, m.getMessage());
            ps.setString(5, LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        }
    }

    public List<ChatMessage> findChatMessages(String teamId, int limit) {
        String sql = """
            SELECT cm.*, u.name as sender_name FROM chat_messages cm
            JOIN users u ON u.id = cm.sender_id
            WHERE cm.team_id = ?
            ORDER BY cm.timestamp DESC LIMIT ?
            """;
        List<ChatMessage> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapChatMessage(rs));
            }
        } catch (SQLException e) {
            LOG.error("findChatMessages error", e);
        }
        Collections.reverse(list);
        return list;
    }

    public int countUnreadMessages(String teamId, String userId) {
        String sql = "SELECT COUNT(*) FROM chat_messages WHERE team_id = ? AND sender_id != ? AND is_read = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.error("countUnreadMessages error", e);
        }
        return 0;
    }

    public void markMessagesRead(String teamId, String userId) {
        String sql = "UPDATE chat_messages SET is_read = 1 WHERE team_id = ? AND sender_id != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("markMessagesRead error", e);
        }
    }

    // ============================================================
    // PERSONAL RECORDS
    // ============================================================

    public void upsertPersonalRecord(String athleteId, String recordType, double value,
                                     String unit, String sessionId) {
        try {
            String sql = """
                INSERT INTO personal_records (athlete_id, record_type, value, unit, achieved_at, session_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(athlete_id, record_type) DO UPDATE SET
                  value = excluded.value,
                  achieved_at = excluded.achieved_at,
                  session_id = excluded.session_id
                WHERE excluded.value > personal_records.value
                """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, athleteId);
                ps.setString(2, recordType);
                ps.setDouble(3, value);
                ps.setString(4, unit);
                ps.setString(5, LocalDateTime.now().format(DT_FMT));
                ps.setString(6, sessionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.error("upsertPersonalRecord error", e);
        }
    }

    public Map<String, Double> findPersonalRecords(String athleteId) {
        String sql = "SELECT record_type, value FROM personal_records WHERE athlete_id = ?";
        Map<String, Double> records = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, athleteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) records.put(rs.getString("record_type"), rs.getDouble("value"));
            }
        } catch (SQLException e) {
            LOG.error("findPersonalRecords error", e);
        }
        return records;
    }

    // ============================================================
    // COACH / MEDICAL PROFILES
    // ============================================================

    public void saveCoachProfile(String userId, String sport, int years, String institution)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO coach_profiles VALUES (?,?,?,?)")) {
            ps.setString(1, userId);
            ps.setString(2, sport);
            ps.setInt(3, years);
            ps.setString(4, institution);
            ps.executeUpdate();
        }
    }

    public void saveMedicalProfile(String userId, String specialty, String license, String institution)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO medical_profiles VALUES (?,?,?,?)")) {
            ps.setString(1, userId);
            ps.setString(2, specialty);
            ps.setString(3, license);
            ps.setString(4, institution);
            ps.executeUpdate();
        }
    }

    public Map<String, String> findMedicalProfile(String userId) {
        String sql = "SELECT specialty, license_number, institution FROM medical_profiles WHERE user_id = ?";
        Map<String, String> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("specialty", rs.getString("specialty"));
                    map.put("license_number", rs.getString("license_number"));
                    map.put("institution", rs.getString("institution"));
                }
            }
        } catch (SQLException e) {
            LOG.error("findMedicalProfile error", e);
        }
        return map;
    }

    // ============================================================
    // USER UPDATES
    // ============================================================

    public void updateUserName(String userId, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { LOG.error("updateUserName error", e); }
    }

    public void updateUserPassword(String userId, String hash) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { LOG.error("updateUserPassword error", e); }
    }

    public void updateAthleteProfile(Athlete a) {
        String sql = """
            UPDATE athlete_profiles SET
              sport = ?, height_cm = ?, weight_kg = ?, hr_max = ?, resting_hr = ?
            WHERE user_id = ?""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getSport());
            if (a.getHeightCm() != null) ps.setDouble(2, a.getHeightCm()); else ps.setNull(2, java.sql.Types.REAL);
            if (a.getWeightKg() != null) ps.setDouble(3, a.getWeightKg()); else ps.setNull(3, java.sql.Types.REAL);
            if (a.getHrMax()    != null) ps.setInt(4, a.getHrMax());       else ps.setNull(4, java.sql.Types.INTEGER);
            if (a.getRestingHr()!= null) ps.setInt(5, a.getRestingHr());   else ps.setNull(5, java.sql.Types.INTEGER);
            ps.setString(6, a.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.error("updateAthleteProfile error", e); }
    }

    // ============================================================
    // MAPPERS
    // ============================================================

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setName(rs.getString("name"));
        String ca = rs.getString("created_at");
        if (ca != null) u.setCreatedAt(LocalDateTime.parse(ca, DT_FMT));
        String ll = rs.getString("last_login");
        if (ll != null) u.setLastLogin(LocalDateTime.parse(ll, DT_FMT));
        return u;
    }

    private Athlete mapAthlete(ResultSet rs) throws SQLException {
        Athlete a = new Athlete();
        a.setUserId(rs.getString("user_id"));
        String bd = rs.getString("birth_date");
        if (bd != null) a.setBirthDate(LocalDate.parse(bd, D_FMT));
        String sex = rs.getString("sex");
        if (sex != null) a.setSex(Athlete.Sex.valueOf(sex));
        a.setSport(rs.getString("sport"));
        double h = rs.getDouble("height_cm"); if (!rs.wasNull()) a.setHeightCm(h);
        double w = rs.getDouble("weight_kg"); if (!rs.wasNull()) a.setWeightKg(w);
        int hrm = rs.getInt("hr_max");        if (!rs.wasNull()) a.setHrMax(hrm);
        int rhr = rs.getInt("resting_hr");    if (!rs.wasNull()) a.setRestingHr(rhr);
        a.setPhotoPath(rs.getString("photo_path"));
        try { a.setName(rs.getString("name")); }   catch (SQLException ignored) {}
        try { a.setEmail(rs.getString("email")); }  catch (SQLException ignored) {}
        return a;
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setId(rs.getString("id"));
        t.setName(rs.getString("name"));
        t.setSport(rs.getString("sport"));
        String cat = rs.getString("category");
        if (cat != null) t.setCategory(Team.Category.valueOf(cat));
        t.setDescription(rs.getString("description"));
        t.setLogoPath(rs.getString("logo_path"));
        t.setPrimaryColor(rs.getString("primary_color"));
        t.setSecondaryColor(rs.getString("secondary_color"));
        t.setCoachId(rs.getString("coach_id"));
        t.setJoinCode(rs.getString("join_code"));
        t.setHrMaxReference(rs.getInt("hr_max_reference"));
        t.setSpo2AlertThreshold(rs.getDouble("spo2_alert_threshold"));
        t.setHrAlertThreshold(rs.getDouble("hr_alert_threshold"));
        t.setWeeklyLoadThreshold(rs.getDouble("weekly_load_threshold"));
        t.setEmergencyProtocol(rs.getString("emergency_protocol"));
        String ca = rs.getString("created_at");
        if (ca != null) t.setCreatedAt(LocalDateTime.parse(ca, DT_FMT));
        return t;
    }

    private Session mapSession(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getString("id"));
        s.setTeamId(rs.getString("team_id"));
        s.setCoachId(rs.getString("coach_id"));
        String st = rs.getString("start_time");
        if (st != null) s.setStartTime(LocalDateTime.parse(st, DT_FMT));
        String et = rs.getString("end_time");
        if (et != null) s.setEndTime(LocalDateTime.parse(et, DT_FMT));
        s.setExerciseType(rs.getString("exercise_type"));
        s.setTargetZoneMin(rs.getInt("target_zone_min"));
        s.setTargetZoneMax(rs.getInt("target_zone_max"));
        s.setNotes(rs.getString("notes"));
        s.setStatus(Session.Status.valueOf(rs.getString("status")));
        return s;
    }

    private SensorReading mapSensorReading(ResultSet rs) throws SQLException {
        SensorReading r = new SensorReading();
        r.setId(rs.getLong("id"));
        r.setAthleteId(rs.getString("athlete_id"));
        r.setSessionId(rs.getString("session_id"));
        r.setTimestamp(Instant.parse(rs.getString("timestamp")));
        r.setSensorType(SensorReading.SensorType.fromString(rs.getString("sensor_type")));
        r.setValue(rs.getDouble("value"));
        r.setUnit(rs.getString("unit"));
        r.setQuality(rs.getDouble("quality"));
        return r;
    }

    private Alert mapAlert(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getString("id"));
        a.setAthleteId(rs.getString("athlete_id"));
        try { a.setAthleteName(rs.getString("athlete_name")); } catch (SQLException ignored) {}
        a.setTeamId(rs.getString("team_id"));
        a.setSessionId(rs.getString("session_id"));
        String ts = rs.getString("timestamp");
        if (ts != null) a.setTimestamp(LocalDateTime.parse(ts, DT_FMT));
        a.setType(rs.getString("type"));
        a.setSeverity(Alert.Severity.valueOf(rs.getString("severity")));
        a.setParameter(rs.getString("parameter"));
        a.setValue(rs.getDouble("value"));
        a.setThreshold(rs.getDouble("threshold"));
        a.setAcknowledged(rs.getInt("acknowledged") == 1);
        a.setAcknowledgedBy(rs.getString("acknowledged_by"));
        String aat = rs.getString("acknowledged_at");
        if (aat != null) a.setAcknowledgedAt(LocalDateTime.parse(aat, DT_FMT));
        return a;
    }

    private SoapNote mapSoapNote(ResultSet rs) throws SQLException {
        SoapNote n = new SoapNote();
        n.setId(rs.getString("id"));
        n.setAthleteId(rs.getString("athlete_id"));
        n.setDoctorId(rs.getString("doctor_id"));
        n.setSessionId(rs.getString("session_id"));
        String d = rs.getString("date");
        if (d != null) n.setDate(LocalDate.parse(d, D_FMT));
        n.setSubjective(rs.getString("subjective"));
        n.setObjective(rs.getString("objective"));
        n.setAssessment(rs.getString("assessment"));
        n.setPlan(rs.getString("plan"));
        n.setAttachmentPath(rs.getString("attachment_path"));
        n.setPrivate(rs.getInt("is_private") == 1);
        String ca = rs.getString("created_at");
        if (ca != null) n.setCreatedAt(LocalDateTime.parse(ca, DT_FMT));
        try { n.setDoctorName(rs.getString("doctor_name")); } catch (SQLException ignored) {}
        return n;
    }

    private MedicalRestriction mapRestriction(ResultSet rs) throws SQLException {
        MedicalRestriction r = new MedicalRestriction();
        r.setId(rs.getString("id"));
        r.setAthleteId(rs.getString("athlete_id"));
        r.setDoctorId(rs.getString("doctor_id"));
        r.setType(MedicalRestriction.Type.valueOf(rs.getString("type")));
        int hl = rs.getInt("hr_limit"); if (!rs.wasNull()) r.setHrLimit(hl);
        r.setDescription(rs.getString("description"));
        String sd = rs.getString("start_date");
        if (sd != null) r.setStartDate(LocalDate.parse(sd, D_FMT));
        String rd = rs.getString("return_date");
        if (rd != null) r.setReturnDate(LocalDate.parse(rd, D_FMT));
        r.setStatus(MedicalRestriction.Status.valueOf(rs.getString("status").toUpperCase()));
        try { r.setAthleteName(rs.getString("athlete_name")); } catch (SQLException ignored) {}
        return r;
    }

    private MedicalRecord mapMedicalRecord(ResultSet rs) throws SQLException {
        MedicalRecord m = new MedicalRecord();
        m.setId(rs.getString("id"));
        m.setAthleteId(rs.getString("athlete_id"));
        m.setDoctorId(rs.getString("doctor_id"));
        m.setFieldType(MedicalRecord.FieldType.valueOf(rs.getString("field_type").toUpperCase()));
        m.setFieldValue(rs.getString("field_value"));
        m.setSeverity(rs.getString("severity"));
        String ca = rs.getString("created_at");
        if (ca != null) m.setCreatedAt(LocalDateTime.parse(ca, DT_FMT));
        return m;
    }

    private ChatMessage mapChatMessage(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setId(rs.getString("id"));
        m.setTeamId(rs.getString("team_id"));
        m.setSenderId(rs.getString("sender_id"));
        m.setMessage(rs.getString("message"));
        String ts = rs.getString("timestamp");
        if (ts != null) m.setTimestamp(LocalDateTime.parse(ts, DT_FMT));
        m.setRead(rs.getInt("is_read") == 1);
        try { m.setSenderName(rs.getString("sender_name")); } catch (SQLException ignored) {}
        return m;
    }
}
