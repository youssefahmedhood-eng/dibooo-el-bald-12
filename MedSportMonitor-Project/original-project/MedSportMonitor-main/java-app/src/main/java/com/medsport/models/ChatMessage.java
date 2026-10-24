package com.medsport.models;

import java.time.LocalDateTime;

public class ChatMessage {

    private String id;
    private String teamId;
    private String senderId;
    private String senderName;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;

    public ChatMessage() {}

    public ChatMessage(String teamId, String senderId, String message) {
        this.teamId = teamId;
        this.senderId = senderId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public String getId()                      { return id; }
    public void setId(String id)               { this.id = id; }
    public String getTeamId()                  { return teamId; }
    public void setTeamId(String id)           { this.teamId = id; }
    public String getSenderId()                { return senderId; }
    public void setSenderId(String id)         { this.senderId = id; }
    public String getSenderName()              { return senderName; }
    public void setSenderName(String n)        { this.senderName = n; }
    public String getMessage()                 { return message; }
    public void setMessage(String m)           { this.message = m; }
    public LocalDateTime getTimestamp()        { return timestamp; }
    public void setTimestamp(LocalDateTime t)  { this.timestamp = t; }
    public boolean isRead()                    { return read; }
    public void setRead(boolean r)             { this.read = r; }
}
