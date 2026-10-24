package com.medsport.models;

import java.time.LocalDateTime;

public class MedicalRecord {

    public enum FieldType { PATHOLOGY, ALLERGY, MEDICATION, EMERGENCY_CONTACT, BLOOD_TYPE }

    private String id;
    private String athleteId;
    private String doctorId;
    private FieldType fieldType;
    private String fieldValue;
    private String severity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MedicalRecord() {}

    public String getId()                      { return id; }
    public void setId(String id)               { this.id = id; }
    public String getAthleteId()               { return athleteId; }
    public void setAthleteId(String id)        { this.athleteId = id; }
    public String getDoctorId()                { return doctorId; }
    public void setDoctorId(String id)         { this.doctorId = id; }
    public FieldType getFieldType()            { return fieldType; }
    public void setFieldType(FieldType t)      { this.fieldType = t; }
    public String getFieldValue()              { return fieldValue; }
    public void setFieldValue(String v)        { this.fieldValue = v; }
    public String getSeverity()                { return severity; }
    public void setSeverity(String s)          { this.severity = s; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }
}
