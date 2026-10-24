package com.medsport.controllers.athlete;

import com.medsport.auth.PasswordUtils;
import com.medsport.auth.SessionManager;
import com.medsport.models.Athlete;
import com.medsport.models.User;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label   nameLabel;
    @FXML private Label   emailLabel;
    @FXML private Label   roleLabel;
    @FXML private Label   sportLabel;
    @FXML private Label   ageLabel;
    @FXML private Label   heightLabel;
    @FXML private Label   weightLabel;
    @FXML private Label   hrMaxLabel;
    @FXML private Label   bmiLabel;
    @FXML private Label   teamLabel;

    // Edit form
    @FXML private TextField     editNameField;
    @FXML private TextField     editHeightField;
    @FXML private TextField     editWeightField;
    @FXML private TextField     editSportField;
    @FXML private PasswordField editPasswordField;
    @FXML private PasswordField editConfirmField;
    @FXML private Label         editErrorLabel;
    @FXML private Label         editSuccessLabel;

    private final SessionManager session = SessionManager.getInstance();
    private final DatabaseService db     = AppContext.getInstance().getDatabase();
    private Athlete athleteProfile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (editErrorLabel   != null) { editErrorLabel.setVisible(false);   editErrorLabel.setManaged(false); }
        if (editSuccessLabel != null) { editSuccessLabel.setVisible(false); editSuccessLabel.setManaged(false); }
        loadProfile();
    }

    private void loadProfile() {
        User user = session.getCurrentUser();
        if (user == null) return;

        if (nameLabel  != null) nameLabel.setText(user.getName());
        if (emailLabel != null) emailLabel.setText(user.getEmail());
        if (roleLabel  != null) roleLabel.setText("Athlete");
        if (teamLabel  != null) teamLabel.setText(
            session.hasTeam() ? session.getCurrentTeam().getName() : "No team");

        Optional<Athlete> opt = db.findAthleteProfile(user.getId());
        if (opt.isPresent()) {
            athleteProfile = opt.get();
            if (sportLabel  != null) sportLabel.setText(athleteProfile.getSport() != null ? athleteProfile.getSport() : "—");
            if (ageLabel    != null) ageLabel.setText(athleteProfile.calculateAge() > 0 ? athleteProfile.calculateAge() + " years" : "—");
            if (heightLabel != null) heightLabel.setText(athleteProfile.getHeightCm() != null ? athleteProfile.getHeightCm().intValue() + " cm" : "—");
            if (weightLabel != null) weightLabel.setText(athleteProfile.getWeightKg() != null ? String.format("%.1f kg", athleteProfile.getWeightKg()) : "—");
            if (hrMaxLabel  != null) hrMaxLabel.setText(athleteProfile.calculateHrMax() + " bpm");

            if (athleteProfile.getHeightCm() != null && athleteProfile.getWeightKg() != null) {
                double h = athleteProfile.getHeightCm() / 100.0;
                double bmi = athleteProfile.getWeightKg() / (h * h);
                if (bmiLabel != null) bmiLabel.setText(String.format("%.1f", bmi));
            }

            // Pre-fill edit form
            if (editNameField   != null) editNameField.setText(user.getName());
            if (editSportField  != null) editSportField.setText(athleteProfile.getSport() != null ? athleteProfile.getSport() : "");
            if (editHeightField != null && athleteProfile.getHeightCm() != null)
                editHeightField.setText(String.valueOf(athleteProfile.getHeightCm().intValue()));
            if (editWeightField != null && athleteProfile.getWeightKg() != null)
                editWeightField.setText(String.format("%.1f", athleteProfile.getWeightKg()));
        }
    }

    @FXML
    private void handleSaveProfile() {
        clearMessages();
        try {
            String name = editNameField != null ? editNameField.getText().trim() : "";
            if (name.isEmpty()) { showError("Name cannot be empty."); return; }

            User user = session.getCurrentUser();
            user.setName(name);
            db.updateUserName(user.getId(), name);

            if (athleteProfile != null) {
                if (editSportField  != null && !editSportField.getText().isEmpty())
                    athleteProfile.setSport(editSportField.getText().trim());
                try {
                    if (editHeightField != null && !editHeightField.getText().isEmpty())
                        athleteProfile.setHeightCm(Double.parseDouble(editHeightField.getText()));
                    if (editWeightField != null && !editWeightField.getText().isEmpty())
                        athleteProfile.setWeightKg(Double.parseDouble(editWeightField.getText()));
                } catch (NumberFormatException ignored) {}
                db.updateAthleteProfile(athleteProfile);
            }

            // Password change
            String pass = editPasswordField != null ? editPasswordField.getText() : "";
            if (!pass.isEmpty()) {
                String conf = editConfirmField != null ? editConfirmField.getText() : "";
                if (!pass.equals(conf)) { showError("Passwords do not match."); return; }
                if (!PasswordUtils.isStrongPassword(pass)) { showError("Password too weak."); return; }
                db.updateUserPassword(user.getId(), PasswordUtils.hash(pass));
            }

            showSuccess("Profile saved successfully.");
            loadProfile();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (editErrorLabel != null) { editErrorLabel.setText(msg); editErrorLabel.setVisible(true); editErrorLabel.setManaged(true); }
    }
    private void showSuccess(String msg) {
        if (editSuccessLabel != null) { editSuccessLabel.setText(msg); editSuccessLabel.setVisible(true); editSuccessLabel.setManaged(true); }
    }
    private void clearMessages() {
        if (editErrorLabel   != null) { editErrorLabel.setVisible(false);   editErrorLabel.setManaged(false); }
        if (editSuccessLabel != null) { editSuccessLabel.setVisible(false); editSuccessLabel.setManaged(false); }
    }
}