package com.medsport.controllers.athlete;

import com.medsport.auth.SessionManager;
import com.medsport.models.*;
import com.medsport.services.*;
import com.medsport.views.components.ViewTransition;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Vital cards ──
    @FXML private Label hrValueLabel;
    @FXML private Label hrZoneLabel;
    @FXML private Label hrTrendLabel;
    @FXML private Pane  hrPulseDot;
    @FXML private Label spo2ValueLabel;
    @FXML private Label spo2StatusLabel;
    @FXML private Label speedValueLabel;
    @FXML private Label cadenceValueLabel;

    // ── Zone bars ──
    @FXML private ProgressBar zone1Bar, zone2Bar, zone3Bar, zone4Bar, zone5Bar;
    @FXML private Label       zone1Pct, zone2Pct, zone3Pct, zone4Pct, zone5Pct;

    // ── Session card ──
    @FXML private Label  sessionStatusLabel;
    @FXML private Label  sessionTypeLabel;
    @FXML private Label  sessionDurationLabel;
    @FXML private Label  targetZoneLabel;
    @FXML private Pane   sessionLiveDot;
    @FXML private VBox   noSessionPane;
    @FXML private VBox   activeSessionPane;

    // ── GPS mini-map ──
    @FXML private Canvas gpsCanvas;

    // ── Last update ──
    @FXML private Label lastUpdateLabel;
    @FXML private Label mqttStatusLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final AppContext      ctx     = AppContext.getInstance();
    private final MqttService     mqtt    = ctx.getMqtt();
    private final SessionService  sessSvc = ctx.getSessions();

    private Timeline refreshTimeline;
    private Timeline sessionClockTimeline;
    private Timeline hrPulseTimeline;

    // Zone accumulation counters (readings since load)
    private final int[] zoneCounts = new int[6];   // index 1-5
    private int totalZoneReadings = 0;
    private long sessionStartEpoch = 0;

    // GPS track points (lat, lon) — normalized to canvas
    private final List<double[]> gpsTrack = new ArrayList<>();
    private double gpsCenterLat = Double.NaN, gpsCenterLon = Double.NaN;
    private double gpsScaleMPx  = 8.0;   // pixels per meter

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Register MQTT listener for this athlete
        String athleteId = session.getUserId();
        mqtt.addReadingListener(reading -> {
            if (!athleteId.equals(reading.getAthleteId())) return;
            Platform.runLater(() -> processReading(reading));
        });

        // Refresh session status every 10 s
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> refreshSession()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        // Pulse animation on HR dot
        setupPulseAnimation();

        // Initial load
        refreshSession();
        updateMqttStatus();

        // Mark MQTT connected label
        if (mqttStatusLabel != null) {
            mqttStatusLabel.setText(mqtt.isConnected() ? "LIVE" : "DEMO");
            mqttStatusLabel.setStyle(mqtt.isConnected()
                ? "-fx-text-fill:#3fb950;" : "-fx-text-fill:#d29922;");
        }
    }

    // ── MQTT data processing ─────────────────────────────────────────────────

    private void processReading(SensorReading r) {
        if (lastUpdateLabel != null)
            lastUpdateLabel.setText("Updated " + java.time.LocalTime.now().format(TIME_FMT));

        switch (r.getSensorType()) {
            case HR      -> updateHr((int) r.getValue());
            case SPO2    -> updateSpo2(r.getValue());
            case GPS_LAT -> updateGpsLat(r.getValue());
            case GPS_LON -> updateGpsLon(r.getValue());
            case GPS_SPEED -> {
                double kmh = r.getValue() * 3.6;
                if (speedValueLabel != null)
                    speedValueLabel.setText(String.format("%.1f km/h", kmh));
            }
            case CADENCE -> {
                if (cadenceValueLabel != null)
                    cadenceValueLabel.setText(String.format("%d spm", (int) r.getValue()));
            }
            default -> {}
        }
    }

    private void updateHr(int hr) {
        if (hrValueLabel != null) {
            animateCountUp(hrValueLabel, hr);
        }

        // Zone calculation (Karvonen 5-zone)
        int[] zone = computeZone(hr);
        int z = zone[0];
        if (hrZoneLabel != null) {
            String[] zNames = {"", "Zone 1", "Zone 2", "Zone 3", "Zone 4", "Zone 5"};
            String[] zDesc  = {"", "Recovery", "Aerobic", "Tempo", "Threshold", "VO2max"};
            String[] zCss   = {"", "zone1", "zone2", "zone3", "zone4", "zone5"};
            hrZoneLabel.setText(z >= 1 && z <= 5 ? zNames[z] + " — " + zDesc[z] : "");
            hrZoneLabel.getStyleClass().removeAll("zone1","zone2","zone3","zone4","zone5");
            if (z >= 1 && z <= 5) hrZoneLabel.getStyleClass().add(zCss[z]);
        }

        // Accumulate zone counts for bars
        if (z >= 1 && z <= 5) {
            zoneCounts[z]++;
            totalZoneReadings++;
            updateZoneBars();
        }

        // Pulse animation speed proportional to HR
        updatePulseRate(hr);
    }

    private void updateSpo2(double val) {
        if (spo2ValueLabel  != null) spo2ValueLabel.setText(String.format("%.1f%%", val));
        if (spo2StatusLabel != null) {
            if (val <= 90)      { spo2StatusLabel.setText("CRITICAL"); spo2StatusLabel.setStyle("-fx-text-fill:#f85149;"); }
            else if (val <= 93) { spo2StatusLabel.setText("LOW");      spo2StatusLabel.setStyle("-fx-text-fill:#d29922;"); }
            else                { spo2StatusLabel.setText("Normal");   spo2StatusLabel.setStyle("-fx-text-fill:#3fb950;"); }
        }
    }

    private double latBuffer = Double.NaN, lonBuffer = Double.NaN;

    private void updateGpsLat(double lat) { latBuffer = lat; maybeAddGpsPoint(); }
    private void updateGpsLon(double lon) { lonBuffer = lon; maybeAddGpsPoint(); }

    private void maybeAddGpsPoint() {
        if (Double.isNaN(latBuffer) || Double.isNaN(lonBuffer)) return;
        double lat = latBuffer, lon = lonBuffer;
        latBuffer = Double.NaN; lonBuffer = Double.NaN;

        if (Double.isNaN(gpsCenterLat)) { gpsCenterLat = lat; gpsCenterLon = lon; }
        gpsTrack.add(new double[]{lat, lon});
        if (gpsTrack.size() > 2000) gpsTrack.remove(0); // rolling window
        drawGpsTrack();
    }

    // ── GPS Canvas ───────────────────────────────────────────────────────────

    private void drawGpsTrack() {
        if (gpsCanvas == null || gpsTrack.isEmpty()) return;
        GraphicsContext gc = gpsCanvas.getGraphicsContext2D();
        double w = gpsCanvas.getWidth(), h = gpsCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        // Background
        gc.setFill(Color.web("#0a0e14"));
        gc.fillRoundRect(0, 0, w, h, 12, 12);

        // Grid lines
        gc.setStroke(Color.web("#21262d", 0.5));
        gc.setLineWidth(0.5);
        for (int i = 1; i < 4; i++) {
            gc.strokeLine(w * i / 4, 0, w * i / 4, h);
            gc.strokeLine(0, h * i / 4, w, h * i / 4);
        }

        if (gpsTrack.size() < 2) return;

        // Auto-scale to fit all points in canvas
        double minLat = gpsTrack.stream().mapToDouble(p -> p[0]).min().orElse(gpsCenterLat);
        double maxLat = gpsTrack.stream().mapToDouble(p -> p[0]).max().orElse(gpsCenterLat);
        double minLon = gpsTrack.stream().mapToDouble(p -> p[1]).min().orElse(gpsCenterLon);
        double maxLon = gpsTrack.stream().mapToDouble(p -> p[1]).max().orElse(gpsCenterLon);
        double spanLat = Math.max(maxLat - minLat, 0.0002);
        double spanLon = Math.max(maxLon - minLon, 0.0002);
        double margin = 20;
        double scaleX = (w - margin * 2) / spanLon;
        double scaleY = (h - margin * 2) / spanLat;

        // Track path
        gc.setStroke(Color.web("#0A84FF", 0.6));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < gpsTrack.size(); i++) {
            double[] p = gpsTrack.get(i);
            double x = margin + (p[1] - minLon) * scaleX;
            double y = h - margin - (p[0] - minLat) * scaleY;
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        // Current position dot
        double[] cur = gpsTrack.get(gpsTrack.size() - 1);
        double cx = margin + (cur[1] - minLon) * scaleX;
        double cy = h - margin - (cur[0] - minLat) * scaleY;
        gc.setFill(Color.web("#0A84FF"));
        gc.fillOval(cx - 6, cy - 6, 12, 12);
        gc.setFill(Color.web("#0A84FF", 0.3));
        gc.fillOval(cx - 12, cy - 12, 24, 24);

        // Start marker
        double[] start = gpsTrack.get(0);
        double sx = margin + (start[1] - minLon) * scaleX;
        double sy = h - margin - (start[0] - minLat) * scaleY;
        gc.setFill(Color.web("#3fb950"));
        gc.fillOval(sx - 5, sy - 5, 10, 10);
    }

    // ── Zone bars ────────────────────────────────────────────────────────────

    private void updateZoneBars() {
        if (totalZoneReadings == 0) return;
        ProgressBar[] bars = {null, zone1Bar, zone2Bar, zone3Bar, zone4Bar, zone5Bar};
        Label[] labels     = {null, zone1Pct, zone2Pct, zone3Pct, zone4Pct, zone5Pct};
        for (int z = 1; z <= 5; z++) {
            double pct = (double) zoneCounts[z] / totalZoneReadings;
            if (bars[z]   != null) bars[z].setProgress(pct);
            if (labels[z] != null) labels[z].setText(String.format("%.0f%%", pct * 100));
        }
    }

    // ── Session card ─────────────────────────────────────────────────────────

    private void refreshSession() {
        String teamId = session.getTeamId();
        if (teamId == null) {
            showPane(noSessionPane, true);
            showPane(activeSessionPane, false);
            return;
        }
        Optional<Session> active = sessSvc.getActiveSession(teamId);
        Platform.runLater(() -> {
            if (active.isPresent()) {
                Session s = active.get();
                showPane(noSessionPane, false);
                showPane(activeSessionPane, true);
                if (sessionTypeLabel    != null) sessionTypeLabel.setText(
                    s.getExerciseType() != null ? s.getExerciseType() : "Training");
                if (targetZoneLabel     != null) targetZoneLabel.setText(
                    "Zone " + s.getTargetZoneMin() + "–" + s.getTargetZoneMax());
                if (sessionLiveDot      != null) sessionLiveDot.setVisible(true);
                sessionStartEpoch = s.getStartTime() != null
                    ? s.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC) : System.currentTimeMillis() / 1000;
                startSessionClock();
            } else {
                showPane(noSessionPane, true);
                showPane(activeSessionPane, false);
                stopSessionClock();
            }
        });
    }

    private void startSessionClock() {
        if (sessionClockTimeline != null) sessionClockTimeline.stop();
        sessionClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long elapsed = System.currentTimeMillis() / 1000 - sessionStartEpoch;
            if (sessionDurationLabel != null)
                sessionDurationLabel.setText(formatDuration(elapsed));
        }));
        sessionClockTimeline.setCycleCount(Timeline.INDEFINITE);
        sessionClockTimeline.play();
    }

    private void stopSessionClock() {
        if (sessionClockTimeline != null) sessionClockTimeline.stop();
    }

    // ── Pulse animation ───────────────────────────────────────────────────────

    private void setupPulseAnimation() {
        if (hrPulseDot == null) return;
        hrPulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(hrPulseDot.scaleXProperty(), 1.0),
                new KeyValue(hrPulseDot.scaleYProperty(), 1.0),
                new KeyValue(hrPulseDot.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(300),
                new KeyValue(hrPulseDot.scaleXProperty(), 1.6),
                new KeyValue(hrPulseDot.scaleYProperty(), 1.6),
                new KeyValue(hrPulseDot.opacityProperty(), 0.2)),
            new KeyFrame(Duration.millis(600),
                new KeyValue(hrPulseDot.scaleXProperty(), 1.0),
                new KeyValue(hrPulseDot.scaleYProperty(), 1.0),
                new KeyValue(hrPulseDot.opacityProperty(), 0.9))
        );
        hrPulseTimeline.setCycleCount(Timeline.INDEFINITE);
        hrPulseTimeline.setRate(1.1);
        hrPulseTimeline.play();
    }

    private void updatePulseRate(int hr) {
        if (hrPulseTimeline == null) return;
        // 60 bpm → rate 1.0,  180 bpm → rate 3.0
        double rate = Math.max(0.5, Math.min(4.0, hr / 60.0));
        hrPulseTimeline.setRate(rate);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int[] computeZone(int hr) {
        // Karvonen zones — use HRmax from athlete profile or 190 default
        int hrMax = 190;
        try {
            var opt = ctx.getDatabase().findAthleteProfile(session.getUserId());
            if (opt.isPresent()) hrMax = opt.get().calculateHrMax();
        } catch (Exception ignored) {}

        double pct = (double) hr / hrMax * 100;
        int zone;
        if      (pct < 60) zone = 1;
        else if (pct < 70) zone = 2;
        else if (pct < 80) zone = 3;
        else if (pct < 90) zone = 4;
        else               zone = 5;
        return new int[]{zone};
    }

    private void animateCountUp(Label label, int target) {
        String current = label.getText().replaceAll("[^0-9]", "");
        int from = current.isEmpty() ? target : Integer.parseInt(current);
        if (from == target) return;
        Timeline tl = new Timeline();
        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            int val = from + (target - from) * i / steps;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 40L), e ->
                label.setText(String.valueOf(val))));
        }
        tl.play();
    }

    private void updateMqttStatus() {
        if (mqttStatusLabel == null) return;
        mqttStatusLabel.setText(mqtt.isConnected() ? "LIVE" : "DEMO");
    }

    private void showPane(Pane p, boolean v) {
        if (p != null) { p.setVisible(v); p.setManaged(v); }
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    public void stop() {
        if (refreshTimeline     != null) refreshTimeline.stop();
        if (sessionClockTimeline!= null) sessionClockTimeline.stop();
        if (hrPulseTimeline     != null) hrPulseTimeline.stop();
        mqtt.removeReadingListener(this::processReading);
    }
}