package com.medsport.controllers.medical;

import com.medsport.auth.SessionManager;
import com.medsport.models.Alert;
import com.medsport.models.Athlete;
import com.medsport.models.MedicalRestriction;
import com.medsport.models.SensorReading;
import com.medsport.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class DashboardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label   teamNameLabel;
    @FXML private Label   mqttStatusLabel;
    @FXML private Label   alertCountLabel;
    @FXML private VBox    criticalAlertFeed;
    @FXML private FlowPane athleteStatusGrid;
    @FXML private VBox    restrictionFeed;
    @FXML private Label   lastUpdateLabel;

    private final SessionManager session  = SessionManager.getInstance();
    private final AppContext      ctx      = AppContext.getInstance();
    private final DatabaseService db       = ctx.getDatabase();
    private final MqttService     mqtt     = ctx.getMqtt();

    private final Map<String, Label> hrLabels   = new HashMap<>();
    private final Map<String, Label> spo2Labels = new HashMap<>();
    private final Map<String, Label> statusDots = new HashMap<>();

    private Timeline refreshTimeline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;

        if (teamNameLabel != null) teamNameLabel.setText(session.getCurrentTeam().getName());
        if (mqttStatusLabel != null) {
            mqttStatusLabel.setText(mqtt.isConnected() ? "● LIVE" : "● DEMO");
            mqttStatusLabel.setStyle(mqtt.isConnected()
                ? "-fx-text-fill:#3fb950; -fx-font-weight:bold;"
                : "-fx-text-fill:#d29922; -fx-font-weight:bold;");
        }

        buildAthleteGrid();
        refreshAlerts();
        refreshRestrictions();

        mqtt.addReadingListener(r -> Platform.runLater(() -> updateAthleteStatus(r)));

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            refreshAlerts();
            refreshRestrictions();
            if (lastUpdateLabel != null)
                lastUpdateLabel.setText("Updated " + java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    // ── Athlete status grid ──────────────────────────────────────────────────

    private void buildAthleteGrid() {
        if (athleteStatusGrid == null) return;
        List<Athlete> athletes = db.findAthletesByTeam(session.getTeamId());
        athleteStatusGrid.getChildren().clear();

        for (Athlete a : athletes) {
            String id   = a.getUserId();
            String name = a.getName() != null ? a.getName() : id;

            Circle dot = new Circle(6);
            dot.setFill(Color.web("#8b949e"));

            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12px; -fx-font-weight:bold;");
            nameLbl.setMaxWidth(110); nameLbl.setEllipsisString("…");

            Label hrLbl = new Label("HR —");
            hrLbl.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11px;");
            Label spo2Lbl = new Label("SpO₂ —");
            spo2Lbl.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11px;");

            hrLabels.put(id, hrLbl);
            spo2Labels.put(id, spo2Lbl);

            // Red dot used as status indicator — we store by label trick
            Label dotLbl = new Label("●");
            dotLbl.setStyle("-fx-text-fill:#8b949e; -fx-font-size:10px;");
            statusDots.put(id, dotLbl);

            VBox card = new VBox(4, new HBox(6, dotLbl, nameLbl), hrLbl, spo2Lbl);
            card.setAlignment(Pos.TOP_LEFT);
            card.setPadding(new Insets(12));
            card.setPrefWidth(150);
            card.getStyleClass().add("glass-card-sm");
            athleteStatusGrid.getChildren().add(card);
        }
    }

    private void updateAthleteStatus(SensorReading r) {
        String id = r.getAthleteId();
        switch (r.getSensorType()) {
            case HR -> {
                int hr = (int) r.getValue();
                Label l = hrLabels.get(id);
                if (l != null) {
                    l.setText("HR " + hr + " bpm");
                    boolean alarm = hr > 175;
                    l.setStyle("-fx-text-fill:" + (alarm ? "#f85149" : "#e6edf3") + "; -fx-font-size:11px;");
                    Label dot = statusDots.get(id);
                    if (dot != null)
                        dot.setStyle("-fx-text-fill:" + (alarm ? "#f85149" : "#3fb950") + "; -fx-font-size:10px;");
                }
            }
            case SPO2 -> {
                Label l = spo2Labels.get(id);
                if (l != null) {
                    boolean low = r.getValue() < 94;
                    l.setText(String.format("SpO₂ %.1f%%", r.getValue()));
                    l.setStyle("-fx-text-fill:" + (low ? "#f85149" : "#8b949e") + "; -fx-font-size:11px;");
                }
            }
            default -> {}
        }
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    private void refreshAlerts() {
        if (!session.hasTeam() || criticalAlertFeed == null) return;
        List<Alert> alerts = db.findActiveAlerts(session.getTeamId());
        Platform.runLater(() -> {
            criticalAlertFeed.getChildren().clear();
            if (alertCountLabel != null) alertCountLabel.setText(alerts.size() + " active");
            alerts.stream().limit(8).forEach(a -> criticalAlertFeed.getChildren().add(buildAlertRow(a)));
        });
    }

    private HBox buildAlertRow(Alert a) {
        boolean critical = a.getSeverity() == Alert.Severity.CRITICAL;
        Label icon = new Label(critical ? "⚠" : "•");
        icon.setStyle("-fx-text-fill:" + (critical ? "#f85149" : "#d29922") + "; -fx-font-size:14px;");

        Label msg = new Label(a.getFormattedMessage());
        msg.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12px;");
        msg.setWrapText(true);
        HBox.setHgrow(msg, Priority.ALWAYS);

        Button ackBtn = new Button("ACK");
        ackBtn.getStyleClass().add("btn-ghost");
        ackBtn.setStyle("-fx-padding:2 8 2 8; -fx-font-size:11px;");
        ackBtn.setOnAction(e -> {
            try {
                db.acknowledgeAlert(a.getId(), session.getUserId());
            } catch (Exception ex) { LOG.error("Ack alert error", ex); }
            refreshAlerts();
        });

        HBox row = new HBox(8, icon, msg, ackBtn);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:" + (critical ? "rgba(248,81,73,0.08)" : "rgba(210,153,34,0.08)")
            + "; -fx-background-radius:6;");
        return row;
    }

    // ── Restrictions ──────────────────────────────────────────────────────────

    private void refreshRestrictions() {
        if (!session.hasTeam() || restrictionFeed == null) return;
        List<MedicalRestriction> list = db.findAllRestrictionsForTeam(session.getTeamId());
        List<MedicalRestriction> active = list.stream().filter(MedicalRestriction::isActive).toList();
        Platform.runLater(() -> {
            restrictionFeed.getChildren().clear();
            if (active.isEmpty()) {
                Label empty = new Label("No active restrictions");
                empty.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");
                restrictionFeed.getChildren().add(empty);
                return;
            }
            active.forEach(r -> {
                Label lbl = new Label("🔴 " + (r.getAthleteName() != null ? r.getAthleteName() : r.getAthleteId())
                    + " — " + r.getPublicSummary());
                lbl.setStyle("-fx-text-fill:#f85149; -fx-font-size:12px;");
                lbl.setWrapText(true);
                restrictionFeed.getChildren().add(lbl);
            });
        });
    }

    public void stop() { if (refreshTimeline != null) refreshTimeline.stop(); }
}