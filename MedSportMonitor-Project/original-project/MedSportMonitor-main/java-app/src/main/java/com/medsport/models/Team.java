package com.medsport.models;

import java.time.LocalDateTime;

public class Team {

    public enum Category { PROFESSIONAL, AMATEUR, UNIVERSITY, YOUTH }

    private String id;
    private String name;
    private String sport;
    private Category category;
    private String description;
    private String logoPath;
    private String primaryColor;
    private String secondaryColor;
    private String coachId;
    private String joinCode;
    private int hrMaxReference;
    private double spo2AlertThreshold;
    private double hrAlertThreshold;
    private double weeklyLoadThreshold;
    private String emergencyProtocol;
    private LocalDateTime createdAt;

    public Team() {
        this.primaryColor = "#0A84FF";
        this.secondaryColor = "#00D4FF";
        this.hrMaxReference = 190;
        this.spo2AlertThreshold = 93.0;
        this.hrAlertThreshold = 95.0;
        this.weeklyLoadThreshold = 800.0;
        this.emergencyProtocol = "Call 112 immediately. Do not move the athlete.";
    }

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }
    public String getSport()                       { return sport; }
    public void setSport(String sport)             { this.sport = sport; }
    public Category getCategory()                  { return category; }
    public void setCategory(Category c)            { this.category = c; }
    public String getDescription()                 { return description; }
    public void setDescription(String d)           { this.description = d; }
    public String getLogoPath()                    { return logoPath; }
    public void setLogoPath(String p)              { this.logoPath = p; }
    public String getPrimaryColor()                { return primaryColor; }
    public void setPrimaryColor(String c)          { this.primaryColor = c; }
    public String getSecondaryColor()              { return secondaryColor; }
    public void setSecondaryColor(String c)        { this.secondaryColor = c; }
    public String getCoachId()                     { return coachId; }
    public void setCoachId(String id)              { this.coachId = id; }
    public String getJoinCode()                    { return joinCode; }
    public void setJoinCode(String code)           { this.joinCode = code; }
    public int getHrMaxReference()                 { return hrMaxReference; }
    public void setHrMaxReference(int v)           { this.hrMaxReference = v; }
    public double getSpo2AlertThreshold()          { return spo2AlertThreshold; }
    public void setSpo2AlertThreshold(double v)    { this.spo2AlertThreshold = v; }
    public double getHrAlertThreshold()            { return hrAlertThreshold; }
    public void setHrAlertThreshold(double v)      { this.hrAlertThreshold = v; }
    public double getWeeklyLoadThreshold()         { return weeklyLoadThreshold; }
    public void setWeeklyLoadThreshold(double v)   { this.weeklyLoadThreshold = v; }
    public String getEmergencyProtocol()           { return emergencyProtocol; }
    public void setEmergencyProtocol(String s)     { this.emergencyProtocol = s; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
}
