package com.medsport.services;

import com.medsport.models.Alert;
import com.medsport.models.SensorReading;
import com.medsport.models.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Evaluates incoming sensor readings against team thresholds and fires alerts.
 */
public class AlertService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

    private final DatabaseService db = DatabaseService.getInstance();

    // Deduplication: athlete_id -> parameter -> last alert timestamp (ms)
    private final Map<String, Map<String, Long>> recentAlerts = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 30_000; // 30 seconds between same alerts

    private final List<Consumer<Alert>> listeners = new ArrayList<>();

    public void addAlertListener(Consumer<Alert> listener) {
        listeners.add(listener);
    }

    public void evaluate(SensorReading reading, Team team, String sessionId) {
        if (team == null || reading == null) return;

        Alert alert = null;

        switch (reading.getSensorType()) {
            case HR -> alert = evaluateHr(reading, team, sessionId);
            case SPO2 -> alert = evaluateSpo2(reading, team, sessionId);
            default -> { /* other sensors don't trigger alerts */ }
        }

        if (alert != null && !isDuplicate(alert)) {
            try {
                db.saveAlert(alert);
                recordAlertTime(alert);
                notifyListeners(alert);
                LOG.warn("ALERT [{}] {} — {} = {}", alert.getSeverity(), alert.getAthleteId(),
                         alert.getParameter(), alert.getValue());
            } catch (Exception e) {
                LOG.error("Failed to save alert", e);
            }
        }
    }

    private Alert evaluateHr(SensorReading r, Team team, String sessionId) {
        // Get athlete profile for personal HRmax — fallback to team reference
        double hrMax = team.getHrMaxReference();
        var profileOpt = db.findAthleteProfile(r.getAthleteId());
        if (profileOpt.isPresent() && profileOpt.get().getHrMax() != null) {
            hrMax = profileOpt.get().getHrMax();
        }

        double absoluteMax = hrMax * 1.05;    // 105% = critical
        double threshold   = hrMax * (team.getHrAlertThreshold() / 100.0); // default 95%

        if (r.getValue() >= absoluteMax) {
            return buildAlert(r, team.getId(), sessionId, "HR_ABOVE_ABSOLUTE_MAX",
                Alert.Severity.CRITICAL, "heart_rate", absoluteMax);
        } else if (r.getValue() >= threshold) {
            return buildAlert(r, team.getId(), sessionId, "HR_ABOVE_MAX",
                Alert.Severity.WARNING, "heart_rate", threshold);
        }
        return null;
    }

    private Alert evaluateSpo2(SensorReading r, Team team, String sessionId) {
        double criticalThreshold = 90.0;
        double warnThreshold     = team.getSpo2AlertThreshold(); // default 93%

        if (r.getValue() <= criticalThreshold) {
            return buildAlert(r, team.getId(), sessionId, "SPO2_CRITICAL",
                Alert.Severity.CRITICAL, "spo2", criticalThreshold);
        } else if (r.getValue() <= warnThreshold) {
            return buildAlert(r, team.getId(), sessionId, "SPO2_LOW",
                Alert.Severity.WARNING, "spo2", warnThreshold);
        }
        return null;
    }

    private Alert buildAlert(SensorReading r, String teamId, String sessionId,
                              String type, Alert.Severity severity, String param, double threshold) {
        Alert a = new Alert();
        a.setId("alert_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        a.setAthleteId(r.getAthleteId());
        a.setTeamId(teamId);
        a.setSessionId(sessionId);
        a.setType(type);
        a.setSeverity(severity);
        a.setParameter(param);
        a.setValue(r.getValue());
        a.setThreshold(threshold);
        return a;
    }

    private boolean isDuplicate(Alert a) {
        long now = System.currentTimeMillis();
        Map<String, Long> byParam = recentAlerts.computeIfAbsent(a.getAthleteId(), k -> new ConcurrentHashMap<>());
        Long last = byParam.get(a.getParameter());
        return last != null && (now - last) < DEDUP_WINDOW_MS;
    }

    private void recordAlertTime(Alert a) {
        recentAlerts.computeIfAbsent(a.getAthleteId(), k -> new ConcurrentHashMap<>())
                    .put(a.getParameter(), System.currentTimeMillis());
    }

    private void notifyListeners(Alert a) {
        for (Consumer<Alert> l : listeners) {
            try { l.accept(a); } catch (Exception e) { LOG.error("Alert listener error", e); }
        }
    }

    public List<Alert> getActiveAlerts(String teamId) {
        return db.findActiveAlerts(teamId);
    }

    public void acknowledge(String alertId, String doctorId) throws Exception {
        db.acknowledgeAlert(alertId, doctorId);
    }
}
