package com.medsport.controllers.athlete;

import com.medsport.auth.SessionManager;
import com.medsport.models.Session;
import com.medsport.services.AppContext;
import com.medsport.services.SessionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class SessionsController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    @FXML private TableView<Session>          sessionsTable;
    @FXML private TableColumn<Session,String> colDate;
    @FXML private TableColumn<Session,String> colType;
    @FXML private TableColumn<Session,String> colDuration;
    @FXML private TableColumn<Session,String> colStatus;

    @FXML private VBox   detailPane;
    @FXML private Label  detailDate;
    @FXML private Label  detailType;
    @FXML private Label  detailDuration;
    @FXML private Label  detailZones;
    @FXML private Label  detailNotes;
    @FXML private Label  emptyLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final SessionService sessSvc = AppContext.getInstance().getSessions();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadSessions();
        if (detailPane != null) { detailPane.setVisible(false); detailPane.setManaged(false); }
    }

    private void setupTable() {
        if (sessionsTable == null) return;
        sessionsTable.setPlaceholder(new Label("No sessions found."));

        if (colDate != null) colDate.setCellValueFactory(cell -> {
            Session s = cell.getValue();
            String d = s.getStartTime() != null ? s.getStartTime().format(FMT) : "—";
            return new javafx.beans.property.SimpleStringProperty(d);
        });
        if (colType != null) colType.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getExerciseType() != null ? cell.getValue().getExerciseType() : "Training"));
        if (colDuration != null) colDuration.setCellValueFactory(cell -> {
            long min = cell.getValue().getDurationMinutes();
            String d = min > 0 ? min + " min" : (cell.getValue().getStatus() == Session.Status.ACTIVE ? "Active" : "—");
            return new javafx.beans.property.SimpleStringProperty(d);
        });
        if (colStatus != null) colStatus.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().name()));

        sessionsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> { if (sel != null) showDetail(sel); });
    }

    private void loadSessions() {
        List<Session> list = sessSvc.getAthleteSessions(session.getUserId());
        if (sessionsTable != null) {
            sessionsTable.setItems(FXCollections.observableArrayList(list));
        }
        if (emptyLabel != null) {
            emptyLabel.setVisible(list.isEmpty());
            emptyLabel.setManaged(list.isEmpty());
        }
    }

    private void showDetail(Session s) {
        if (detailPane == null) return;
        detailPane.setVisible(true); detailPane.setManaged(true);
        if (detailDate     != null) detailDate.setText(s.getStartTime() != null ? s.getStartTime().format(FMT) : "—");
        if (detailType     != null) detailType.setText(s.getExerciseType() != null ? s.getExerciseType() : "Training");
        if (detailDuration != null) detailDuration.setText(s.getDurationMinutes() > 0 ? s.getDurationMinutes() + " min" : "—");
        if (detailZones    != null) detailZones.setText("Z" + s.getTargetZoneMin() + "–Z" + s.getTargetZoneMax());
        if (detailNotes    != null) detailNotes.setText(s.getNotes() != null ? s.getNotes() : "No notes.");
    }

    @FXML private void handleRefresh() { loadSessions(); }
}