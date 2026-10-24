package com.medsport.controllers.coach;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SimulatorController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatorController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG_LINES = 200;

    // Connection panel
    @FXML private Label   mqttStatusLabel;
    @FXML private Label   brokerLabel;
    @FXML private Button  connectBtn;
    @FXML private Button  disconnectBtn;

    // Global simulator controls
    @FXML private Slider  globalIntensitySlider;
    @FXML private Label   globalIntensityLabel;
    @FXML private Button  pauseAllBtn;
    @FXML private Button  resumeAllBtn;

    // Per-athlete sliders container (built dynamically)
    @FXML private VBox    athleteSliders;

    // MQTT message log
    @FXML private TextArea  mqttLogArea;
    @FXML private Button    clearLogBtn;
    @FXML private Label     msgCountLabel;

    // Stats counters
    @FXML private Label  hrCountLabel;
    @FXML private Label  spo2CountLabel;
    @FXML private Label  gpsCountLabel;
    @FXML private Label  totalCountLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final AppContext      ctx     = AppContext.getInstance();
    private final MqttService     mqtt    = ctx.getMqtt();
    private final DatabaseService db      = ctx.getDatabase();

    private int hrCount = 0, spo2Count = 0, gpsCount = 0, totalCount = 0;
    private final List<String> logBuffer = new ArrayList<>();
    private Timeline statsTimeline;
    private boolean paused = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Connection status
        updateConnectionUI();

        // Global intensity slider
        if (globalIntensitySlider != null) {
            globalIntensitySlider.setMin(0); globalIntensitySlider.setMax(1);
            globalIntensitySlider.setValue(0.65);
            globalIntensitySlider.valueProperty().addListener((o,ov,nv) -> {
                if (globalIntensityLabel != null)
                    globalIntensityLabel.setText(String.format("%.0f%%", nv.doubleValue()*100));
            });
        }

        // Register MQTT log listener
        mqtt.addMqttLogListener(this::appendLog);

        // Per-athlete reading listener for stats
        mqtt.addReadingListener(r -> Platform.runLater(() -> {
            switch (r.getSensorType()) {
                case HR  -> hrCount++;
                case SPO2 -> spo2Count++;
                case GPS_LAT, GPS_LON, GPS_SPEED, GPS_ALT -> gpsCount++;
                default -> {}
            }
            totalCount++;
        }));

        // Build per-athlete intensity sliders
        buildAthleteSliders();

        // Update stats every 2 s
        statsTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateStats()));
        statsTimeline.setCycleCount(Timeline.INDEFINITE);
        statsTimeline.play();
    }

    // ── MQTT connection ───────────────────────────────────────────────────────

    @FXML private void handleConnect() {
        mqtt.connect();
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(Duration.seconds(1));
        pt.setOnFinished(e -> Platform.runLater(this::updateConnectionUI));
        pt.play();
    }

    @FXML private void handleDisconnect() {
        mqtt.disconnect();
        updateConnectionUI();
    }

    private void updateConnectionUI() {
        boolean live = mqtt.isConnected();
        if (mqttStatusLabel != null) {
            mqttStatusLabel.setText(live ? "CONNECTED" : "DISCONNECTED");
            mqttStatusLabel.setStyle(live ? "-fx-text-fill:#3fb950; -fx-font-weight:bold;"
                                         : "-fx-text-fill:#f85149; -fx-font-weight:bold;");
        }
        if (connectBtn    != null) connectBtn.setDisable(live);
        if (disconnectBtn != null) disconnectBtn.setDisable(!live);
    }

    // ── Athlete sliders ───────────────────────────────────────────────────────

    private void buildAthleteSliders() {
        if (athleteSliders == null || !session.hasTeam()) return;
        List<Athlete> athletes = db.findAthletesByTeam(session.getTeamId());
        athleteSliders.getChildren().clear();

        for (Athlete a : athletes) {
            String name = a.getName() != null ? a.getName() : a.getUserId();

            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13px; -fx-font-weight:bold;");
            nameLabel.setMinWidth(140);

            Slider intensitySlider = new Slider(0, 1, 0.65);
            intensitySlider.setPrefWidth(200);
            Label pctLabel = new Label("65%");
            pctLabel.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;"); pctLabel.setMinWidth(40);

            intensitySlider.valueProperty().addListener((o,ov,nv) ->
                pctLabel.setText(String.format("%.0f%%", nv.doubleValue()*100)));

            ToggleButton activeToggle = new ToggleButton("Active");
            activeToggle.setSelected(true);
            activeToggle.getStyleClass().add("btn-secondary");
            activeToggle.selectedProperty().addListener((o,ov,selected) ->
                activeToggle.setText(selected ? "Active" : "Paused"));

            HBox row = new HBox(12, nameLabel, intensitySlider, pctLabel, activeToggle);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 16, 8, 16));
            row.getStyleClass().add("glass-card-sm");
            athleteSliders.getChildren().add(row);
        }
    }

    // ── MQTT log ─────────────────────────────────────────────────────────────

    private void appendLog(String msg) {
        String line = "[" + LocalTime.now().format(TIME_FMT) + "] " + msg;
        Platform.runLater(() -> {
            if (mqttLogArea == null) return;
            logBuffer.add(line);
            if (logBuffer.size() > MAX_LOG_LINES) logBuffer.remove(0);

            // Only update text every 10 messages to reduce jank
            if (logBuffer.size() % 10 == 0 || logBuffer.size() < 10) {
                mqttLogArea.setText(String.join("\n", logBuffer));
                mqttLogArea.setScrollTop(Double.MAX_VALUE);
            }
            if (msgCountLabel != null) msgCountLabel.setText(totalCount + " msgs");
        });
    }

    @FXML private void handleClearLog() {
        logBuffer.clear();
        if (mqttLogArea != null) mqttLogArea.clear();
        hrCount = 0; spo2Count = 0; gpsCount = 0; totalCount = 0;
        updateStats();
    }

    @FXML private void handlePauseAll() {
        paused = true;
        if (pauseAllBtn  != null) pauseAllBtn.setDisable(true);
        if (resumeAllBtn != null) resumeAllBtn.setDisable(false);
    }

    @FXML private void handleResumeAll() {
        paused = false;
        if (pauseAllBtn  != null) pauseAllBtn.setDisable(false);
        if (resumeAllBtn != null) resumeAllBtn.setDisable(true);
    }

    private void updateStats() {
        if (hrCountLabel    != null) hrCountLabel.setText(String.valueOf(hrCount));
        if (spo2CountLabel  != null) spo2CountLabel.setText(String.valueOf(spo2Count));
        if (gpsCountLabel   != null) gpsCountLabel.setText(String.valueOf(gpsCount));
        if (totalCountLabel != null) totalCountLabel.setText(String.valueOf(totalCount));
    }

    public void stop() {
        if (statsTimeline != null) statsTimeline.stop();
    }
}