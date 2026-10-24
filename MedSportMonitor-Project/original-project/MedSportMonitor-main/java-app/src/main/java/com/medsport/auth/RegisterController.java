package com.medsport.auth;

import com.medsport.models.Athlete;
import com.medsport.models.Team;
import com.medsport.models.User;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import com.medsport.services.TeamService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

public class RegisterController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterController.class);

    // Step 1 — Personal info
    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Step 2 — Role selection
    @FXML private ToggleGroup   roleGroup;
    @FXML private RadioButton   athleteRadio;
    @FXML private RadioButton   coachRadio;
    @FXML private RadioButton   medicalRadio;

    // Step 3 — Role-specific fields
    @FXML private Pane          athleteFields;
    @FXML private Pane          coachFields;
    @FXML private Pane          medicalFields;
    @FXML private TextField     sportField;
    @FXML private DatePicker    birthDatePicker;
    @FXML private ComboBox<String> sexCombo;
    @FXML private TextField     heightField;
    @FXML private TextField     weightField;
    @FXML private TextField     institutionField;
    @FXML private TextField     licenseField;
    @FXML private TextField     specialtyField;

    // Navigation
    @FXML private Button        backButton;
    @FXML private Button        nextButton;
    @FXML private Label         stepLabel;
    @FXML private Label         errorLabel;
    @FXML private ProgressIndicator spinner;

    // Step containers
    @FXML private VBox          step1Pane;
    @FXML private VBox          step2Pane;
    @FXML private VBox          step3Pane;

    private int currentStep = 1;
    private User.Role selectedRole;

    private final DatabaseService db      = DatabaseService.getInstance();
    private final TeamService     teamSvc = new TeamService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (spinner != null) spinner.setVisible(false);
        if (errorLabel != null) errorLabel.setVisible(false);
        if (sexCombo != null) sexCombo.getItems().addAll("Male", "Female", "Other");
        showStep(1);
    }

    @FXML
    private void handleNext() {
        clearError();
        if (currentStep == 1) {
            if (!validateStep1()) return;
            currentStep = 2;
            showStep(2);
        } else if (currentStep == 2) {
            if (!validateStep2()) return;
            currentStep = 3;
            showStep(3);
            updateRoleFields();
        } else {
            handleSubmit();
        }
    }

    @FXML
    private void handleBack() {
        clearError();
        if (currentStep > 1) {
            currentStep--;
            showStep(currentStep);
        } else {
            navigateToLogin();
        }
    }

    private boolean validateStep1() {
        String name  = textOf(nameField);
        String email = textOf(emailField);
        String pass  = passwordField != null ? passwordField.getText() : "";
        String conf  = confirmPasswordField != null ? confirmPasswordField.getText() : "";

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("All fields are required.");
            return false;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Enter a valid email address.");
            return false;
        }
        if (!PasswordUtils.isStrongPassword(pass)) {
            showError("Password must be at least 8 characters with upper, lower, and digit.");
            return false;
        }
        if (!pass.equals(conf)) {
            showError("Passwords do not match.");
            return false;
        }
        if (db.findUserByEmail(email).isPresent()) {
            showError("An account with this email already exists.");
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (roleGroup == null || roleGroup.getSelectedToggle() == null) {
            showError("Please select your role.");
            return false;
        }
        RadioButton selected = (RadioButton) roleGroup.getSelectedToggle();
        if      (selected == athleteRadio) selectedRole = User.Role.ATHLETE;
        else if (selected == coachRadio)   selectedRole = User.Role.COACH;
        else if (selected == medicalRadio) selectedRole = User.Role.MEDICAL;
        else { showError("Please select your role."); return false; }
        return true;
    }

    private void handleSubmit() {
        setLoading(true);
        Thread t = new Thread(() -> {
            try {
                String userId = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                User user = new User(
                    userId,
                    textOf(emailField),
                    PasswordUtils.hash(passwordField.getText()),
                    selectedRole,
                    textOf(nameField)
                );
                db.saveUser(user);

                // Save role-specific profile
                switch (selectedRole) {
                    case ATHLETE -> {
                        Athlete a = new Athlete();
                        a.setUserId(userId);
                        a.setSport(textOf(sportField));
                        if (birthDatePicker != null && birthDatePicker.getValue() != null) {
                            a.setBirthDate(birthDatePicker.getValue());
                        }
                        if (sexCombo != null && sexCombo.getValue() != null) {
                            a.setSex(switch (sexCombo.getValue()) {
                                case "Male"   -> Athlete.Sex.M;
                                case "Female" -> Athlete.Sex.F;
                                default       -> Athlete.Sex.OTHER;
                            });
                        }
                        parseDouble(heightField).ifPresent(a::setHeightCm);
                        parseDouble(weightField).ifPresent(a::setWeightKg);
                        if (birthDatePicker != null && birthDatePicker.getValue() != null) {
                            int age = LocalDate.now().getYear() - birthDatePicker.getValue().getYear();
                            a.setHrMax((int)(208 - 0.7 * age));
                        }
                        db.saveAthleteProfile(a);
                    }
                    case COACH -> db.saveCoachProfile(userId, textOf(sportField), 0, textOf(institutionField));
                    case MEDICAL -> db.saveMedicalProfile(userId, textOf(specialtyField),
                                                          textOf(licenseField), textOf(institutionField));
                }

                SessionManager.getInstance().login(user, null);
                AppContext.getInstance().connectMqtt();

                Platform.runLater(() -> {
                    setLoading(false);
                    if (selectedRole == User.Role.COACH) {
                        navigateToTeamWizard();
                    } else {
                        navigateToMain(user);
                    }
                });

            } catch (Exception ex) {
                LOG.error("Registration error", ex);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Registration failed: " + ex.getMessage());
                });
            }
        }, "register-thread");
        t.setDaemon(true);
        t.start();
    }

    private void navigateToTeamWizard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TeamWizard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/theme-coach.css")).toExternalForm()
            );
            Stage stage = (Stage) nextButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            LOG.error("Navigate to team wizard failed", e);
        }
    }

    private void navigateToMain(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);
            String themeCss = switch (user.getRole()) {
                case ATHLETE -> "/css/theme-athlete.css";
                case COACH   -> "/css/theme-coach.css";
                case MEDICAL -> "/css/theme-medical.css";
            };
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource(themeCss)).toExternalForm()
            );
            Stage stage = (Stage) nextButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            LOG.error("Navigate to main failed", e);
            showError("UI loading error. Please restart.");
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            LOG.error("Navigate to login failed", e);
        }
    }

    private void showStep(int step) {
        if (step1Pane != null) step1Pane.setVisible(step == 1);
        if (step2Pane != null) step2Pane.setVisible(step == 2);
        if (step3Pane != null) step3Pane.setVisible(step == 3);
        if (stepLabel != null) stepLabel.setText("Step " + step + " of 3");
        if (backButton != null) backButton.setText(step == 1 ? "Back to Login" : "Back");
        if (nextButton != null) nextButton.setText(step == 3 ? "Create Account" : "Next");
    }

    private void updateRoleFields() {
        if (athleteFields != null) athleteFields.setVisible(selectedRole == User.Role.ATHLETE);
        if (coachFields != null)   coachFields.setVisible(selectedRole == User.Role.COACH);
        if (medicalFields != null) medicalFields.setVisible(selectedRole == User.Role.MEDICAL);
    }

    private void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        if (nextButton != null)   nextButton.setDisable(loading);
        if (backButton != null)   backButton.setDisable(loading);
        if (spinner != null)      spinner.setVisible(loading);
    }

    private String textOf(TextField f) {
        return f != null ? f.getText().trim() : "";
    }

    private java.util.Optional<Double> parseDouble(TextField f) {
        try {
            return java.util.Optional.of(Double.parseDouble(textOf(f)));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
