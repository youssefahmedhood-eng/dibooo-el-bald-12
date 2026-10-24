package com.medsport.auth;

import com.medsport.models.Team;
import com.medsport.models.User;
import com.medsport.services.AppContext;
import com.medsport.services.DatabaseService;
import com.medsport.views.components.AnimatedBackground;
import com.medsport.views.components.RippleEffect;
import com.medsport.views.components.ViewTransition;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;
    @FXML private Button        registerButton;
    @FXML private ProgressIndicator spinner;
    @FXML private Pane          rootPane;
    @FXML private StackPane     bgContainer;

    private final DatabaseService db = DatabaseService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (spinner != null) spinner.setVisible(false);
        if (errorLabel != null) errorLabel.setVisible(false);

        // Enter key triggers login
        if (passwordField != null) {
            passwordField.setOnAction(e -> handleLogin());
        }
        if (emailField != null) {
            emailField.setOnAction(e -> {
                if (passwordField != null) passwordField.requestFocus();
            });
        }

        // Animated background
        if (bgContainer != null) {
            Pane bg = AnimatedBackground.create(
                bgContainer.getPrefWidth() > 0 ? bgContainer.getPrefWidth() : 1280,
                bgContainer.getPrefHeight() > 0 ? bgContainer.getPrefHeight() : 800,
                "#0A84FF");
            bg.setMouseTransparent(true);
            bgContainer.getChildren().add(0, bg);
        }

        // Ripple on login button
        if (loginButton != null) RippleEffect.attach(loginButton, Color.web("#ffffff", 0.4));

        // Entrance animation
        if (rootPane != null) ViewTransition.slideUp(rootPane);
    }

    @FXML
    private void handleLogin() {
        String email    = emailField != null ? emailField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        clearError();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            shakeField(email.isEmpty() ? emailField : passwordField);
            return;
        }

        setLoading(true);

        // Run auth off the FX thread so UI stays responsive
        Thread authThread = new Thread(() -> {
            try {
                Optional<User> userOpt = db.findUserByEmail(email);
                if (userOpt.isEmpty() || !PasswordUtils.verify(password, userOpt.get().getPasswordHash())) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Invalid email or password.");
                        shakeField(passwordField);
                    });
                    return;
                }

                User user = userOpt.get();
                db.updateLastLogin(user.getId());

                // Resolve team
                Optional<Team> teamOpt = db.findTeamByMember(user.getId());
                if (teamOpt.isEmpty() && user.getRole() == User.Role.COACH) {
                    teamOpt = db.findTeamByCoach(user.getId());
                }

                SessionManager.getInstance().login(user, teamOpt.orElse(null));
                AppContext.getInstance().connectMqtt();

                final Optional<Team> finalTeam = teamOpt;
                Platform.runLater(() -> navigateToMain(user, finalTeam.orElse(null)));

            } catch (Exception e) {
                LOG.error("Login error", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Connection error. Please try again.");
                });
            }
        }, "auth-thread");
        authThread.setDaemon(true);
        authThread.start();
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Slide-out transition
            TranslateTransition slide = new TranslateTransition(Duration.millis(300),
                loginButton.getScene().getRoot());
            slide.setFromX(0);
            slide.setToX(-loginButton.getScene().getWidth());
            slide.setOnFinished(e -> stage.setScene(scene));
            slide.play();
        } catch (Exception e) {
            LOG.error("Navigate to register failed", e);
        }
    }

    private void navigateToMain(User user, Team team) {
        try {
            String fxmlPath = switch (user.getRole()) {
                case ATHLETE -> "/fxml/athlete/Dashboard.fxml";
                case COACH   -> "/fxml/coach/Dashboard.fxml";
                case MEDICAL -> "/fxml/medical/Dashboard.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);

            // Apply role-specific theme
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

            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Fade transition
            FadeTransition ft = new FadeTransition(Duration.millis(300),
                loginButton.getScene().getRoot());
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(scene);
                FadeTransition ftIn = new FadeTransition(Duration.millis(300), scene.getRoot());
                ftIn.setFromValue(0);
                ftIn.setToValue(1);
                ftIn.play();
            });
            ft.play();

            setLoading(false);
            LOG.info("User {} ({}) logged in", user.getName(), user.getRole());

        } catch (Exception e) {
            LOG.error("Navigate to main failed", e);
            setLoading(false);
            showError("UI loading error. Please restart the application.");
        }
    }

    private void showError(String message) {
        if (errorLabel == null) return;
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        if (loginButton != null)   loginButton.setDisable(loading);
        if (registerButton != null) registerButton.setDisable(loading);
        if (spinner != null)       spinner.setVisible(loading);
        if (emailField != null)    emailField.setDisable(loading);
        if (passwordField != null) passwordField.setDisable(loading);
    }

    private void shakeField(Control field) {
        if (field == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), field);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setFromX(0);
        tt.setToX(8);
        tt.play();
    }
}
