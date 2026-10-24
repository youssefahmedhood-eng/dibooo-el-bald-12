package com.medsport.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SoapNote {

    private String id;
    private String athleteId;
    private String doctorId;
    private String sessionId;
    private LocalDate date;
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    private String attachmentPath;
    private boolean isPrivate;
    private LocalDateTime createdAt;

    // Transient display fields
    private String athleteName;
    private String doctorName;

    public SoapNote() {
        this.isPrivate = true;
        this.date = LocalDate.now();
    }

    public String getId()                      { return id; }
    public void setId(String id)               { this.id = id; }
    public String getAthleteId()               { return athleteId; }
    public void setAthleteId(String id)        { this.athleteId = id; }
    public String getDoctorId()                { return doctorId; }
    public void setDoctorId(String id)         { this.doctorId = id; }
    public String getSessionId()               { return sessionId; }
    public void setSessionId(String id)        { this.sessionId = id; }
    public LocalDate getDate()                 { return date; }
    public void setDate(LocalDate d)           { this.date = d; }
    public String getSubjective()              { return subjective; }
    public void setSubjective(String s)        { this.subjective = s; }
    public String getObjective()               { return objective; }
    public void setObjective(String o)         { this.objective = o; }
    public String getAssessment()              { return assessment; }
    public void setAssessment(String a)        { this.assessment = a; }
    public String getPlan()                    { return plan; }
    public void setPlan(String p)              { this.plan = p; }
    public String getAttachmentPath()          { return attachmentPath; }
    public void setAttachmentPath(String p)    { this.attachmentPath = p; }
    public boolean isPrivate()                 { return isPrivate; }
    public void setPrivate(boolean p)          { this.isPrivate = p; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }
    public String getAthleteName()             { return athleteName; }
    public void setAthleteName(String n)       { this.athleteName = n; }
    public String getDoctorName()              { return doctorName; }
    public void setDoctorName(String n)        { this.doctorName = n; }
}
