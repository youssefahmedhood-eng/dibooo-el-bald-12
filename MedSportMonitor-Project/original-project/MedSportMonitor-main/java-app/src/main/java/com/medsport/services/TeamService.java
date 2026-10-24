package com.medsport.services;

import com.medsport.models.Team;
import com.medsport.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class TeamService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamService.class);
    private final DatabaseService db = DatabaseService.getInstance();

    /** Creates a new team and adds the coach as a member. Returns the saved team. */
    public Team createTeam(Team team, String coachId) throws Exception {
        team.setId("team_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        team.setCoachId(coachId);
        team.setJoinCode(generateJoinCode());

        db.saveTeam(team);
        db.addTeamMember(team.getId(), coachId, "COACH");
        LOG.info("Team created: {} (code: {})", team.getName(), team.getJoinCode());
        return team;
    }

    /** Joins an existing team by join code. Returns the team if successful. */
    public Optional<Team> joinTeam(String joinCode, String userId, User.Role role) throws Exception {
        Optional<Team> teamOpt = db.findTeamByJoinCode(joinCode.trim().toUpperCase());
        if (teamOpt.isEmpty()) {
            LOG.warn("Join attempt with invalid code: {}", joinCode);
            return Optional.empty();
        }
        Team team = teamOpt.get();
        db.addTeamMember(team.getId(), userId, role.name());
        LOG.info("User {} joined team {} as {}", userId, team.getName(), role);
        return Optional.of(team);
    }

    /** Removes a member from their team. */
    public void leaveTeam(String teamId, String userId) throws Exception {
        db.removeTeamMember(teamId, userId);
        LOG.info("User {} left team {}", userId, teamId);
    }

    public Optional<Team> findTeamForUser(String userId) {
        return db.findTeamByMember(userId);
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        String code = sb.toString();
        // Ensure uniqueness
        if (db.findTeamByJoinCode(code).isPresent()) return generateJoinCode();
        return code;
    }
}
