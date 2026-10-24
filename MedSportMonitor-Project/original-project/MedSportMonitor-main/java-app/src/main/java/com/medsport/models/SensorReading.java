package com.medsport.models;

import java.time.Instant;

public class SensorReading {

    public enum SensorType {
        HR, SPO2, GPS_LAT, GPS_LON, GPS_SPEED, GPS_ALT,
        ACCEL_X, ACCEL_Y, ACCEL_Z, CADENCE;

        public String toDbString() { return name().toLowerCase(); }

        public static SensorType fromString(String s) {
            return switch (s.toLowerCase()) {
                case "heart_rate", "hr"    -> HR;
                case "spo2", "blood_oxygen" -> SPO2;
                case "gps_lat"             -> GPS_LAT;
                case "gps_lon"             -> GPS_LON;
                case "gps_speed", "speed"  -> GPS_SPEED;
                case "gps_alt", "altitude" -> GPS_ALT;
                case "accel_x"             -> ACCEL_X;
                case "accel_y"             -> ACCEL_Y;
                case "accel_z"             -> ACCEL_Z;
                case "cadence"             -> CADENCE;
                default                    -> valueOf(s.toUpperCase());
            };
        }
    }

    private long id;
    private String athleteId;
    private String sessionId;
    private Instant timestamp;
    private SensorType sensorType;
    private double value;
    private String unit;
    private double quality;

    // MQTT payload fields (transient)
    private String teamId;

    public SensorReading() {
        this.quality = 1.0;
    }

    public SensorReading(String athleteId, String sessionId, Instant timestamp,
                         SensorType type, double value, String unit) {
        this();
        this.athleteId = athleteId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.sensorType = type;
        this.value = value;
        this.unit = unit;
    }

    public long getId()                       { return id; }
    public void setId(long id)                { this.id = id; }
    public String getAthleteId()              { return athleteId; }
    public void setAthleteId(String id)       { this.athleteId = id; }
    public String getSessionId()              { return sessionId; }
    public void setSessionId(String id)       { this.sessionId = id; }
    public Instant getTimestamp()             { return timestamp; }
    public void setTimestamp(Instant t)       { this.timestamp = t; }
    public SensorType getSensorType()         { return sensorType; }
    public void setSensorType(SensorType t)   { this.sensorType = t; }
    public double getValue()                  { return value; }
    public void setValue(double v)            { this.value = v; }
    public String getUnit()                   { return unit; }
    public void setUnit(String u)             { this.unit = u; }
    public double getQuality()                { return quality; }
    public void setQuality(double q)          { this.quality = q; }
    public String getTeamId()                 { return teamId; }
    public void setTeamId(String id)          { this.teamId = id; }
}
