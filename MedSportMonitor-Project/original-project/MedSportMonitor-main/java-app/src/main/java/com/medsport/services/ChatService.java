package com.medsport.services;

import com.medsport.models.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);
    private final DatabaseService db = DatabaseService.getInstance();

    // In-memory delivery for real-time updates within same app session
    private final List<Consumer<ChatMessage>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public ChatMessage sendMessage(String teamId, String senderId, String text) throws Exception {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Message cannot be empty");

        ChatMessage msg = new ChatMessage(teamId, senderId, text.trim());
        msg.setId("chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        db.saveChatMessage(msg);

        for (Consumer<ChatMessage> l : listeners) {
            try { l.accept(msg); } catch (Exception e) { LOG.error("Chat listener error", e); }
        }
        return msg;
    }

    public List<ChatMessage> getMessages(String teamId, int limit) {
        return db.findChatMessages(teamId, limit);
    }

    public int getUnreadCount(String teamId, String userId) {
        return db.countUnreadMessages(teamId, userId);
    }

    public void markRead(String teamId, String userId) {
        db.markMessagesRead(teamId, userId);
    }

    public void addListener(Consumer<ChatMessage> listener) { listeners.add(listener); }
    public void removeListener(Consumer<ChatMessage> listener) { listeners.remove(listener); }
}
