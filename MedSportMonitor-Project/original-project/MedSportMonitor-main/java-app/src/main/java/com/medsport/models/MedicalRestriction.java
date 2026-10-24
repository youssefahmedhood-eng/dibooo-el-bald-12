package com.medsport.models;

import java.time.LocalDate;

public class MedicalRestriction {

    public enum Type { FULL, PARTIAL, HR_LIMITED }
    public enum Status { ACTIVE, RESOLVED }

    private String id;
    private String athleteId;
    private String doctorId;
    private Type type;
    private Integer hrLimit;
    private String description;
    private LocalDate startDate;
    private LocalDate returnDate;
    private Status status;

    // Transient display fields
    private String athleteName;

    public MedicalRestriction() {
        this.status = Status.ACTIVE;
        this.startDate = LocalDate.now();
    }

    public boolean isActive()                      { return status == Status.ACTIVE; }

    public String getPublicSummary() {
        return switch (type) {
            case FULL -> "Full training restriction";
            case PARTIAL -> "Partial training restriction";
            case HR_LIMITED -> "Heart rate limited to " + hrLimit + " bpm";
        };
    }

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getAthleteId()                   { return athleteId; }
    public void setAthleteId(String id)            { this.athleteId = id; }
    public String getDoctorId()                    { return doctorId; }
    public void setDoctorId(String id)             { this.doctorId = id; }
    public Type getType()                          { return type; }
    public void setType(Type t)                    { this.type = t; }
    public Integer getHrLimit()                    { return hrLimit; }
    public void setHrLimit(Integer h)              { this.hrLimit = h; }
    public String getDescription()                 { return description; }
    public void setDescription(String d)           { this.description = d; }
    public LocalDate getStartDate()                { return startDate; }
    public void setStartDate(LocalDate d)          { this.startDate = d; }
    public LocalDate getReturnDate()               { return returnDate; }
    public void setReturnDate(LocalDate d)         { this.returnDate = d; }
    public Status getStatus()                      { return status; }
    public void setStatus(Status s)                { this.status = s; }
    public String getAthleteName()                 { return athleteName; }
    public void setAthleteName(String n)           { this.athleteName = n; }
}
