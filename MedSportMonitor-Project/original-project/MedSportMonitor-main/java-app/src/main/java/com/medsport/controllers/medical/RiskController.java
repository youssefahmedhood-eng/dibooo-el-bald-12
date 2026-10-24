package com.medsport.controllers.medical;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.SensorReading;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import com.medsport.services.MqttService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class RiskController implements Initializable {

    @FXML private VBox     riskCards;
    @FXML private Canvas   riskRadarCanvas;
    @FXML private Label    teamRiskLabel;
    @FXML private Label    highRiskCountLabel;
    @FXML private Label    medRiskCountLabel;
    @FXML private Label    lowRiskCountLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();
    private final MqttService     mqtt   = AppContext.getInstance().getMqtt();

    // Live HR per athlete for risk calc
    private final Map<String, Double> liveHr   = new HashMap<>();
    private final Map<String, Double> liveSpo2  = new HashMap<>();

    private List<Athlete> athletes = new ArrayList<>();

    private static final String[] RISK_AXES = {"Cardiac","Resp.","Load","Recovery","Hydration","Overtraining"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());
        buildRiskCards();
        drawTeamRiskRadar();

        mqtt.addReadingListener(r -> {
            switch (r.getSensorType()) {
                case HR   -> liveHr.put(r.getAthleteId(), r.getValue());
                case SPO2 -> liveSpo2.put(r.getAthleteId(), r.getValue());
                default   -> {}
            }
            Platform.runLater(this::updateRiskCards);
        });
    }

    private void buildRiskCards() {
        if (riskCards == null) return;
        riskCards.getChildren().clear();
        int high = 0, med = 0, low = 0;
        for (Athlete a : athletes) {
            double risk = computeRiskScore(a);
            if (risk >= 0.65) high++; else if (risk >= 0.35) med++; else low++;
            riskCards.getChildren().add(buildAthleteRiskRow(a, risk));
        }
        if (highRiskCountLabel != null) highRiskCountLabel.setText(String.valueOf(high));
        if (medRiskCountLabel  != null) medRiskCountLabel.setText(String.valueOf(med));
        if (lowRiskCountLabel  != null) lowRiskCountLabel.setText(String.valueOf(low));
        if (teamRiskLabel != null) {
            double avg = athletes.isEmpty() ? 0 : athletes.stream()
                .mapToDouble(this::computeRiskScore).average().orElse(0);
            teamRiskLabel.setText(avg >= 0.65 ? "HIGH" : avg >= 0.35 ? "MODERATE" : "LOW");
            teamRiskLabel.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:"
                + (avg >= 0.65 ? "#f85149" : avg >= 0.35 ? "#d29922" : "#3fb950") + ";");
        }
    }

    private void updateRiskCards() { buildRiskCards(); }

    private double computeRiskScore(Athlete a) {
        double score = 0;
        Double hr   = liveHr.get(a.getUserId());
        Double spo2 = liveSpo2.get(a.getUserId());
        if (hr != null)   { double hrMax = a.calculateHrMax(); score += hr / hrMax; }
        if (spo2 != null) { score += Math.max(0, (95 - spo2) / 10.0); }
        // Seed stable component from athlete ID hash for demo
        Random rng = new Random(a.getUserId().hashCode());
        score += rng.nextDouble() * 0.4;
        return Math.min(1.0, score / (hr != null ? 2.4 : 1.4));
    }

    private HBox buildAthleteRiskRow(Athlete a, double risk) {
        String name = a.getName() != null ? a.getName() : a.getUserId();
        boolean high = risk >= 0.65;
        boolean med  = risk >= 0.35 && !high;
        String color = high ? "#f85149" : med ? "#d29922" : "#3fb950";
        String label = high ? "HIGH" : med ? "MODERATE" : "LOW";

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13px; -fx-font-weight:bold;");
        nameLbl.setMinWidth(150);

        ProgressBar bar = new ProgressBar(risk);
        bar.setPrefWidth(200);
        bar.setStyle("-fx-accent:" + color + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label riskLbl = new Label(String.format("%.0f%% %s", risk * 100, label));
        riskLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:bold;");
        riskLbl.setMinWidth(110);

        HBox row = new HBox(16, nameLbl, bar, riskLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("glass-card-sm");
        return row;
    }

    private void drawTeamRiskRadar() {
        if (riskRadarCanvas == null) return;
        double cx = riskRadarCanvas.getWidth() / 2, cy = riskRadarCanvas.getHeight() / 2;
        double r = Math.min(cx, cy) * 0.70;
        int n = RISK_AXES.length;
        Random rng = new Random(42);
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) vals[i] = 0.2 + rng.nextDouble() * 0.65;

        GraphicsContext gc = riskRadarCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, riskRadarCanvas.getWidth(), riskRadarCanvas.getHeight());
        gc.setFill(Color.web("#0d1117")); gc.fillRect(0, 0, riskRadarCanvas.getWidth(), riskRadarCanvas.getHeight());

        gc.setStroke(Color.web("#21262d", 0.7)); gc.setLineWidth(0.8);
        for (int ring = 1; ring <= 5; ring++) {
            double rr = r * ring / 5.0;
            gc.beginPath();
            for (int i = 0; i <= n; i++) {
                double a = Math.PI * 2 * i / n - Math.PI / 2;
                if (i == 0) gc.moveTo(cx + rr * Math.cos(a), cy + rr * Math.sin(a));
                else        gc.lineTo(cx + rr * Math.cos(a), cy + rr * Math.sin(a));
            }
            gc.closePath(); gc.stroke();
        }
        gc.setFill(Color.web("#8b949e")); gc.setFont(javafx.scene.text.Font.font(11));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n - Math.PI / 2;
            gc.strokeLine(cx, cy, cx + r * Math.cos(a), cy + r * Math.sin(a));
            gc.fillText(RISK_AXES[i], cx + (r + 22) * Math.cos(a) - RISK_AXES[i].length() * 3,
                cy + (r + 22) * Math.sin(a) + 4);
        }
        gc.setFill(Color.web("#D32F2F", 0.25)); gc.setStroke(Color.web("#D32F2F")); gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * (i % n) / n - Math.PI / 2;
            if (i == 0) gc.moveTo(cx + r * vals[i % n] * Math.cos(a), cy + r * vals[i % n] * Math.sin(a));
            else        gc.lineTo(cx + r * vals[i % n] * Math.cos(a), cy + r * vals[i % n] * Math.sin(a));
        }
        gc.closePath(); gc.fill(); gc.stroke();
    }
}