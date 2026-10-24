package com.medsport.controllers.shared;

import com.medsport.auth.SessionManager;
import com.medsport.models.User;
import com.medsport.services.AppContext;
import com.medsport.services.ChatService;
import com.medsport.views.components.AnimatedBackground;
import com.medsport.views.components.RippleEffect;
import com.medsport.views.components.ViewTransition;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Root layout controller. Manages the sidebar nav and content area.
 * Loads role-specific views into the content pane.
 */
public class MainLayoutController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainLayoutController.class);

    @FXML private BorderPane    rootPane;
    @FXML private VBox          sidebar;
    @FXML private Button        sidebarToggle;
    @FXML private Label         userNameLabel;
    @FXML private Label         userRoleLabel;
    @FXML private StackPane     contentArea;
    @FXML private Label         chatBadge;

    // Nav buttons — athlete
    @FXML private Button navDashboard;
    @FXML private Button navSessions;
    @FXML private Button navPerformance;
    @FXML private Button navTeam;
    @FXML private Button navProfile;

    // Nav buttons — coach extras
    @FXML private Button navCoachSession;
    @FXML private Button navComparison;
    @FXML private Button navTeamMgmt;
    @FXML private Button navSimulator;

    // Nav buttons — medical extras
    @FXML private Button navHistory;
    @FXML private Button navRestrictions;
    @FXML private Button navRisk;
    @FXML private Button navReports;

    @FXML private Button logoutButton;

    private boolean sidebarCollapsed = false;
    private static final double SIDEBAR_EXPANDED = 220;
    private static final double SIDEBAR_COLLAPSED = 64;

    private final SessionManager session = SessionManager.getInstance();
    private final AppContext ctx         = AppContext.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = session.getCurrentUser();
        if (user == null) return;

        // Header
        if (userNameLabel != null) userNameLabel.setText(user.getName());
        if (userRoleLabel != null) userRoleLabel.setText(formatRole(user.getRole()));

        configureNavForRole(user.getRole());
        attachRippleToNavButtons();
        setupChatBadge();

        // Load default view
        String defaultView = switch (user.getRole()) {
            case ATHLETE -> "/fxml/athlete/Dashboard.fxml";
            case COACH   -> "/fxml/coach/Dashboard.fxml";
            case MEDICAL -> "/fxml/medical/Dashboard.fxml";
        };
        loadView(defaultView, null);

        LOG.info("MainLayout ready for {} ({})", user.getName(), user.getRole());
    }

    private void configureNavForRole(User.Role role) {
        // Hide all role-specific buttons first
        setVisible(false, navCoachSession, navComparison, navTeamMgmt, navSimulator,
                   navHistory, navRestrictions, navRisk, navReports);

        switch (role) {
            case ATHLETE -> {
                setVisible(true, navDashboard, navSessions, navPerformance, navTeam, navProfile);
                if (session.getTeamId() == null) setVisible(false, navTeam);
            }
            case COACH -> {
                setVisible(true, navDashboard, navCoachSession, navComparison,
                           navTeamMgmt, navPerformance, navSimulator);
            }
            case MEDICAL -> {
                setVisible(true, navDashboard, navHistory, navRestrictions, navRisk, navReports);
            }
        }
    }

    private void setupChatBadge() {
        if (chatBadge == null || session.getTeamId() == null) return;
        ChatService chat = ctx.getChat();
        // Poll for unread messages every 10 seconds
        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            int unread = chat.getUnreadCount(session.getTeamId(), session.getUserId());
            Platform.runLater(() -> {
                chatBadge.setVisible(unread > 0);
                chatBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            });
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
    }

    // ---- NAV HANDLERS ----

    @FXML private void showDashboard()    { loadView(dashboardPath(), "Dashboard"); }
    @FXML private void showSessions()     { loadView("/fxml/athlete/Sessions.fxml", "Sessions"); }
    @FXML private void showPerformance()  {
        String path = session.getRole() == User.Role.COACH
            ? "/fxml/coach/Performance.fxml" : "/fxml/athlete/Performance.fxml";
        loadView(path, "Performance");
    }
    @FXML private void showTeam()         { loadView("/fxml/athlete/Team.fxml", "Team"); }
    @FXML private void showProfile()      { loadView("/fxml/athlete/Profile.fxml", "Profile"); }
    @FXML private void showCoachSession() { loadView("/fxml/coach/Session.fxml", "Session"); }
    @FXML private void showComparison()   { loadView("/fxml/coach/Comparison.fxml", "Comparison"); }
    @FXML private void showTeamMgmt()     { loadView("/fxml/coach/TeamManagement.fxml", "Team"); }
    @FXML private void showSimulator()    { loadView("/fxml/coach/Simulator.fxml", "Simulator"); }
    @FXML private void showHistory()      { loadView("/fxml/medical/History.fxml", "History"); }
    @FXML private void showRestrictions() { loadView("/fxml/medical/Restrictions.fxml", "Restrictions"); }
    @FXML private void showRisk()         { loadView("/fxml/medical/Risk.fxml", "Risk"); }
    @FXML private void showReports()      { loadView("/fxml/medical/Reports.fxml", "Reports"); }

    @FXML
    private void handleLogout() {
        ctx.getMqtt().disconnect();
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            scene.getStylesheets().add(
                java.util.Objects.requireNonNull(
                    getClass().getResource("/css/base.css")).toExternalForm()
            );
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            LOG.error("Logout navigation failed", e);
        }
    }

    @FXML
    private void toggleSidebar() {
        double target = sidebarCollapsed ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;
        sidebarCollapsed = !sidebarCollapsed;

        if (sidebar != null) {
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(250),
                    new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH))
            );
            tl.play();
        }
    }

    // ---- VIEW LOADING ----

    public void loadView(String fxmlPath, String navLabel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            if (contentArea == null) return;

            Node current = contentArea.getChildren().isEmpty()
                ? null : contentArea.getChildren().get(0);

            if (current != null) {
                FadeTransition ftOut = new FadeTransition(Duration.millis(100), current);
                ftOut.setToValue(0);
                ftOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(view);
                    ViewTransition.slideUp(view);
                });
                ftOut.play();
            } else {
                contentArea.getChildren().setAll(view);
                ViewTransition.slideUp(view);
            }

        } catch (Exception e) {
            LOG.error("Failed to load view: {}", fxmlPath, e);
        }
    }

    private void attachRippleToNavButtons() {
        Color navRipple = Color.web("#ffffff", 0.15);
        Button[] navBtns = {navDashboard, navSessions, navPerformance, navTeam, navProfile,
            navCoachSession, navComparison, navTeamMgmt, navSimulator,
            navHistory, navRestrictions, navRisk, navReports};
        for (Button b : navBtns) {
            if (b != null) {
                StackPane sp = new StackPane(b);
                sp.setPickOnBounds(false);
                // Nav buttons aren't inside a StackPane already — use direct scale effect instead
                b.setOnMousePressed(e -> b.setScaleX(0.96));
                b.setOnMouseReleased(e -> {
                    b.setScaleX(1.0);
                    ScaleTransition st = new ScaleTransition(Duration.millis(120), b);
                    st.setFromX(0.96); st.setToX(1.0);
                    st.setInterpolator(Interpolator.EASE_OUT);
                    st.play();
                });
            }
        }
    }

    // ---- HELPERS ----

    private String dashboardPath() {
        return switch (session.getRole()) {
            case ATHLETE -> "/fxml/athlete/Dashboard.fxml";
            case COACH   -> "/fxml/coach/Dashboard.fxml";
            case MEDICAL -> "/fxml/medical/Dashboard.fxml";
        };
    }

    private void setVisible(boolean visible, Node... nodes) {
        for (Node n : nodes) {
            if (n != null) {
                n.setVisible(visible);
                n.setManaged(visible);
            }
        }
    }

    private String formatRole(User.Role role) {
        return switch (role) {
            case ATHLETE -> "Athlete";
            case COACH   -> "Coach";
            case MEDICAL -> "Medical Staff";
        };
    }
}
