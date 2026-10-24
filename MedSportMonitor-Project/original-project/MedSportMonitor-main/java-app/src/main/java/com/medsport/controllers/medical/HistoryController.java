package com.medsport.controllers.medical;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.SoapNote;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(HistoryController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Athlete selector
    @FXML private ComboBox<String> athleteCombo;

    // Notes list
    @FXML private ListView<SoapNote> notesList;
    @FXML private Label              noNotesLabel;

    // Note detail panel
    @FXML private VBox    detailPane;
    @FXML private Label   noteDateLabel;
    @FXML private Label   noteDoctorLabel;
    @FXML private TextArea subjArea;
    @FXML private TextArea objArea;
    @FXML private TextArea assessArea;
    @FXML private TextArea planArea;

    // New note form
    @FXML private VBox     newNotePane;
    @FXML private ComboBox<String> newNoteAthleteCombo;
    @FXML private TextArea newSubjArea;
    @FXML private TextArea newObjArea;
    @FXML private TextArea newAssessArea;
    @FXML private TextArea newPlanArea;
    @FXML private CheckBox privateCheck;
    @FXML private Label    saveStatusLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();

    private List<Athlete> athletes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());

        athletes.forEach(a -> {
            String name = a.getName() != null ? a.getName() : a.getUserId();
            if (athleteCombo      != null) athleteCombo.getItems().add(name);
            if (newNoteAthleteCombo != null) newNoteAthleteCombo.getItems().add(name);
        });

        if (athleteCombo != null) {
            athleteCombo.setOnAction(e -> loadNotes());
            if (!athleteCombo.getItems().isEmpty()) athleteCombo.setValue(athleteCombo.getItems().get(0));
        }
        if (newNoteAthleteCombo != null && !newNoteAthleteCombo.getItems().isEmpty())
            newNoteAthleteCombo.setValue(newNoteAthleteCombo.getItems().get(0));

        if (detailPane != null) { detailPane.setVisible(false); detailPane.setManaged(false); }
        if (newNotePane != null) { newNotePane.setVisible(false); newNotePane.setManaged(false); }
        if (saveStatusLabel != null) { saveStatusLabel.setVisible(false); saveStatusLabel.setManaged(false); }

        setupNotesList();
        loadNotes();
    }

    private void setupNotesList() {
        if (notesList == null) return;
        notesList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SoapNote n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); setGraphic(null); return; }
                String date = n.getDate() != null ? n.getDate().format(DATE_FMT) : "—";
                String preview = n.getSubjective() != null
                    ? (n.getSubjective().length() > 40 ? n.getSubjective().substring(0, 40) + "…" : n.getSubjective())
                    : "(no subjective)";
                setText(date + "  " + preview);
                setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12px;");
            }
        });
        notesList.getSelectionModel().selectedItemProperty().addListener((o, ov, note) -> {
            if (note != null) showNote(note);
        });
    }

    private void loadNotes() {
        if (athleteCombo == null) return;
        int idx = athleteCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= athletes.size()) return;
        Athlete a = athletes.get(idx);

        Thread t = new Thread(() -> {
            List<SoapNote> notes = db.findSoapNotesByAthlete(a.getUserId());
            Platform.runLater(() -> {
                if (notesList != null) {
                    notesList.getItems().setAll(notes);
                }
                if (noNotesLabel != null) {
                    noNotesLabel.setVisible(notes.isEmpty());
                    noNotesLabel.setManaged(notes.isEmpty());
                }
                if (detailPane != null) { detailPane.setVisible(false); detailPane.setManaged(false); }
            });
        }, "load-notes");
        t.setDaemon(true); t.start();
    }

    private void showNote(SoapNote n) {
        if (detailPane == null) return;
        detailPane.setVisible(true); detailPane.setManaged(true);
        if (noteDateLabel  != null) noteDateLabel.setText(n.getDate() != null ? n.getDate().format(DATE_FMT) : "—");
        if (noteDoctorLabel != null) noteDoctorLabel.setText(n.getDoctorName() != null ? n.getDoctorName() : "—");
        if (subjArea   != null) { subjArea.setText(n.getSubjective());   subjArea.setEditable(false); }
        if (objArea    != null) { objArea.setText(n.getObjective());     objArea.setEditable(false); }
        if (assessArea != null) { assessArea.setText(n.getAssessment()); assessArea.setEditable(false); }
        if (planArea   != null) { planArea.setText(n.getPlan());         planArea.setEditable(false); }
    }

    @FXML private void handleNewNote() {
        if (newNotePane == null) return;
        boolean show = !newNotePane.isVisible();
        newNotePane.setVisible(show); newNotePane.setManaged(show);
        if (show && newSubjArea != null) {
            newSubjArea.clear(); newObjArea.clear(); newAssessArea.clear(); newPlanArea.clear();
        }
    }

    @FXML private void handleSaveNote() {
        if (newNoteAthleteCombo == null) return;
        int idx = newNoteAthleteCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= athletes.size()) return;
        Athlete a = athletes.get(idx);

        SoapNote n = new SoapNote();
        n.setId(java.util.UUID.randomUUID().toString());
        n.setAthleteId(a.getUserId());
        n.setDoctorId(session.getUserId());
        n.setSubjective(newSubjArea  != null ? newSubjArea.getText()  : "");
        n.setObjective(newObjArea    != null ? newObjArea.getText()   : "");
        n.setAssessment(newAssessArea != null ? newAssessArea.getText(): "");
        n.setPlan(newPlanArea        != null ? newPlanArea.getText()  : "");
        n.setPrivate(privateCheck    != null && privateCheck.isSelected());

        Thread t = new Thread(() -> {
            try {
                db.saveSoapNote(n);
                Platform.runLater(() -> {
                    if (saveStatusLabel != null) {
                        saveStatusLabel.setText("Note saved.");
                        saveStatusLabel.setStyle("-fx-text-fill:#3fb950;");
                        saveStatusLabel.setVisible(true); saveStatusLabel.setManaged(true);
                    }
                    if (newNotePane != null) { newNotePane.setVisible(false); newNotePane.setManaged(false); }
                    if (athleteCombo != null) athleteCombo.setValue(
                        newNoteAthleteCombo != null ? newNoteAthleteCombo.getValue() : null);
                    loadNotes();
                });
            } catch (Exception ex) {
                LOG.error("Save note error", ex);
                Platform.runLater(() -> {
                    if (saveStatusLabel != null) {
                        saveStatusLabel.setText("Error: " + ex.getMessage());
                        saveStatusLabel.setStyle("-fx-text-fill:#f85149;");
                        saveStatusLabel.setVisible(true); saveStatusLabel.setManaged(true);
                    }
                });
            }
        }, "save-note");
        t.setDaemon(true); t.start();
    }

    @FXML private void handleCancelNote() {
        if (newNotePane != null) { newNotePane.setVisible(false); newNotePane.setManaged(false); }
    }
}