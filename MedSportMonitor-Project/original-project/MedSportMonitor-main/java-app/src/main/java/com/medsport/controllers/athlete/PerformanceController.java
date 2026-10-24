package com.medsport.controllers.athlete;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.Session;
import com.medsport.services.*;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class PerformanceController implements Initializable {

    @FXML private Canvas  radarCanvas;
    @FXML private Label   avgHrLabel;
    @FXML private Label   maxHrLabel;
    @FXML private Label   avgSpo2Label;
    @FXML private Label   avgSpeedLabel;
    @FXML private Label   totalDistLabel;
    @FXML private Label   trainingLoadLabel;
    @FXML private ListView<String> recordsList;
    @FXML private Label   acrLabel;
    @FXML private Label   apiStatusLabel;

    private final SessionManager       session   = SessionManager.getInstance();
    private final AppContext            ctx       = AppContext.getInstance();
    private final DatabaseService       db        = ctx.getDatabase();
    private final AnalyticsRestClient   analytics = ctx.getAnalytics();
    private final SessionService        sessSvc   = ctx.getSessions();

    private final String[] AXES = {"Endurance","Speed","Recovery","Consistency","Load","Intensity"};
    private double[] radarValues = {0.72, 0.58, 0.80, 0.65, 0.70, 0.55};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        drawRadar();
        loadRecords();
        fetchAnalytics();
    }

    // ── Fetch from analytics API ─────────────────────────────────────────────

    private void fetchAnalytics() {
        String userId = session.getUserId();
        if (userId == null) return;

        Athlete profile = db.findAthleteProfile(userId).orElse(null);
        double hrMax    = profile != null ? profile.calculateHrMax() : 190.0;
        double restHr   = profile != null && profile.getRestingHr() != null && profile.getRestingHr() > 0 ? profile.getRestingHr() : 55.0;

        // Build session stubs for API payload
        List<Session> dbSessions = sessSvc.getAthleteSessions(userId);
        List<Map<String,Object>> sessionMaps = new ArrayList<>();
        for (Session s : dbSessions) {
            Map<String,Object> m = new HashMap<>();
            m.put("session_id",  s.getId());
            m.put("duration_min", s.getDurationMinutes());
            m.put("type",        s.getExerciseType() != null ? s.getExerciseType() : "Training");
            m.put("hr_max",      hrMax);
            m.put("resting_hr",  restHr);
            sessionMaps.add(m);
        }

        // Radar metrics
        analytics.radarMetrics(userId, sessionMaps).thenAccept(node -> {
            if (node == null) return;
            JsonNode metrics = node.path("metrics");
            if (!metrics.isMissingNode()) {
                double[] vals = {
                    metrics.path("endurance").asDouble(radarValues[0]),
                    metrics.path("speed").asDouble(radarValues[1]),
                    metrics.path("recovery").asDouble(radarValues[2]),
                    metrics.path("consistency").asDouble(radarValues[3]),
                    metrics.path("load").asDouble(radarValues[4]),
                    metrics.path("intensity").asDouble(radarValues[5])
                };
                Platform.runLater(() -> { radarValues = vals; drawRadar(); });
            }
        });

        // Fatigue index (last 7 sessions)
        List<Map<String,Object>> last7 = sessionMaps.stream()
            .sorted((a,b) -> 0).limit(7).toList();
        analytics.fatigueIndex(userId, last7).thenAccept(node -> {
            if (node == null) return;
            double acr   = node.path("acr").asDouble(-1);
            String cls   = node.path("classification").asText("—");
            double load  = node.path("training_load").asDouble(-1);
            Platform.runLater(() -> {
                if (acrLabel          != null && acr  >= 0) acrLabel.setText(String.format("%.2f  (%s)", acr, cls));
                if (trainingLoadLabel != null && load >= 0) trainingLoadLabel.setText(String.format("%.0f AU", load));
                if (apiStatusLabel    != null) setApiStatus(true);
            });
        });

        // Heart zone stats from latest session
        if (!dbSessions.isEmpty()) {
            String latestId = dbSessions.get(0).getId();
            Map<String,Object> sensorData = new HashMap<>();
            sensorData.put("hr_max", hrMax);
            sensorData.put("resting_hr", restHr);
            sensorData.put("hr_series", List.of()); // placeholder — real readings from DB not yet buffered
            analytics.sessionSummary(latestId, sensorData).thenAccept(node -> {
                if (node == null) return;
                double avgHr  = node.path("avg_hr").asDouble(-1);
                double maxHr  = node.path("max_hr").asDouble(-1);
                double avgSpd = node.path("avg_speed_kmh").asDouble(-1);
                Platform.runLater(() -> {
                    if (avgHrLabel   != null && avgHr  > 0) avgHrLabel.setText(String.format("%.0f bpm", avgHr));
                    if (maxHrLabel   != null && maxHr  > 0) maxHrLabel.setText(String.format("%.0f bpm", maxHr));
                    if (avgSpeedLabel != null && avgSpd > 0) avgSpeedLabel.setText(String.format("%.1f km/h", avgSpd));
                    if (apiStatusLabel != null) setApiStatus(true);
                });
            }).exceptionally(e -> { Platform.runLater(() -> setApiStatus(false)); return null; });
        }
    }

    // ── Radar draw ─────────────────────────────────────────────────────────

    private void drawRadar() {
        if (radarCanvas == null) return;
        double cx = radarCanvas.getWidth() / 2, cy = radarCanvas.getHeight() / 2;
        double r = Math.min(cx, cy) * 0.75;
        int n = AXES.length;
        GraphicsContext gc = radarCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, radarCanvas.getWidth(), radarCanvas.getHeight());

        gc.setStroke(Color.web("#21262d", 0.7)); gc.setLineWidth(0.8);
        for (int ring = 1; ring <= 5; ring++) {
            double rr = r * ring / 5.0;
            gc.beginPath();
            for (int i = 0; i <= n; i++) {
                double a = Math.PI*2*i/n - Math.PI/2;
                if (i==0) gc.moveTo(cx+rr*Math.cos(a), cy+rr*Math.sin(a));
                else       gc.lineTo(cx+rr*Math.cos(a), cy+rr*Math.sin(a));
            }
            gc.closePath(); gc.stroke();
        }
        gc.setFill(Color.web("#8b949e")); gc.setFont(javafx.scene.text.Font.font(11));
        for (int i = 0; i < n; i++) {
            double a = Math.PI*2*i/n - Math.PI/2;
            gc.strokeLine(cx, cy, cx+r*Math.cos(a), cy+r*Math.sin(a));
            gc.fillText(AXES[i], cx+(r+20)*Math.cos(a)-AXES[i].length()*3, cy+(r+20)*Math.sin(a)+4);
        }
        gc.setFill(Color.web("#0A84FF", 0.25)); gc.setStroke(Color.web("#0A84FF")); gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= n; i++) {
            double a = Math.PI*2*(i%n)/n - Math.PI/2;
            if (i==0) gc.moveTo(cx+r*radarValues[i%n]*Math.cos(a), cy+r*radarValues[i%n]*Math.sin(a));
            else       gc.lineTo(cx+r*radarValues[i%n]*Math.cos(a), cy+r*radarValues[i%n]*Math.sin(a));
        }
        gc.closePath(); gc.fill(); gc.stroke();
        gc.setFill(Color.web("#0A84FF"));
        for (int i = 0; i < n; i++) {
            double a = Math.PI*2*i/n - Math.PI/2;
            gc.fillOval(cx+r*radarValues[i]*Math.cos(a)-4, cy+r*radarValues[i]*Math.sin(a)-4, 8, 8);
        }
    }

    private void loadRecords() {
        if (recordsList == null) return;
        recordsList.getItems().addAll(
            "Max HR:        — bpm",
            "Max Speed:     — km/h",
            "Longest run:   — km",
            "Best 5 km:     —",
            "Max Load:      — AU"
        );
    }

    private void setApiStatus(boolean ok) {
        if (apiStatusLabel == null) return;
        apiStatusLabel.setText(ok ? "● Analytics live" : "● Analytics offline");
        apiStatusLabel.setStyle("-fx-text-fill:" + (ok ? "#3fb950" : "#8b949e")
            + "; -fx-font-size:11px;");
    }

    @FXML private void handleRefresh() { fetchAnalytics(); }
}