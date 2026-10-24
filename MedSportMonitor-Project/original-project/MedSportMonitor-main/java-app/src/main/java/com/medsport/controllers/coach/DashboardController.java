package com.medsport.controllers.coach;

import com.medsport.auth.SessionManager;
import com.medsport.models.Alert;
import com.medsport.models.Athlete;
import com.medsport.models.SensorReading;
import com.medsport.models.Session;
import com.medsport.models.Team;
import com.medsport.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

    // Team overview
    @FXML private Label teamNameLabel;
    @FXML private Label teamCodeLabel;
    @FXML private Label athleteCountLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Label mqttStatusLabel;

    // Athlete grid — each card added dynamically
    @FXML private FlowPane athleteGrid;

    // Alert feed
    @FXML private VBox alertFeed;
    @FXML private Label noAlertsLabel;

    // Session quick-action
    @FXML private Button startSessionBtn;
    @FXML private Button stopSessionBtn;

    private final SessionManager session = SessionManager.getInstance();
    private final AppContext      ctx     = AppContext.getInstance();
    private final DatabaseService db      = ctx.getDatabase();
    private final MqttService     mqtt    = ctx.getMqtt();
    private final SessionService  sessSvc = ctx.getSessions();
    private final AlertService    alertSvc= ctx.getAlerts();

    // Per-athlete card references: athleteId -> label nodes
    private final Map<String, Label> hrLabels   = new HashMap<>();
    private final Map<String, Label> spo2Labels = new HashMap<>();
    private final Map<String, Label> zoneLabels = new HashMap<>();

    private List<Athlete> athletes = new ArrayList<>();
    private Timeline refreshTimeline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        var team = session.getCurrentTeam();
        if (teamNameLabel != null) teamNameLabel.setText(team.getName());
        if (teamCodeLabel != null) teamCodeLabel.setText("Code: " + team.getJoinCode());

        athletes = db.findAthletesByTeam(session.getTeamId());
        if (athleteCountLabel != null) athleteCountLabel.setText(athletes.size() + " athletes");

        buildAthleteGrid();
        refreshAlerts();
        refreshSessionStatus();

        // MQTT listener: update athlete card on every reading
        mqtt.addReadingListener(r -> Platform.runLater(() -> updateAthleteCard(r)));

        // Refresh alerts + session every 15 s
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            refreshAlerts();
            refreshSessionStatus();
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        if (mqttStatusLabel != null) {
            mqttStatusLabel.setText(mqtt.isConnected() ? "LIVE" : "DEMO");
            mqttStatusLabel.setStyle(mqtt.isConnected()
                ? "-fx-text-fill:#3fb950;" : "-fx-text-fill:#d29922;");
        }
    }

    // ── Athlete grid ─────────────────────────────────────────────────────────

    private void buildAthleteGrid() {
        if (athleteGrid == null) return;
        athleteGrid.getChildren().clear();
        for (Athlete a : athletes) {
            VBox card = buildAthleteCard(a);
            athleteGrid.getChildren().add(card);
        }
    }

    private VBox buildAthleteCard(Athlete a) {
        // Avatar circle
        Circle avatar = new Circle(22);
        avatar.setFill(Color.web("#0A84FF", 0.2));
        avatar.setStroke(Color.web("#0A84FF", 0.6));
        avatar.setStrokeWidth(1.5);

        Label avatarInitial = new Label(a.getName() != null && !a.getName().isEmpty()
            ? String.valueOf(a.getName().charAt(0)).toUpperCase() : "?");
        avatarInitial.setStyle("-fx-text-fill:-theme-accent; -fx-font-weight:bold; -fx-font-size:16px;");

        StackPane avatarStack = new StackPane(avatar, avatarInitial);

        Label nameLabel = new Label(a.getName() != null ? a.getName() : "Athlete");
        nameLabel.setStyle("-fx-text-fill:#e6edf3; -fx-font-weight:bold; -fx-font-size:13px;");
        nameLabel.setMaxWidth(140);
        nameLabel.setEllipsisString("…");

        Label hrLabel = new Label("HR: —");
        hrLabel.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");
        Label spo2Label = new Label("SpO2: —");
        spo2Label.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");
        Label zoneLabel = new Label("");
        zoneLabel.setStyle("-fx-font-size:11px;");

        hrLabels.put(a.getUserId(), hrLabel);
        spo2Labels.put(a.getUserId(), spo2Label);
        zoneLabels.put(a.getUserId(), zoneLabel);

        VBox card = new VBox(6, avatarStack, nameLabel, hrLabel, spo2Label, zoneLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setPrefWidth(160);
        card.setPrefHeight(150);
        card.getStyleClass().add("card");
        card.setStyle(card.getStyle() != null ? card.getStyle() : "" +
            "; -fx-cursor:hand;");
        return card;
    }

    private void updateAthleteCard(SensorReading r) {
        String id = r.getAthleteId();
        switch (r.getSensorType()) {
            case HR -> {
                int hr = (int) r.getValue();
                Label l = hrLabels.get(id);
                if (l != null) {
                    l.setText("HR: " + hr + " bpm");
                    String color = hr > 170 ? "#f85149" : hr > 150 ? "#d29922" : "#3fb950";
                    l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:bold;");
                }
                Label zl = zoneLabels.get(id);
                if (zl != null) {
                    int z = hrToZone(hr);
                    String[] zCss = {"","#4CAF50","#8BC34A","#FFC107","#FF9800","#F44336"};
                    zl.setText("Zone " + z);
                    if (z >= 1 && z <= 5) zl.setStyle("-fx-text-fill:" + zCss[z] + "; -fx-font-size:11px; -fx-font-weight:bold;");
                }
            }
            case SPO2 -> {
                Label l = spo2Labels.get(id);
                if (l != null) {
                    String color = r.getValue() <= 93 ? "#f85149" : "#8b949e";
                    l.setText(String.format("SpO2: %.1f%%", r.getValue()));
                    l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px;");
                }
            }
            default -> {}
        }
    }

    // ── Alerts ───────────────────────────────────────────────────────────────

    private void refreshAlerts() {
        if (!session.hasTeam() || alertFeed == null) return;
        List<Alert> alerts = db.findActiveAlerts(session.getTeamId());
        Platform.runLater(() -> {
            alertFeed.getChildren().clear();
            if (noAlertsLabel != null) {
                noAlertsLabel.setVisible(alerts.isEmpty());
                noAlertsLabel.setManaged(alerts.isEmpty());
            }
            // Show latest 5
            alerts.stream().limit(5).forEach(a -> alertFeed.getChildren().add(buildAlertRow(a)));
        });
    }

    private HBox buildAlertRow(Alert a) {
        boolean critical = a.getSeverity() == Alert.Severity.CRITICAL;
        Label dot = new Label();
        dot.getStyleClass().add(critical ? "alert-dot-critical" : "alert-dot-warning");
        dot.setPrefSize(8, 8);

        Label msg = new Label(a.getFormattedMessage() != null ? a.getFormattedMessage() : a.getParameter() + " alert");
        msg.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12px;");
        msg.setWrapText(true);
        HBox.setHgrow(msg, Priority.ALWAYS);

        Label badge = new Label(a.getSeverity().name());
        badge.getStyleClass().add(critical ? "badge-critical" : "badge-warning");

        HBox row = new HBox(8, dot, msg, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add(critical ? "alert-row-critical" : "alert-row-warning");
        row.setStyle("-fx-background-radius:6;");
        return row;
    }

    // ── Session quick-actions ────────────────────────────────────────────────

    private void refreshSessionStatus() {
        if (!session.hasTeam()) return;
        Optional<Session> active = sessSvc.getActiveSession(session.getTeamId());
        Platform.runLater(() -> {
            boolean hasActive = active.isPresent();
            if (sessionStatusLabel != null) {
                sessionStatusLabel.setText(hasActive ? "Session LIVE" : "No active session");
                sessionStatusLabel.setStyle(hasActive
                    ? "-fx-text-fill:#3fb950; -fx-font-weight:bold;"
                    : "-fx-text-fill:#484f58;");
            }
            if (startSessionBtn != null) startSessionBtn.setDisable(hasActive);
            if (stopSessionBtn  != null) stopSessionBtn.setDisable(!hasActive);
        });
    }

    @FXML private void handleStartSession() {
        // Delegate to session controller — just navigate there
    }

    @FXML private void handleStopSession() {
        if (!session.hasTeam()) return;
        Thread t = new Thread(() -> {
            try {
                sessSvc.getActiveSession(session.getTeamId())
                    .ifPresent(s -> {
                        try { sessSvc.endSession(s.getId()); } catch (Exception ignored) {}
                    });
                Platform.runLater(this::refreshSessionStatus);
            } catch (Exception e) { LOG.error("Stop session error", e); }
        }, "stop-session");
        t.setDaemon(true);
        t.start();
    }

    private int hrToZone(int hr) {
        double pct = hr / 190.0 * 100;
        if (pct < 60) return 1; if (pct < 70) return 2;
        if (pct < 80) return 3; if (pct < 90) return 4; return 5;
    }

    public void stop() {
        if (refreshTimeline != null) refreshTimeline.stop();
        mqtt.removeReadingListener(this::updateAthleteCard);
    }
}