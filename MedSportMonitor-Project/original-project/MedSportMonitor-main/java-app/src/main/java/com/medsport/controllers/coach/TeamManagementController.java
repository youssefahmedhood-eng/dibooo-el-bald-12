package com.medsport.controllers.coach;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.Team;
import com.medsport.services.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class TeamManagementController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(TeamManagementController.class);

    @FXML private Label  teamNameLabel;
    @FXML private Label  teamCodeLabel;
    @FXML private Label  sportLabel;
    @FXML private Label  memberCountLabel;

    @FXML private TableView<Athlete>          athleteTable;
    @FXML private TableColumn<Athlete,String> colName;
    @FXML private TableColumn<Athlete,String> colSport;
    @FXML private TableColumn<Athlete,String> colAge;
    @FXML private TableColumn<Athlete,String> colHrMax;

    // Thresholds edit
    @FXML private Slider hrThreshSlider;
    @FXML private Label  hrThreshLabel;
    @FXML private Slider spo2ThreshSlider;
    @FXML private Label  spo2ThreshLabel;
    @FXML private Slider weeklyLoadSlider;
    @FXML private Label  weeklyLoadLabel;

    @FXML private Button     saveThreshBtn;
    @FXML private Label      saveStatusLabel;

    // Remove member
    @FXML private Button     removeMemberBtn;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();
    private final TeamService     teamSvc= new TeamService();

    private List<Athlete> athletes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        Team team = session.getCurrentTeam();

        if (teamNameLabel  != null) teamNameLabel.setText(team.getName());
        if (teamCodeLabel  != null) teamCodeLabel.setText("Join code: " + team.getJoinCode());
        if (sportLabel     != null) sportLabel.setText(team.getSport() != null ? team.getSport() : "—");

        // Thresholds
        if (hrThreshSlider != null) {
            hrThreshSlider.setMin(80); hrThreshSlider.setMax(100);
            hrThreshSlider.setValue(team.getHrAlertThreshold());
            hrThreshSlider.valueProperty().addListener((o,ov,nv) -> {
                if (hrThreshLabel != null) hrThreshLabel.setText((int)nv.doubleValue() + "%");
            });
            if (hrThreshLabel != null) hrThreshLabel.setText((int)team.getHrAlertThreshold() + "%");
        }
        if (spo2ThreshSlider != null) {
            spo2ThreshSlider.setMin(85); spo2ThreshSlider.setMax(98);
            spo2ThreshSlider.setValue(team.getSpo2AlertThreshold());
            spo2ThreshSlider.valueProperty().addListener((o,ov,nv) -> {
                if (spo2ThreshLabel != null) spo2ThreshLabel.setText((int)nv.doubleValue() + "%");
            });
            if (spo2ThreshLabel != null) spo2ThreshLabel.setText((int)team.getSpo2AlertThreshold() + "%");
        }
        if (weeklyLoadSlider != null) {
            weeklyLoadSlider.setMin(400); weeklyLoadSlider.setMax(2000);
            weeklyLoadSlider.setValue(team.getWeeklyLoadThreshold());
            weeklyLoadSlider.valueProperty().addListener((o,ov,nv) -> {
                if (weeklyLoadLabel != null) weeklyLoadLabel.setText((int)nv.doubleValue() + " AU");
            });
            if (weeklyLoadLabel != null) weeklyLoadLabel.setText((int)team.getWeeklyLoadThreshold() + " AU");
        }

        if (saveStatusLabel != null) { saveStatusLabel.setVisible(false); saveStatusLabel.setManaged(false); }

        loadAthleteTable();
    }

    private void loadAthleteTable() {
        athletes = db.findAthletesByTeam(session.getTeamId());
        if (memberCountLabel != null) memberCountLabel.setText(athletes.size() + " members");
        if (athleteTable == null) return;

        if (colName  != null) colName.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getName() != null ? c.getValue().getName() : "—"));
        if (colSport != null) colSport.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getSport() != null ? c.getValue().getSport() : "—"));
        if (colAge   != null) colAge.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().calculateAge() > 0 ? c.getValue().calculateAge() + " yr" : "—"));
        if (colHrMax != null) colHrMax.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().calculateHrMax() + " bpm"));

        athleteTable.setItems(FXCollections.observableArrayList(athletes));
    }

    @FXML
    private void handleSaveThresholds() {
        if (!session.hasTeam()) return;
        Team team = session.getCurrentTeam();
        if (hrThreshSlider   != null) team.setHrAlertThreshold(hrThreshSlider.getValue());
        if (spo2ThreshSlider != null) team.setSpo2AlertThreshold(spo2ThreshSlider.getValue());
        if (weeklyLoadSlider != null) team.setWeeklyLoadThreshold(weeklyLoadSlider.getValue());
        Thread t = new Thread(() -> {
            try {
                db.saveTeam(team);
                Platform.runLater(() -> {
                    if (saveStatusLabel != null) {
                        saveStatusLabel.setText("Saved!");
                        saveStatusLabel.setStyle("-fx-text-fill:#3fb950;");
                        saveStatusLabel.setVisible(true); saveStatusLabel.setManaged(true);
                    }
                });
            } catch (Exception e) {
                LOG.error("Save thresholds error", e);
                Platform.runLater(() -> {
                    if (saveStatusLabel != null) {
                        saveStatusLabel.setText("Error: " + e.getMessage());
                        saveStatusLabel.setStyle("-fx-text-fill:#f85149;");
                        saveStatusLabel.setVisible(true); saveStatusLabel.setManaged(true);
                    }
                });
            }
        }, "save-thresh");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleRemoveMember() {
        if (athleteTable == null) return;
        Athlete sel = athleteTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Remove " + sel.getName() + " from the team?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Thread t = new Thread(() -> {
                    try {
                        teamSvc.leaveTeam(session.getTeamId(), sel.getUserId());
                        Platform.runLater(this::loadAthleteTable);
                    } catch (Exception e) { LOG.error("Remove member error", e); }
                }, "remove-member");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    @FXML private void handleRefresh() { loadAthleteTable(); }
}