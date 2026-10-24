package com.medsport.services;

import com.medsport.models.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SessionService {

    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);
    private final DatabaseService db = DatabaseService.getInstance();

    public Session startSession(Session session) throws Exception {
        session.setId("sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        session.setStartTime(LocalDateTime.now());
        session.setStatus(Session.Status.ACTIVE);
        db.saveSession(session);
        LOG.info("Session started: {}", session.getId());
        return session;
    }

    public void endSession(String sessionId) throws Exception {
        // Load the session, mark completed, save
        // We rely on a raw SQL update for efficiency
        try (var ps = db.getConnection().prepareStatement(
                "UPDATE training_sessions SET end_time = ?, status = 'COMPLETED' WHERE id = ?")) {
            ps.setString(1, LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setString(2, sessionId);
            ps.executeUpdate();
        }
        LOG.info("Session completed: {}", sessionId);
    }

    public void cancelSession(String sessionId) throws Exception {
        try (var ps = db.getConnection().prepareStatement(
                "UPDATE training_sessions SET status = 'CANCELLED' WHERE id = ?")) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        }
    }

    public List<Session> getTeamSessions(String teamId) {
        return db.findSessionsByTeam(teamId);
    }

    public List<Session> getAthleteSessions(String athleteId) {
        return db.findSessionsByAthlete(athleteId);
    }

    public Optional<Session> getActiveSession(String teamId) {
        return db.findActiveSession(teamId);
    }
}
