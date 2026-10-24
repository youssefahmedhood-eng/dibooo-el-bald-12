package com.medsport.controllers.medical;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.SoapNote;
import com.medsport.services.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportsController implements Initializable {

    @FXML private ComboBox<String>  athleteCombo;
    @FXML private ComboBox<String>  reportTypeCombo;
    @FXML private DatePicker        fromDatePicker;
    @FXML private DatePicker        toDatePicker;
    @FXML private Button            generateBtn;
    @FXML private ProgressBar       generateProgress;
    @FXML private Label             statusLabel;
    @FXML private VBox              recentReportsList;
    @FXML private Label             apiStatusLabel;

    private final SessionManager       session   = SessionManager.getInstance();
    private final DatabaseService       db        = AppContext.getInstance().getDatabase();
    private final AnalyticsRestClient   analytics = AppContext.getInstance().getAnalytics();

    private List<Athlete> athletes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());

        if (athleteCombo != null) {
            athleteCombo.getItems().add("Whole Team");
            athletes.forEach(a -> athleteCombo.getItems().add(a.getName() != null ? a.getName() : a.getUserId()));
            athleteCombo.setValue("Whole Team");
        }
        if (reportTypeCombo != null) {
            reportTypeCombo.getItems().addAll(
                "Clinical Summary", "Training Load Report",
                "SOAP Notes Export", "Alert History", "Risk Analysis Report");
            reportTypeCombo.setValue("Clinical Summary");
        }
        LocalDate today = LocalDate.now();
        if (fromDatePicker != null) fromDatePicker.setValue(today.minusMonths(1));
        if (toDatePicker   != null) toDatePicker.setValue(today);
        if (generateProgress != null) { generateProgress.setVisible(false); generateProgress.setManaged(false); }
        if (statusLabel      != null) { statusLabel.setVisible(false); statusLabel.setManaged(false); }

        checkApiHealth();
    }

    private void checkApiHealth() {
        analytics.checkHealthAsync().thenAccept(ok -> Platform.runLater(() -> {
            if (apiStatusLabel != null) {
                apiStatusLabel.setText(ok ? "● API Online" : "● API Offline");
                apiStatusLabel.setStyle("-fx-text-fill:" + (ok ? "#3fb950" : "#f85149")
                    + "; -fx-font-weight:bold; -fx-font-size:12px;");
            }
        }));
    }

    @FXML private void handleGenerate() {
        if (generateBtn     != null) generateBtn.setDisable(true);
        if (generateProgress != null) { generateProgress.setProgress(-1); generateProgress.setVisible(true); generateProgress.setManaged(true); }
        setStatus("Generating report…", "#8b949e");

        String athleteFilter = athleteCombo != null ? athleteCombo.getValue() : "Whole Team";
        String reportType    = reportTypeCombo != null ? reportTypeCombo.getValue() : "Clinical Summary";
        LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : LocalDate.now().minusMonths(1);
        LocalDate to   = toDatePicker   != null ? toDatePicker.getValue()   : LocalDate.now();
        String period  = from.format(DateTimeFormatter.ISO_DATE) + " → " + to.format(DateTimeFormatter.ISO_DATE);

        // Resolve athlete
        Athlete targetAthlete = null;
        if (!"Whole Team".equals(athleteFilter) && athleteCombo != null) {
            int idx = athleteCombo.getItems().indexOf(athleteFilter) - 1;
            if (idx >= 0 && idx < athletes.size()) targetAthlete = athletes.get(idx);
        }
        final Athlete athlete = targetAthlete;

        // Build payloads
        Map<String,String> teamInfo = new HashMap<>();
        teamInfo.put("name", session.getCurrentTeam().getName());
        teamInfo.put("sport", session.getCurrentTeam().getSport() != null ? session.getCurrentTeam().getSport() : "—");

        Map<String,String> doctorInfo = new HashMap<>();
        var user = session.getCurrentUser();
        doctorInfo.put("name",  user != null ? user.getName() : "Medical Staff");
        doctorInfo.put("email", user != null ? user.getEmail() : "");

        Map<String,Object> athleteInfo = new HashMap<>();
        List<Map<String,Object>> sessions  = new ArrayList<>();
        List<Map<String,Object>> soapNotes = new ArrayList<>();

        if (athlete != null) {
            athleteInfo.put("name",   athlete.getName() != null ? athlete.getName() : "—");
            athleteInfo.put("sport",  athlete.getSport() != null ? athlete.getSport() : "—");
            Athlete profile = db.findAthleteProfile(athlete.getUserId()).orElse(null);
            if (profile != null) {
                athleteInfo.put("age",     profile.calculateAge() > 0 ? profile.calculateAge() : "—");
                athleteInfo.put("hr_max",  (int) profile.calculateHrMax());
                athleteInfo.put("height",  profile.getHeightCm() > 0 ? profile.getHeightCm() : "—");
                athleteInfo.put("weight",  profile.getWeightKg() > 0 ? profile.getWeightKg() : "—");
            }
            // SOAP notes
            List<SoapNote> soap = db.findSoapNotesByAthlete(athlete.getUserId());
            for (SoapNote s : soap) {
                Map<String,Object> m = new HashMap<>();
                m.put("date",        s.getDate() != null ? s.getDate().toString() : "");
                m.put("subjective",  s.getSubjective() != null ? s.getSubjective() : "");
                m.put("objective",   s.getObjective()  != null ? s.getObjective()  : "");
                m.put("assessment",  s.getAssessment() != null ? s.getAssessment() : "");
                m.put("plan",        s.getPlan()       != null ? s.getPlan()       : "");
                soapNotes.add(m);
            }
            athleteInfo.put("athlete_id", athlete.getUserId());
        } else {
            athleteInfo.put("name", "Whole Team — " + session.getCurrentTeam().getName());
            athleteInfo.put("athlete_id", "all");
        }

        String athleteId = athlete != null ? athlete.getUserId() : "all";

        analytics.generateAthletePdf(athleteId, athleteInfo, teamInfo, doctorInfo,
            sessions, soapNotes, null, null, period)
            .thenAccept(pdfBytes -> {
                Platform.runLater(() -> {
                    if (generateProgress != null) { generateProgress.setVisible(false); generateProgress.setManaged(false); }
                    if (generateBtn != null) generateBtn.setDisable(false);
                });
                if (pdfBytes != null && pdfBytes.length > 0) {
                    try {
                        File tmp = File.createTempFile("medsport_" + reportType.replace(" ","_") + "_", ".pdf");
                        try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(pdfBytes); }
                        Platform.runLater(() -> {
                            setStatus("PDF saved: " + tmp.getName(), "#3fb950");
                            addRecentReport(reportType, tmp.getAbsolutePath());
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> setStatus("Error saving PDF: " + ex.getMessage(), "#f85149"));
                    }
                } else {
                    // API offline — write plain-text placeholder
                    try {
                        File tmp = File.createTempFile("medsport_report_", ".txt");
                        try (PrintWriter pw = new PrintWriter(tmp, java.nio.charset.StandardCharsets.UTF_8)) {
                            pw.println("=== MedSport Monitor — " + reportType + " ===");
                            pw.println("Team: " + session.getCurrentTeam().getName());
                            pw.println("Athlete: " + athleteFilter);
                            pw.println("Period: " + period);
                            pw.println("Generated: " + LocalDate.now());
                            pw.println();
                            pw.println("[Start FastAPI at python-analytics/ for full PDF export]");
                            if (!soapNotes.isEmpty()) {
                                pw.println(); pw.println("== SOAP Notes ==");
                                for (Map<String,Object> n : soapNotes) {
                                    pw.println("Date: " + n.get("date"));
                                    pw.println("S: " + n.get("subjective"));
                                    pw.println("O: " + n.get("objective"));
                                    pw.println("A: " + n.get("assessment"));
                                    pw.println("P: " + n.get("plan"));
                                    pw.println();
                                }
                            }
                        }
                        Platform.runLater(() -> {
                            setStatus("API offline — saved text: " + tmp.getName(), "#d29922");
                            addRecentReport(reportType, tmp.getAbsolutePath());
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), "#f85149"));
                    }
                }
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    if (generateProgress != null) { generateProgress.setVisible(false); generateProgress.setManaged(false); }
                    if (generateBtn != null) generateBtn.setDisable(false);
                    setStatus("Error: " + ex.getMessage(), "#f85149");
                });
                return null;
            });
    }

    private void addRecentReport(String type, String path) {
        if (recentReportsList == null) return;
        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("btn-ghost");
        openBtn.setStyle("-fx-padding:2 10 2 10; -fx-font-size:11px;");
        openBtn.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().open(new File(path)); }
            catch (IOException ex) { setStatus("Cannot open: " + ex.getMessage(), "#f85149"); }
        });
        Label nameLbl = new Label(type + "  •  " + new File(path).getName());
        nameLbl.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12px;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        HBox row = new HBox(12, nameLbl, openBtn);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:8 16 8 16; -fx-background-color:rgba(22,27,34,0.5); -fx-background-radius:6;");
        recentReportsList.getChildren().add(0, row);
        if (recentReportsList.getChildren().size() > 10) recentReportsList.getChildren().remove(10);
    }

    private void setStatus(String msg, String color) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px;");
        statusLabel.setVisible(true); statusLabel.setManaged(true);
    }

    @FXML private void handleRefreshApi() { checkApiHealth(); }
}