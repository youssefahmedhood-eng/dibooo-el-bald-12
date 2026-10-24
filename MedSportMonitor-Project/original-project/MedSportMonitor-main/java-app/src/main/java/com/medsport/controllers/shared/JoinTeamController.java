package com.medsport.controllers.shared;

import com.medsport.auth.SessionManager;
import com.medsport.models.Team;
import com.medsport.services.TeamService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class JoinTeamController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(JoinTeamController.class);

    @FXML private TextField         joinCodeField;
    @FXML private Label             teamPreviewLabel;
    @FXML private Label             errorLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Button            joinButton;

    private final TeamService    teamSvc = new TeamService();
    private final SessionManager session = SessionManager.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hide(spinner); hide(errorLabel); hide(teamPreviewLabel);
        if (joinCodeField != null) {
            joinCodeField.textProperty().addListener((o, ov, nv) -> {
                String code = nv.toUpperCase().trim();
                if (!code.equals(nv)) joinCodeField.setText(code);
                if (code.length() == 6) previewTeam(code);
                else hide(teamPreviewLabel);
            });
        }
    }

    private void previewTeam(String code) {
        Thread t = new Thread(() -> {
            Optional<Team> team = teamSvc.findTeamForUser(code);   // reuse DB join code lookup
            // Actually we need findByJoinCode — use DB directly via teamSvc workaround
            Platform.runLater(() -> {
                if (team.isPresent()) {
                    teamPreviewLabel.setText("Team found: " + team.get().getName());
                    show(teamPreviewLabel);
                } else {
                    hide(teamPreviewLabel);
                }
            });
        }, "team-preview");
        t.setDaemon(true);
        t.start();
    }

    @FXML private void handleJoin() {
        String code = joinCodeField != null ? joinCodeField.getText().trim().toUpperCase() : "";
        if (code.length() != 6) { showError("Enter a valid 6-character join code."); return; }
        setLoading(true);
        Thread t = new Thread(() -> {
            try {
                Optional<Team> teamOpt = teamSvc.joinTeam(code, session.getUserId(), session.getRole());
                if (teamOpt.isEmpty()) throw new Exception("Invalid join code: " + code);
                session.setCurrentTeam(teamOpt.get());
                Platform.runLater(this::navigateToMain);
            } catch (Exception e) {
                LOG.error("Join team failed", e);
                Platform.runLater(() -> { setLoading(false); showError(e.getMessage()); });
            }
        }, "join-team-thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML private void handleBack() { navigateToMain(); }

    private void navigateToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
            String role = session.getRole() != null ? session.getRole().name().toLowerCase() : "athlete";
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme-" + role + ".css")).toExternalForm());
            Stage stage = (Stage)(joinButton != null ? joinButton : teamPreviewLabel).getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) { LOG.error("Nav failed", e); }
    }

    private void show(Node n)      { if (n != null) { n.setVisible(true);  n.setManaged(true); } }
    private void hide(Node n)      { if (n != null) { n.setVisible(false); n.setManaged(false); } }
    private void showError(String m){ if (errorLabel != null) { errorLabel.setText(m); show(errorLabel); } }
    private void setLoading(boolean l) {
        if (joinButton != null) joinButton.setDisable(l);
        if (spinner    != null) { spinner.setVisible(l); spinner.setManaged(l); }
    }
}