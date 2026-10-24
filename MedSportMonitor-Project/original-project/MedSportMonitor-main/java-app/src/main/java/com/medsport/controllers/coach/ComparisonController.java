package com.medsport.controllers.coach;

import com.medsport.auth.SessionManager;
import com.medsport.models.*;
import com.medsport.services.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class ComparisonController implements Initializable {

    @FXML private ComboBox<String> athlete1Combo;
    @FXML private ComboBox<String> athlete2Combo;
    @FXML private Canvas           radarCanvas;
    @FXML private Label            a1NameLabel, a2NameLabel;
    @FXML private Label            a1HrLabel,   a2HrLabel;
    @FXML private Label            a1Spo2Label, a2Spo2Label;
    @FXML private Label            a1ZoneLabel, a2ZoneLabel;
    @FXML private Label            a1LoadLabel, a2LoadLabel;
    @FXML private Button           compareBtn;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();
    private final MqttService     mqtt   = AppContext.getInstance().getMqtt();

    private List<Athlete> athletes = new ArrayList<>();
    private final String[] AXES = {"Endurance","Speed","Recovery","Consistency","Load","Intensity"};

    // Simulated radar values per athlete (Phase H: replace with analytics)
    private final Map<String, double[]> radarData = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());

        List<String> names = athletes.stream()
            .map(a -> a.getName() != null ? a.getName() : a.getUserId())
            .toList();

        if (athlete1Combo != null) { athlete1Combo.getItems().addAll(names); if (!names.isEmpty()) athlete1Combo.setValue(names.get(0)); }
        if (athlete2Combo != null) { athlete2Combo.getItems().addAll(names); if (names.size() > 1) athlete2Combo.setValue(names.get(1)); }

        // Seed radar data with varied placeholders per athlete
        Random rng = new Random(42);
        for (Athlete a : athletes) {
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) v[i] = 0.4 + rng.nextDouble() * 0.55;
            radarData.put(a.getName() != null ? a.getName() : a.getUserId(), v);
        }

        // Wire MQTT for live HR/SpO2 updates
        mqtt.addReadingListener(r -> {
            javafx.application.Platform.runLater(() -> updateLive(r));
        });

        if (!names.isEmpty()) drawComparison();
    }

    @FXML private void handleCompare() { drawComparison(); }

    private void drawComparison() {
        if (radarCanvas == null) return;
        String n1 = athlete1Combo != null ? athlete1Combo.getValue() : null;
        String n2 = athlete2Combo != null ? athlete2Combo.getValue() : null;
        if (n1 == null) return;

        double[] v1 = radarData.getOrDefault(n1, new double[]{0.7,0.6,0.8,0.65,0.7,0.55});
        double[] v2 = n2 != null ? radarData.getOrDefault(n2, new double[]{0.5,0.75,0.6,0.8,0.5,0.7}) : null;

        GraphicsContext gc = radarCanvas.getGraphicsContext2D();
        double w = radarCanvas.getWidth(), h = radarCanvas.getHeight();
        double cx = w/2, cy = h/2, r = Math.min(cx,cy)*0.72;
        int n = AXES.length;

        gc.clearRect(0,0,w,h);
        gc.setFill(Color.web("#0d1117")); gc.fillRect(0,0,w,h);

        // Grid rings
        gc.setStroke(Color.web("#21262d",0.7)); gc.setLineWidth(0.8);
        for (int ring = 1; ring <= 5; ring++) {
            double rr = r*ring/5.0;
            gc.beginPath();
            for (int i = 0; i <= n; i++) {
                double a = Math.PI*2*i/n - Math.PI/2;
                double x = cx + rr*Math.cos(a), y = cy + rr*Math.sin(a);
                if (i==0) gc.moveTo(x,y); else gc.lineTo(x,y);
            }
            gc.closePath(); gc.stroke();
        }
        // Spokes + labels
        gc.setFill(Color.web("#8b949e")); gc.setFont(javafx.scene.text.Font.font(11));
        for (int i = 0; i < n; i++) {
            double a = Math.PI*2*i/n - Math.PI/2;
            gc.strokeLine(cx, cy, cx+r*Math.cos(a), cy+r*Math.sin(a));
            double lx = cx+(r+22)*Math.cos(a), ly = cy+(r+22)*Math.sin(a);
            gc.fillText(AXES[i], lx - AXES[i].length()*3, ly+4);
        }

        // Athlete 1 polygon (blue)
        drawPolygon(gc, cx, cy, r, n, v1, Color.web("#0A84FF", 0.25), Color.web("#0A84FF", 0.9));

        // Athlete 2 polygon (green)
        if (v2 != null)
            drawPolygon(gc, cx, cy, r, n, v2, Color.web("#00C853", 0.20), Color.web("#00C853", 0.9));

        // Legend
        gc.setFill(Color.web("#0A84FF")); gc.fillRect(20, h-36, 14, 14);
        gc.setFill(Color.web("#e6edf3")); gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText(n1 != null ? n1 : "Athlete 1", 40, h-25);
        if (n2 != null) {
            gc.setFill(Color.web("#00C853")); gc.fillRect(20, h-18, 14, 14);
            gc.setFill(Color.web("#e6edf3")); gc.fillText(n2, 40, h-7);
        }

        // Update stat labels
        if (a1NameLabel != null) a1NameLabel.setText(n1 != null ? n1 : "—");
        if (a2NameLabel != null) a2NameLabel.setText(n2 != null ? n2 : "—");
    }

    private void drawPolygon(GraphicsContext gc, double cx, double cy, double r,
                             int n, double[] vals, Color fill, Color stroke) {
        gc.setFill(fill); gc.setStroke(stroke); gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= n; i++) {
            double a = Math.PI*2*(i%n)/n - Math.PI/2;
            double v = vals[i%n];
            double x = cx+r*v*Math.cos(a), y = cy+r*v*Math.sin(a);
            if (i==0) gc.moveTo(x,y); else gc.lineTo(x,y);
        }
        gc.closePath(); gc.fill(); gc.stroke();
        // Dots
        gc.setFill(stroke);
        for (int i = 0; i < n; i++) {
            double a = Math.PI*2*i/n - Math.PI/2;
            double x = cx+r*vals[i]*Math.cos(a), y = cy+r*vals[i]*Math.sin(a);
            gc.fillOval(x-4, y-4, 8, 8);
        }
    }

    private void updateLive(SensorReading r) {
        if (r.getSensorType() == SensorReading.SensorType.HR) {
            String id = r.getAthleteId();
            athletes.stream().filter(a -> a.getUserId().equals(id)).findFirst().ifPresent(a -> {
                String name = a.getName();
                if (athlete1Combo != null && name.equals(athlete1Combo.getValue()) && a1HrLabel != null)
                    a1HrLabel.setText("HR: " + (int)r.getValue() + " bpm");
                if (athlete2Combo != null && name.equals(athlete2Combo.getValue()) && a2HrLabel != null)
                    a2HrLabel.setText("HR: " + (int)r.getValue() + " bpm");
            });
        }
        if (r.getSensorType() == SensorReading.SensorType.SPO2) {
            String id = r.getAthleteId();
            athletes.stream().filter(a -> a.getUserId().equals(id)).findFirst().ifPresent(a -> {
                String name = a.getName();
                if (athlete1Combo != null && name.equals(athlete1Combo.getValue()) && a1Spo2Label != null)
                    a1Spo2Label.setText(String.format("SpO2: %.1f%%", r.getValue()));
                if (athlete2Combo != null && name.equals(athlete2Combo.getValue()) && a2Spo2Label != null)
                    a2Spo2Label.setText(String.format("SpO2: %.1f%%", r.getValue()));
            });
        }
    }
    public void stop() { mqtt.removeReadingListener(this::updateLive); }
}