package com.medsport.services;

import com.medsport.models.Athlete;
import com.medsport.models.Team;
import com.medsport.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PdfService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfService.class);
    private final AnalyticsRestClient analytics;
    private final DatabaseService db = DatabaseService.getInstance();

    public PdfService(AnalyticsRestClient analytics) {
        this.analytics = analytics;
    }

    public CompletableFuture<Optional<Path>> generateAthleteReport(
            Athlete athlete, User doctor, Team team, String period, boolean includeSoap) {

        Map<String,String> doctorInfo = new HashMap<>();
        doctorInfo.put("name", doctor.getName() != null ? doctor.getName() : "");
        Map<String,String> profile = db.findMedicalProfile(doctor.getId());
        doctorInfo.put("license",   profile.getOrDefault("license_number", ""));
        doctorInfo.put("specialty", profile.getOrDefault("specialty", "Sports Medicine"));

        Map<String,String> teamInfo = new HashMap<>();
        teamInfo.put("name",  team.getName() != null  ? team.getName()  : "");
        teamInfo.put("sport", team.getSport() != null ? team.getSport() : "");

        Map<String,Object> athleteInfo = new HashMap<>();
        athleteInfo.put("name",  athlete.getName() != null ? athlete.getName() : "");
        athleteInfo.put("sport", athlete.getSport() != null ? athlete.getSport() : "");

        List<Map<String,Object>> soapNotes = includeSoap
            ? db.findSoapNotesByAthlete(athlete.getUserId()).stream().map(n -> {
                Map<String,Object> m = new HashMap<>();
                m.put("date",       n.getDate() != null ? n.getDate().toString() : "");
                m.put("subjective", n.getSubjective() != null ? n.getSubjective() : "");
                m.put("objective",  n.getObjective()  != null ? n.getObjective()  : "");
                m.put("assessment", n.getAssessment() != null ? n.getAssessment() : "");
                m.put("plan",       n.getPlan()       != null ? n.getPlan()       : "");
                return m;
            }).toList()
            : List.of();

        return analytics.generateAthletePdf(
            athlete.getUserId(), athleteInfo, teamInfo, doctorInfo,
            List.of(), soapNotes, null, null, period)
            .thenApply(bytes -> {
                if (bytes == null || bytes.length == 0) {
                    LOG.warn("PDF generation failed — analytics service unavailable");
                    return Optional.empty();
                }
                try {
                    Path out = Paths.get(System.getProperty("user.home"), "Downloads",
                        "MedSport_Report_" + (athlete.getName() != null ? athlete.getName().replace(" ","_") : athlete.getUserId())
                        + "_" + java.time.LocalDate.now() + ".pdf");
                    Files.write(out, bytes);
                    LOG.info("PDF report saved to {}", out);
                    return Optional.of(out);
                } catch (Exception e) {
                    LOG.error("Failed to save PDF", e);
                    return Optional.empty();
                }
            });
    }
}