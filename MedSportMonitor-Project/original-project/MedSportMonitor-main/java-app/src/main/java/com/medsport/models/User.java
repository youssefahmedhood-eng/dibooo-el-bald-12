package com.medsport.models;

import java.time.LocalDateTime;

public class User {

    public enum Role { ATHLETE, COACH, MEDICAL }

    private String id;
    private String email;
    private String passwordHash;
    private Role role;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {}

    public User(String id, String email, String passwordHash, Role role, String name) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.name = name;
    }

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }
    public String getPasswordHash()                { return passwordHash; }
    public void setPasswordHash(String h)          { this.passwordHash = h; }
    public Role getRole()                          { return role; }
    public void setRole(Role role)                 { this.role = role; }
    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
    public LocalDateTime getLastLogin()            { return lastLogin; }
    public void setLastLogin(LocalDateTime t)      { this.lastLogin = t; }

    @Override
    public String toString() {
        return "User{id='" + id + "', email='" + email + "', role=" + role + "}";
    }
}
