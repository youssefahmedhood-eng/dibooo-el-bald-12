package com.medsport.models;

import java.time.LocalDateTime;

public class Alert {

    public enum Severity { CRITICAL, WARNING, ATTENTION }

    private String id;
    private String athleteId;
    private String athleteName;
    private String teamId;
    private String sessionId;
    private LocalDateTime timestamp;
    private String type;
    private Severity severity;
    private String parameter;
    private double value;
    private double threshold;
    private boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;

    public Alert() {}

    public String getSeverityLabel() {
        return severity != null ? severity.name() : "UNKNOWN";
    }

    public String getFormattedMessage() {
        return String.format("%s — %s: %.1f (threshold: %.1f)",
            athleteName != null ? athleteName : athleteId,
            parameter, value, threshold);
    }

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getAthleteId()                   { return athleteId; }
    public void setAthleteId(String id)            { this.athleteId = id; }
    public String getAthleteName()                 { return athleteName; }
    public void setAthleteName(String n)           { this.athleteName = n; }
    public String getTeamId()                      { return teamId; }
    public void setTeamId(String id)               { this.teamId = id; }
    public String getSessionId()                   { return sessionId; }
    public void setSessionId(String id)            { this.sessionId = id; }
    public LocalDateTime getTimestamp()            { return timestamp; }
    public void setTimestamp(LocalDateTime t)      { this.timestamp = t; }
    public String getType()                        { return type; }
    public void setType(String type)               { this.type = type; }
    public Severity getSeverity()                  { return severity; }
    public void setSeverity(Severity s)            { this.severity = s; }
    public String getParameter()                   { return parameter; }
    public void setParameter(String p)             { this.parameter = p; }
    public double getValue()                       { return value; }
    public void setValue(double v)                 { this.value = v; }
    public double getThreshold()                   { return threshold; }
    public void setThreshold(double t)             { this.threshold = t; }
    public boolean isAcknowledged()                { return acknowledged; }
    public void setAcknowledged(boolean a)         { this.acknowledged = a; }
    public String getAcknowledgedBy()              { return acknowledgedBy; }
    public void setAcknowledgedBy(String id)       { this.acknowledgedBy = id; }
    public LocalDateTime getAcknowledgedAt()       { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime t) { this.acknowledgedAt = t; }
}
