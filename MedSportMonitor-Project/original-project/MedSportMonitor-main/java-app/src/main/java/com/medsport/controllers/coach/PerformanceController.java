package com.medsport.controllers.coach;

import com.fasterxml.jackson.databind.JsonNode;
import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.Session;
import com.medsport.services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class PerformanceController implements Initializable {

    @FXML private ComboBox<String> athleteCombo;
    @FXML private Canvas           radarCanvas;
    @FXML private Label            avgHrLabel, maxHrLabel, avgSpo2Label;
    @FXML private Label            totalDistLabel, trainingLoadLabel;
    @FXML private Label            acrLabel;
    @FXML private Label            apiStatusLabel;

    private final SessionManager       session   = SessionManager.getInstance();
    private final DatabaseService       db        = AppContext.getInstance().getDatabase();
    private final AnalyticsRestClient   analytics = AppContext.getInstance().getAnalytics();
    private final SessionService        sessSvc   = AppContext.getInstance().getSessions();

    private List<Athlete> athletes = new ArrayList<>();
    private final String[] AXES = {"Endurance","Speed","Recovery","Consistency","Load","Intensity"};
    private final Map<String, double[]> radarData = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());

        Random rng = new Random(99);
        for (Athlete a : athletes) {
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) v[i] = 0.35 + rng.nextDouble() * 0.60;
            String name = a.getName() != null ? a.getName() : a.getUserId();
            radarData.put(name, v);
        }

        if (athleteCombo != null) {
            athletes.forEach(a -> athleteCombo.getItems().add(a.getName() != null ? a.getName() : a.getUserId()));
            if (!athleteCombo.getItems().isEmpty()) {
                athleteCombo.setValue(athleteCombo.getItems().get(0));
                athleteCombo.setOnAction(e -> handleAthleteSelected());
            }
        }
        handleAthleteSelected();
        fetchTeamLoad();
    }

    @FXML private void handleAthleteSelected() {
        String name = athleteCombo != null ? athleteCombo.getValue() : null;
        if (name == null) return;
        drawRadar(name);
        fetchAthleteAnalytics(name);
    }

    // ── Analytics calls ───────────────────────────────────────────────────────

    private void fetchAthleteAnalytics(String athleteName) {
        int idx = athleteCombo != null ? athleteCombo.getItems().indexOf(athleteName) : -1;
        if (idx < 0 || idx >= athletes.size()) return;
        Athlete a = athletes.get(idx);

        Athlete profile = db.findAthleteProfile(a.getUserId()).orElse(null);
        double hrMax  = profile != null ? profile.calculateHrMax() : 190.0;
        double restHr = profile != null && profile.getRestingHr() != null && profile.getRestingHr() > 0 ? profile.getRestingHr() : 55.0;

        List<Session> dbSessions = sessSvc.getAthleteSessions(a.getUserId());
        List<Map<String,Object>> sessionMaps = new ArrayList<>();
        for (Session s : dbSessions) {
            Map<String,Object> m = new HashMap<>();
            m.put("session_id",   s.getId());
            m.put("duration_min", s.getDurationMinutes());
            m.put("type",         s.getExerciseType() != null ? s.getExerciseType() : "Training");
            m.put("hr_max",       hrMax);
            m.put("resting_hr",   restHr);
            sessionMaps.add(m);
        }

        // Radar
        analytics.radarMetrics(a.getUserId(), sessionMaps).thenAccept(node -> {
            if (node == null) return;
            JsonNode metrics = node.path("metrics");
            if (!metrics.isMissingNode()) {
                double[] vals = {
                    metrics.path("endurance").asDouble(radarData.getOrDefault(athleteName, new double[6])[0]),
                    metrics.path("speed").asDouble(0.5),
                    metrics.path("recovery").asDouble(0.5),
                    metrics.path("consistency").asDouble(0.5),
                    metrics.path("load").asDouble(0.5),
                    metrics.path("intensity").asDouble(0.5)
                };
                Platform.runLater(() -> { radarData.put(athleteName, vals); drawRadar(athleteName); });
            }
        });

        // Fatigue / ACR
        List<Map<String,Object>> last7 = sessionMaps.stream().limit(7).toList();
        analytics.fatigueIndex(a.getUserId(), last7).thenAccept(node -> {
            if (node == null) return;
            double acr  = node.path("acr").asDouble(-1);
            String cls  = node.path("classification").asText("—");
            double load = node.path("training_load").asDouble(-1);
            Platform.runLater(() -> {
                if (acrLabel          != null && acr  >= 0) acrLabel.setText(String.format("%.2f  (%s)", acr, cls));
                if (trainingLoadLabel != null && load >= 0) trainingLoadLabel.setText(String.format("%.0f AU", load));
                setApiStatus(true);
            });
        }).exceptionally(e -> { Platform.runLater(() -> setApiStatus(false)); return null; });

        // Heart stats from latest session
        if (!dbSessions.isEmpty()) {
            Map<String,Object> sd = new HashMap<>();
            sd.put("hr_max", hrMax); sd.put("resting_hr", restHr); sd.put("hr_series", List.of());
            analytics.sessionSummary(dbSessions.get(0).getId(), sd).thenAccept(node -> {
                if (node == null) return;
                double avgHr = node.path("avg_hr").asDouble(-1);
                double maxHr = node.path("max_hr").asDouble(-1);
                Platform.runLater(() -> {
                    if (avgHrLabel != null && avgHr > 0) avgHrLabel.setText(String.format("%.0f bpm", avgHr));
                    if (maxHrLabel != null && maxHr > 0) maxHrLabel.setText(String.format("%.0f bpm", maxHr));
                });
            });
        }
    }

    private void fetchTeamLoad() {
        if (!session.hasTeam()) return;
        List<Map<String,Object>> athleteMaps = new ArrayList<>();
        for (Athlete a : athletes) {
            Athlete p = db.findAthleteProfile(a.getUserId()).orElse(null);
            Map<String,Object> m = new HashMap<>();
            m.put("athlete_id", a.getUserId());
            m.put("name", a.getName() != null ? a.getName() : a.getUserId());
            m.put("sessions", List.of());
            athleteMaps.add(m);
        }
        analytics.teamTrainingLoad(session.getTeamId(), athleteMaps, 7).thenAccept(node -> {
            if (node == null) return;
            double teamAvg = node.path("team_avg_load").asDouble(-1);
            Platform.runLater(() -> {
                if (trainingLoadLabel != null && teamAvg > 0)
                    trainingLoadLabel.setText(String.format("%.0f AU (team avg)", teamAvg));
            });
        });
    }

    // ── Radar draw ────────────────────────────────────────────────────────────

    private void drawRadar(String athleteName) {
        if (radarCanvas == null) return;
        double[] vals = radarData.getOrDefault(athleteName, new double[]{0.7,0.6,0.8,0.65,0.7,0.55});
        double cx = radarCanvas.getWidth()/2, cy = radarCanvas.getHeight()/2;
        double r = Math.min(cx, cy) * 0.72;
        int n = AXES.length;
        GraphicsContext gc = radarCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, radarCanvas.getWidth(), radarCanvas.getHeight());
        gc.setFill(Color.web("#0d1117")); gc.fillRect(0, 0, radarCanvas.getWidth(), radarCanvas.getHeight());

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
            gc.fillText(AXES[i], cx+(r+22)*Math.cos(a)-AXES[i].length()*3, cy+(r+22)*Math.sin(a)+4);
        }
        gc.setFill(Color.web("#00C853", 0.25)); gc.setStroke(Color.web("#00C853")); gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= n; i++) {
            double a = Math.PI*2*(i%n)/n - Math.PI/2;
            if (i==0) gc.moveTo(cx+r*vals[i%n]*Math.cos(a), cy+r*vals[i%n]*Math.sin(a));
            else       gc.lineTo(cx+r*vals[i%n]*Math.cos(a), cy+r*vals[i%n]*Math.sin(a));
        }
        gc.closePath(); gc.fill(); gc.stroke();
        gc.setFill(Color.web("#00C853"));
        for (int i = 0; i < n; i++) {
            double a = Math.PI*2*i/n - Math.PI/2;
            gc.fillOval(cx+r*vals[i]*Math.cos(a)-4, cy+r*vals[i]*Math.sin(a)-4, 8, 8);
        }
    }

    private void setApiStatus(boolean ok) {
        if (apiStatusLabel == null) return;
        apiStatusLabel.setText(ok ? "● Analytics live" : "● Analytics offline");
        apiStatusLabel.setStyle("-fx-text-fill:" + (ok ? "#3fb950" : "#8b949e") + "; -fx-font-size:11px;");
    }
}