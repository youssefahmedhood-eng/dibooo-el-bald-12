package com.medsport.controllers.shared;

import com.medsport.auth.SessionManager;
import com.medsport.models.Team;
import com.medsport.services.TeamService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class TeamWizardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(TeamWizardController.class);

    @FXML private Label stepLabel;
    @FXML private Button nextButton, backButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Label wdot1, wdot2, wdot3, wdot4;
    @FXML private VBox  wStep1, wStep2, wStep3, wStep4;
    @FXML private TextField teamNameField;
    @FXML private ComboBox<String> sportCombo, categoryCombo;
    @FXML private Slider hrThresholdSlider, spo2ThresholdSlider, loadSlider;
    @FXML private Label  hrThresholdLabel, spo2ThresholdLabel, loadLabel;
    @FXML private Label  joinCodeDisplay, joinCodeSmall;

    private int  currentStep = 1;
    private Team createdTeam;

    private final TeamService    teamSvc = new TeamService();
    private final SessionManager session = SessionManager.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hide(spinner); hide(errorLabel);
        if (sportCombo    != null) sportCombo.getItems().addAll(
            "Football","Basketball","Athletics","Swimming","Cycling","Tennis","Volleyball","Rugby","Other");
        if (categoryCombo != null) categoryCombo.getItems().addAll(
            "Senior","U23","U18","U16","U14","Amateur","Professional");
        bindSlider(hrThresholdSlider,   hrThresholdLabel,  "%");
        bindSlider(spo2ThresholdSlider, spo2ThresholdLabel,"%");
        bindSlider(loadSlider,          loadLabel,         "");
        showStep(1);
    }

    private void bindSlider(Slider s, Label l, String suffix) {
        if (s != null && l != null)
            s.valueProperty().addListener((o, ov, nv) -> l.setText((int)nv.doubleValue() + suffix));
    }

    @FXML private void handleNext() {
        clearError();
        switch (currentStep) {
            case 1 -> { if (validateStep1()) { currentStep = 2; showStep(2); } }
            case 2 -> createTeamAsync();
            case 3 -> { currentStep = 4; showStep(4); }
            case 4 -> navigateToDashboard();
        }
    }

    @FXML private void handleBack() {
        clearError();
        if (currentStep > 1 && currentStep != 3) { currentStep--; showStep(currentStep); }
    }

    @FXML private void copyJoinCode() {
        if (createdTeam == null) return;
        ClipboardContent cc = new ClipboardContent();
        cc.putString(createdTeam.getJoinCode());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private boolean validateStep1() {
        String n = teamNameField != null ? teamNameField.getText().trim() : "";
        if (n.length() < 3) { showError("Team name must be at least 3 characters."); return false; }
        return true;
    }

    private void createTeamAsync() {
        setLoading(true);
        // Build Team object from form fields
        Team team = new Team();
        team.setName(teamNameField  != null ? teamNameField.getText().trim() : "My Team");
        team.setSport(sportCombo    != null && sportCombo.getValue() != null ? sportCombo.getValue() : "Other");
        // Map String category to enum (default AMATEUR if unrecognised)
        if (categoryCombo != null && categoryCombo.getValue() != null) {
            String cat = categoryCombo.getValue();
            team.setCategory(switch (cat) {
                case "Professional" -> Team.Category.PROFESSIONAL;
                case "U23","U18","U16","U14","Senior" -> Team.Category.YOUTH;
                case "Amateur" -> Team.Category.AMATEUR;
                default -> Team.Category.AMATEUR;
            });
        }
        team.setHrAlertThreshold(hrThresholdSlider   != null ? hrThresholdSlider.getValue()   : 95.0);
        team.setSpo2AlertThreshold(spo2ThresholdSlider!= null ? spo2ThresholdSlider.getValue() : 93.0);
        team.setWeeklyLoadThreshold(loadSlider         != null ? loadSlider.getValue()          : 1000.0);

        Thread t = new Thread(() -> {
            try {
                Team created = teamSvc.createTeam(team, session.getUserId());
                session.setCurrentTeam(created);
                createdTeam = created;
                Platform.runLater(() -> {
                    setLoading(false);
                    if (joinCodeDisplay != null) joinCodeDisplay.setText(created.getJoinCode());
                    if (joinCodeSmall   != null) joinCodeSmall.setText(created.getJoinCode());
                    currentStep = 3; showStep(3);
                });
            } catch (Exception e) {
                LOG.error("Team creation failed", e);
                Platform.runLater(() -> { setLoading(false); showError(e.getMessage()); });
            }
        }, "team-create-thread");
        t.setDaemon(true);
        t.start();
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme-coach.css")).toExternalForm());
            ((Stage) nextButton.getScene().getWindow()).setScene(scene);
        } catch (Exception e) { LOG.error("Navigate failed", e); }
    }

    private void showStep(int step) {
        setPane(wStep1, step == 1); setPane(wStep2, step == 2);
        setPane(wStep3, step == 3); setPane(wStep4, step == 4);
        if (stepLabel  != null) stepLabel.setText("Step " + step + " of 4");
        if (backButton != null) backButton.setDisable(step == 1 || step == 3);
        if (nextButton != null) nextButton.setText(
            step == 2 ? "Create Team" : step == 4 ? "Go to Dashboard" : "Next");
        updateDot(wdot1, step, 1); updateDot(wdot2, step, 2);
        updateDot(wdot3, step, 3); updateDot(wdot4, step, 4);
    }

    private void updateDot(Label d, int cur, int pos) {
        if (d == null) return;
        d.getStyleClass().removeAll("wizard-step-active","wizard-step-done","wizard-step-pending");
        if (pos < cur)       d.getStyleClass().add("wizard-step-done");
        else if (pos == cur) d.getStyleClass().add("wizard-step-active");
        else                 d.getStyleClass().add("wizard-step-pending");
    }

    private void setPane(VBox p, boolean v) { if (p != null) { p.setVisible(v); p.setManaged(v); } }
    private void show(javafx.scene.Node n)  { if (n != null) { n.setVisible(true);  n.setManaged(true); } }
    private void hide(javafx.scene.Node n)  { if (n != null) { n.setVisible(false); n.setManaged(false); } }
    private void showError(String m) { if (errorLabel != null) { errorLabel.setText(m); show(errorLabel); } }
    private void clearError()        { hide(errorLabel); }
    private void setLoading(boolean l) {
        if (nextButton != null) nextButton.setDisable(l);
        if (backButton != null) backButton.setDisable(l);
        if (spinner    != null) { spinner.setVisible(l); spinner.setManaged(l); }
    }
}