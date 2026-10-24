package com.medsport.controllers.medical;

import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.MedicalRestriction;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RestrictionsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(RestrictionsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private ComboBox<String> athleteCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        hrLimitField;
    @FXML private Label            hrLimitRow;
    @FXML private TextArea         descriptionArea;
    @FXML private DatePicker       returnDatePicker;
    @FXML private Label            saveStatusLabel;
    @FXML private Button           addBtn;

    // Active restrictions list
    @FXML private VBox             restrictionList;
    @FXML private Label            noRestrictionsLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();

    private List<Athlete> athletes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) return;
        athletes = db.findAthletesByTeam(session.getTeamId());
        athletes.forEach(a -> {
            if (athleteCombo != null)
                athleteCombo.getItems().add(a.getName() != null ? a.getName() : a.getUserId());
        });
        if (athleteCombo != null && !athleteCombo.getItems().isEmpty())
            athleteCombo.setValue(athleteCombo.getItems().get(0));

        if (typeCombo != null) {
            typeCombo.getItems().addAll("Full Training Restriction", "Partial Training Restriction", "HR Limited");
            typeCombo.setValue("Full Training Restriction");
            typeCombo.setOnAction(e -> {
                boolean hrMode = "HR Limited".equals(typeCombo.getValue());
                if (hrLimitRow  != null) { hrLimitRow.setVisible(hrMode);  hrLimitRow.setManaged(hrMode);  }
                if (hrLimitField != null) { hrLimitField.setVisible(hrMode); hrLimitField.setManaged(hrMode); }
            });
        }
        if (hrLimitRow   != null) { hrLimitRow.setVisible(false);  hrLimitRow.setManaged(false);  }
        if (hrLimitField != null) { hrLimitField.setVisible(false); hrLimitField.setManaged(false); }
        if (saveStatusLabel != null) { saveStatusLabel.setVisible(false); saveStatusLabel.setManaged(false); }

        loadRestrictions();
    }

    @FXML private void handleAdd() {
        if (athleteCombo == null) return;
        int idx = athleteCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= athletes.size()) return;
        Athlete a = athletes.get(idx);

        MedicalRestriction r = new MedicalRestriction();
        r.setId(UUID.randomUUID().toString());
        r.setAthleteId(a.getUserId());
        r.setDoctorId(session.getUserId());
        r.setDescription(descriptionArea != null ? descriptionArea.getText() : "");
        if (returnDatePicker != null && returnDatePicker.getValue() != null)
            r.setReturnDate(returnDatePicker.getValue());

        String type = typeCombo != null ? typeCombo.getValue() : "Full Training Restriction";
        r.setType(switch (type) {
            case "Partial Training Restriction" -> MedicalRestriction.Type.PARTIAL;
            case "HR Limited"                  -> MedicalRestriction.Type.HR_LIMITED;
            default                            -> MedicalRestriction.Type.FULL;
        });
        if (r.getType() == MedicalRestriction.Type.HR_LIMITED && hrLimitField != null) {
            try { r.setHrLimit(Integer.parseInt(hrLimitField.getText().trim())); }
            catch (NumberFormatException ignored) { r.setHrLimit(150); }
        }

        Thread t = new Thread(() -> {
            try {
                db.saveRestriction(r);
                Platform.runLater(() -> {
                    setStatus("Restriction added.", "#3fb950");
                    if (descriptionArea != null) descriptionArea.clear();
                    if (returnDatePicker != null) returnDatePicker.setValue(null);
                    loadRestrictions();
                });
            } catch (Exception ex) {
                LOG.error("Save restriction error", ex);
                Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), "#f85149"));
            }
        }, "save-restriction");
        t.setDaemon(true); t.start();
    }

    private void loadRestrictions() {
        if (!session.hasTeam()) return;
        Thread t = new Thread(() -> {
            List<MedicalRestriction> all = db.findAllRestrictionsForTeam(session.getTeamId());
            List<MedicalRestriction> active = all.stream().filter(MedicalRestriction::isActive).toList();
            Platform.runLater(() -> {
                if (restrictionList == null) return;
                restrictionList.getChildren().clear();
                boolean empty = active.isEmpty();
                if (noRestrictionsLabel != null) {
                    noRestrictionsLabel.setVisible(empty); noRestrictionsLabel.setManaged(empty);
                }
                active.forEach(r -> restrictionList.getChildren().add(buildRestrictionRow(r)));
            });
        }, "load-restrictions");
        t.setDaemon(true); t.start();
    }

    private HBox buildRestrictionRow(MedicalRestriction r) {
        Label nameLbl = new Label(r.getAthleteName() != null ? r.getAthleteName() : r.getAthleteId());
        nameLbl.setStyle("-fx-text-fill:#e6edf3; -fx-font-weight:bold; -fx-font-size:13px;");
        nameLbl.setMinWidth(140);

        Label typeLbl = new Label(r.getPublicSummary());
        typeLbl.setStyle("-fx-text-fill:#d29922; -fx-font-size:12px;");
        HBox.setHgrow(typeLbl, Priority.ALWAYS);

        Label returnLbl = new Label(r.getReturnDate() != null
            ? "Until " + r.getReturnDate().format(DATE_FMT) : "Open-ended");
        returnLbl.setStyle("-fx-text-fill:#8b949e; -fx-font-size:12px;");

        Button resolveBtn = new Button("Resolve");
        resolveBtn.getStyleClass().add("btn-ghost");
        resolveBtn.setStyle("-fx-padding:4 10 4 10; -fx-font-size:11px;");
        resolveBtn.setOnAction(e -> {
            r.setStatus(MedicalRestriction.Status.RESOLVED);
            Thread th = new Thread(() -> {
                try { db.saveRestriction(r); } catch (Exception ex) { LOG.error("Resolve error", ex); }
                Platform.runLater(this::loadRestrictions);
            }, "resolve-restriction");
            th.setDaemon(true); th.start();
        });

        HBox row = new HBox(12, nameLbl, typeLbl, returnLbl, resolveBtn);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("glass-card-sm");
        return row;
    }

    private void setStatus(String msg, String color) {
        if (saveStatusLabel == null) return;
        saveStatusLabel.setText(msg);
        saveStatusLabel.setStyle("-fx-text-fill:" + color + ";");
        saveStatusLabel.setVisible(true); saveStatusLabel.setManaged(true);
    }
}