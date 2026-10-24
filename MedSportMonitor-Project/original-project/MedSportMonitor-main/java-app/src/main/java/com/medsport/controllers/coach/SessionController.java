package com.medsport.controllers.coach;

import com.medsport.auth.SessionManager;
import com.medsport.models.*;
import com.medsport.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class SessionController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SessionController.class);

    // Session header
    @FXML private Label   sessionStatusLabel;
    @FXML private Label   sessionTimerLabel;
    @FXML private Button  startBtn;
    @FXML private Button  stopBtn;
    @FXML private ComboBox<String> exerciseTypeCombo;
    @FXML private Slider  zoneMinSlider;
    @FXML private Slider  zoneMaxSlider;
    @FXML private Label   zoneMinLabel;
    @FXML private Label   zoneMaxLabel;
    @FXML private TextArea notesArea;

    // Athletes panel — rows built dynamically
    @FXML private VBox  athleteRows;
    @FXML private Label noTeamLabel;

    // Slider panel to send intensity to simulator
    @FXML private Slider  globalIntensitySlider;
    @FXML private Label   globalIntensityLabel;

    private final SessionManager  session  = SessionManager.getInstance();
    private final AppContext       ctx      = AppContext.getInstance();
    private final DatabaseService  db       = ctx.getDatabase();
    private final SessionService   sessSvc  = ctx.getSessions();
    private final MqttService      mqtt     = ctx.getMqtt();

    private Session activeSession;
    private Timeline sessionClockTl;
    private long     sessionStartMs;

    // Per-athlete: athleteId -> hr/spo2 label refs
    private final Map<String, Label> hrLabels   = new HashMap<>();
    private final Map<String, Label> spo2Labels = new HashMap<>();
    private final Map<String, Label> zoneLabels = new HashMap<>();
    private final Map<String, Label> alertDots  = new HashMap<>();

    private List<Athlete> athletes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) {
            if (noTeamLabel != null) { noTeamLabel.setVisible(true); noTeamLabel.setManaged(true); }
            return;
        }
        athletes = db.findAthletesByTeam(session.getTeamId());

        // Populate exercise type combo
        if (exerciseTypeCombo != null)
            exerciseTypeCombo.getItems().addAll(
                "Endurance Run","Interval Training","Strength Circuit",
                "Tempo Run","Recovery Session","Match Simulation","Field Test");

        // Zone sliders
        if (zoneMinSlider != null) {
            zoneMinSlider.setMin(1); zoneMinSlider.setMax(5); zoneMinSlider.setValue(2);
            zoneMinSlider.valueProperty().addListener((o,ov,nv) -> {
                int v = nv.intValue();
                if (zoneMinLabel != null) zoneMinLabel.setText("Z" + v);
                if (zoneMaxSlider != null && zoneMaxSlider.getValue() < v)
                    zoneMaxSlider.setValue(v);
            });
        }
        if (zoneMaxSlider != null) {
            zoneMaxSlider.setMin(1); zoneMaxSlider.setMax(5); zoneMaxSlider.setValue(4);
            zoneMaxSlider.valueProperty().addListener((o,ov,nv) -> {
                if (zoneMaxLabel != null) zoneMaxLabel.setText("Z" + nv.intValue());
            });
        }

        // Global intensity slider
        if (globalIntensitySlider != null) {
            globalIntensitySlider.setMin(0); globalIntensitySlider.setMax(1);
            globalIntensitySlider.setValue(0.6);
            globalIntensitySlider.valueProperty().addListener((o,ov,nv) -> {
                if (globalIntensityLabel != null)
                    globalIntensityLabel.setText(String.format("%.0f%%", nv.doubleValue() * 100));
            });
        }

        buildAthleteRows();

        // MQTT listener for live updates
        mqtt.addReadingListener(r -> Platform.runLater(() -> updateRow(r)));

        // Restore existing active session
        sessSvc.getActiveSession(session.getTeamId()).ifPresent(s -> {
            activeSession = s;
            sessionStartMs = s.getStartTime() != null
                ? s.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                : System.currentTimeMillis();
            setLiveUI(true);
            startClock();
        });
    }

    // ── Athlete rows ─────────────────────────────────────────────────────────

    private void buildAthleteRows() {
        if (athleteRows == null) return;
        athleteRows.getChildren().clear();
        for (Athlete a : athletes) {
            HBox row = buildAthleteRow(a);
            athleteRows.getChildren().add(row);
        }
    }

    private HBox buildAthleteRow(Athlete a) {
        // Alert dot
        Label dot = new Label(" ");
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color:transparent; -fx-background-radius:50%;");

        // Name
        Label name = new Label(a.getName() != null ? a.getName() : "—");
        name.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13px; -fx-font-weight:bold;");
        name.setMinWidth(140);

        // HR
        Label hr = new Label("HR: —");
        hr.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");
        hr.setMinWidth(90);

        // SpO2
        Label spo2 = new Label("SpO2: —");
        spo2.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");
        spo2.setMinWidth(90);

        // Zone
        Label zone = new Label("—");
        zone.setStyle("-fx-font-size:12px;");
        zone.setMinWidth(60);

        hrLabels.put(a.getUserId(), hr);
        spo2Labels.put(a.getUserId(), spo2);
        zoneLabels.put(a.getUserId(), zone);
        alertDots.put(a.getUserId(), dot);

        // Per-athlete intensity slider
        Slider intensity = new Slider(0, 1, 0.6);
        intensity.setPrefWidth(120);
        Label intensityLbl = new Label("60%");
        intensityLbl.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11px;");
        intensityLbl.setMinWidth(36);
        intensity.valueProperty().addListener((o, ov, nv) ->
            intensityLbl.setText(String.format("%.0f%%", nv.doubleValue() * 100)));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, dot, name, hr, spo2, zone, spacer, intensity, intensityLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("glass-card-sm");
        row.setStyle("-fx-padding:10 16; -fx-background-radius:8; -fx-margin: 0 0 4 0;");
        return row;
    }

    private void updateRow(SensorReading r) {
        String id = r.getAthleteId();
        switch (r.getSensorType()) {
            case HR -> {
                int hr = (int) r.getValue();
                Label l = hrLabels.get(id);
                if (l == null) return;
                int z = hrToZone(hr);
                String[] zColors = {"","#4CAF50","#8BC34A","#FFC107","#FF9800","#F44336"};
                l.setText("HR: " + hr);
                l.setStyle("-fx-text-fill:" + (z >= 1 && z <= 5 ? zColors[z] : "#e6edf3") + "; -fx-font-size:12px; -fx-font-weight:bold;");
                Label zl = zoneLabels.get(id);
                if (zl != null && z >= 1 && z <= 5) {
                    zl.setText("Z" + z);
                    zl.setStyle("-fx-text-fill:" + zColors[z] + "; -fx-font-size:12px; -fx-font-weight:bold;");
                }
                // Flash alert dot if above threshold
                double threshold = session.getCurrentTeam() != null
                    ? session.getCurrentTeam().getHrAlertThreshold() : 95;
                int hrMax = 190;
                Label dot = alertDots.get(id);
                if (dot != null) {
                    if (hr > hrMax * threshold / 100.0)
                        dot.setStyle("-fx-background-color:#f85149; -fx-background-radius:50%;");
                    else
                        dot.setStyle("-fx-background-color:transparent; -fx-background-radius:50%;");
                }
            }
            case SPO2 -> {
                Label l = spo2Labels.get(id);
                if (l == null) return;
                String c = r.getValue() <= 93 ? "#f85149" : "#8b949e";
                l.setText(String.format("SpO2: %.1f%%", r.getValue()));
                l.setStyle("-fx-text-fill:" + c + "; -fx-font-size:12px;");
            }
            default -> {}
        }
    }

    // ── Session start / stop ─────────────────────────────────────────────────

    @FXML
    private void handleStart() {
        Session s = new Session();
        s.setTeamId(session.getTeamId());
        s.setCoachId(session.getUserId());
        s.setExerciseType(exerciseTypeCombo != null && exerciseTypeCombo.getValue() != null
            ? exerciseTypeCombo.getValue() : "Training");
        s.setTargetZoneMin(zoneMinSlider != null ? (int) zoneMinSlider.getValue() : 2);
        s.setTargetZoneMax(zoneMaxSlider != null ? (int) zoneMaxSlider.getValue() : 4);
        s.setNotes(notesArea != null ? notesArea.getText() : "");

        Thread t = new Thread(() -> {
            try {
                Session started = sessSvc.startSession(s);
                activeSession = started;
                sessionStartMs = System.currentTimeMillis();
                Platform.runLater(() -> { setLiveUI(true); startClock(); });
                LOG.info("Session started: {}", started.getId());
            } catch (Exception e) {
                LOG.error("Start session error", e);
            }
        }, "start-session");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleStop() {
        if (activeSession == null) return;
        Thread t = new Thread(() -> {
            try {
                sessSvc.endSession(activeSession.getId());
                activeSession = null;
                Platform.runLater(() -> { setLiveUI(false); stopClock(); });
            } catch (Exception e) {
                LOG.error("Stop session error", e);
            }
        }, "stop-session");
        t.setDaemon(true);
        t.start();
    }

    private void setLiveUI(boolean live) {
        if (startBtn != null) startBtn.setDisable(live);
        if (stopBtn  != null) stopBtn.setDisable(!live);
        if (sessionStatusLabel != null) {
            sessionStatusLabel.setText(live ? "SESSION LIVE" : "No Active Session");
            sessionStatusLabel.setStyle(live
                ? "-fx-text-fill:#3fb950; -fx-font-weight:bold;"
                : "-fx-text-fill:#484f58;");
        }
    }

    private void startClock() {
        if (sessionClockTl != null) sessionClockTl.stop();
        sessionClockTl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long elapsed = (System.currentTimeMillis() - sessionStartMs) / 1000;
            long h = elapsed / 3600, m = (elapsed % 3600) / 60, s = elapsed % 60;
            String txt = h > 0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%02d:%02d",m,s);
            if (sessionTimerLabel != null) sessionTimerLabel.setText(txt);
        }));
        sessionClockTl.setCycleCount(Timeline.INDEFINITE);
        sessionClockTl.play();
    }
    private void stopClock() {
        if (sessionClockTl != null) sessionClockTl.stop();
        if (sessionTimerLabel != null) sessionTimerLabel.setText("00:00");
    }

    private int hrToZone(int hr) {
        double p = hr / 190.0 * 100;
        if (p < 60) return 1; if (p < 70) return 2;
        if (p < 80) return 3; if (p < 90) return 4; return 5;
    }
    public void stop() {
        stopClock();
        mqtt.removeReadingListener(this::updateRow);
    }
}