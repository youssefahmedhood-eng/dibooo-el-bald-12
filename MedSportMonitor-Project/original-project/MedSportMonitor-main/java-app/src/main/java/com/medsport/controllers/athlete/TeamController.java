package com.medsport.controllers.athlete;

import com.medsport.auth.SessionManager;
import com.medsport.models.ChatMessage;
import com.medsport.services.AppContext;
import com.medsport.services.ChatService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TeamController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int POLL_INTERVAL_S = 5;

    @FXML private ScrollPane    chatScrollPane;
    @FXML private VBox          chatMessagesBox;
    @FXML private TextField     messageField;
    @FXML private Button        sendButton;
    @FXML private Label         teamNameLabel;
    @FXML private Label         teamCodeLabel;
    @FXML private Label         memberCountLabel;
    @FXML private Label         noTeamPane;

    private final SessionManager session  = SessionManager.getInstance();
    private final ChatService    chatSvc  = AppContext.getInstance().getChat();

    private Timeline pollTimeline;
    private String   lastMessageId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!session.hasTeam()) {
            show(noTeamPane);
            disable(sendButton); disable(messageField);
            return;
        }
        if (noTeamPane != null) { noTeamPane.setVisible(false); noTeamPane.setManaged(false); }

        // Team info header
        var team = session.getCurrentTeam();
        if (teamNameLabel != null) teamNameLabel.setText(team.getName());
        if (teamCodeLabel != null) teamCodeLabel.setText("Code: " + team.getJoinCode());

        // Load history
        loadMessages(50);

        // Auto-scroll on new messages
        if (chatMessagesBox != null) {
            chatMessagesBox.heightProperty().addListener((obs, o, n) -> scrollToBottom());
        }

        // Send on Enter
        if (messageField != null) {
            messageField.setOnAction(e -> handleSend());
        }

        // Poll for new messages
        pollTimeline = new Timeline(new KeyFrame(Duration.seconds(POLL_INTERVAL_S), e -> pollNew()));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    @FXML
    private void handleSend() {
        if (messageField == null || !session.hasTeam()) return;
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        messageField.clear();
        disable(sendButton);

        Thread t = new Thread(() -> {
            try {
                chatSvc.sendMessage(session.getTeamId(), session.getUserId(), text);
                Platform.runLater(() -> {
                    enable(sendButton);
                    loadMessages(50);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> enable(sendButton));
            }
        }, "chat-send");
        t.setDaemon(true);
        t.start();
    }

    private void loadMessages(int limit) {
        if (!session.hasTeam()) return;
        List<ChatMessage> msgs = chatSvc.getMessages(session.getTeamId(), limit);
        if (chatMessagesBox == null) return;
        chatMessagesBox.getChildren().clear();
        msgs.forEach(m -> chatMessagesBox.getChildren().add(buildBubble(m)));
        scrollToBottom();
        if (!msgs.isEmpty()) lastMessageId = msgs.get(msgs.size() - 1).getId();
    }

    private void pollNew() {
        if (!session.hasTeam()) return;
        List<ChatMessage> msgs = chatSvc.getMessages(session.getTeamId(), 5);
        if (msgs.isEmpty()) return;
        String newest = msgs.get(msgs.size() - 1).getId();
        if (!newest.equals(lastMessageId)) {
            Platform.runLater(() -> loadMessages(50));
        }
    }

    private HBox buildBubble(ChatMessage msg) {
        boolean isSelf = session.getUserId().equals(msg.getSenderId());

        Text textNode = new Text(msg.getMessage());
        textNode.setStyle("-fx-fill: #e6edf3;");
        textNode.wrappingWidthProperty().set(360);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(isSelf ? "chat-bubble-self" : "chat-bubble-other");

        if (!isSelf) {
            Label sender = new Label(msg.getSenderName() != null ? msg.getSenderName() : "User");
            sender.getStyleClass().add("chat-sender");
            bubble.getChildren().add(sender);
        }

        Label text = new Label(msg.getMessage());
        text.setWrapText(true);
        text.setMaxWidth(360);
        text.setStyle("-fx-text-fill: #e6edf3;");

        Label time = new Label(msg.getTimestamp() != null ? msg.getTimestamp().format(TIME_FMT) : "");
        time.getStyleClass().add("chat-time");

        bubble.getChildren().addAll(text, time);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 12, 3, 12));
        row.setAlignment(isSelf ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private void scrollToBottom() {
        if (chatScrollPane != null)
            chatScrollPane.setVvalue(1.0);
    }

    private void show(javafx.scene.Node n)    { if (n != null) { n.setVisible(true); n.setManaged(true); } }
    private void disable(javafx.scene.Node n) { if (n instanceof Control c) c.setDisable(true); }
    private void enable(javafx.scene.Node n)  { if (n instanceof Control c) c.setDisable(false); }

    public void stop() { if (pollTimeline != null) pollTimeline.stop(); }
}