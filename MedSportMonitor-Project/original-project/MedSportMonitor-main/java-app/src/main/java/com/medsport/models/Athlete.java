package com.medsport.models;

import java.time.LocalDate;

public class Athlete {

    public enum Sex { M, F, OTHER }

    private String userId;
    private LocalDate birthDate;
    private Sex sex;
    private String sport;
    private Double heightCm;
    private Double weightKg;
    private Integer hrMax;
    private Integer restingHr;
    private String photoPath;

    // Derived / transient fields
    private String name;
    private String email;

    public Athlete() {}

    public int calculateAge() {
        if (birthDate == null) return 0;
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    public int calculateHrMax() {
        if (hrMax != null && hrMax > 0) return hrMax;
        return Math.max(150, 208 - (int)(0.7 * calculateAge()));
    }

    public String getUserId()                    { return userId; }
    public void setUserId(String id)             { this.userId = id; }
    public LocalDate getBirthDate()              { return birthDate; }
    public void setBirthDate(LocalDate d)        { this.birthDate = d; }
    public Sex getSex()                          { return sex; }
    public void setSex(Sex sex)                  { this.sex = sex; }
    public String getSport()                     { return sport; }
    public void setSport(String sport)           { this.sport = sport; }
    public Double getHeightCm()                  { return heightCm; }
    public void setHeightCm(Double h)            { this.heightCm = h; }
    public Double getWeightKg()                  { return weightKg; }
    public void setWeightKg(Double w)            { this.weightKg = w; }
    public Integer getHrMax()                    { return hrMax; }
    public void setHrMax(Integer h)              { this.hrMax = h; }
    public Integer getRestingHr()                { return restingHr; }
    public void setRestingHr(Integer h)          { this.restingHr = h; }
    public String getPhotoPath()                 { return photoPath; }
    public void setPhotoPath(String p)           { this.photoPath = p; }
    public String getName()                      { return name; }
    public void setName(String n)                { this.name = n; }
    public String getEmail()                     { return email; }
    public void setEmail(String e)               { this.email = e; }
}
