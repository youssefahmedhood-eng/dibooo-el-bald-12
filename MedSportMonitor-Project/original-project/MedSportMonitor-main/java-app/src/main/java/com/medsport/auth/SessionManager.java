package com.medsport.auth;

import com.medsport.models.Team;
import com.medsport.models.User;

/**
 * Singleton holding the current authenticated user and their team for the session lifetime.
 */
public class SessionManager {

    private static volatile SessionManager instance;

    private User currentUser;
    private Team currentTeam;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager();
            }
        }
        return instance;
    }

    public boolean isLoggedIn()                { return currentUser != null; }

    public void login(User user, Team team) {
        this.currentUser = user;
        this.currentTeam = team;
    }

    public void logout() {
        this.currentUser = null;
        this.currentTeam = null;
    }

    public User getCurrentUser()               { return currentUser; }
    public Team getCurrentTeam()               { return currentTeam; }
    public void setCurrentTeam(Team t)         { this.currentTeam = t; }

    public boolean hasTeam()                   { return currentTeam != null; }
    public String getUserId()                  { return currentUser != null ? currentUser.getId() : null; }
    public String getTeamId()                  { return currentTeam != null ? currentTeam.getId() : null; }
    public User.Role getRole()                 { return currentUser != null ? currentUser.getRole() : null; }
}
