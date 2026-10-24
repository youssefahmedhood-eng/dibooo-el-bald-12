package com.medsport.models;

import java.time.LocalDateTime;

public class Session {

    public enum Status { PLANNED, ACTIVE, COMPLETED, CANCELLED }

    private String id;
    private String teamId;
    private String coachId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exerciseType;
    private int targetZoneMin;
    private int targetZoneMax;
    private String notes;
    private Status status;

    public Session() {
        this.targetZoneMin = 2;
        this.targetZoneMax = 4;
        this.status = Status.PLANNED;
    }

    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public String getId()                        { return id; }
    public void setId(String id)                 { this.id = id; }
    public String getTeamId()                    { return teamId; }
    public void setTeamId(String id)             { this.teamId = id; }
    public String getCoachId()                   { return coachId; }
    public void setCoachId(String id)            { this.coachId = id; }
    public LocalDateTime getStartTime()          { return startTime; }
    public void setStartTime(LocalDateTime t)    { this.startTime = t; }
    public LocalDateTime getEndTime()            { return endTime; }
    public void setEndTime(LocalDateTime t)      { this.endTime = t; }
    public String getExerciseType()              { return exerciseType; }
    public void setExerciseType(String t)        { this.exerciseType = t; }
    public int getTargetZoneMin()                { return targetZoneMin; }
    public void setTargetZoneMin(int z)          { this.targetZoneMin = z; }
    public int getTargetZoneMax()                { return targetZoneMax; }
    public void setTargetZoneMax(int z)          { this.targetZoneMax = z; }
    public String getNotes()                     { return notes; }
    public void setNotes(String n)               { this.notes = n; }
    public Status getStatus()                    { return status; }
    public void setStatus(Status s)              { this.status = s; }
}
