package com.medsport.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Async HTTP client for the FastAPI analytics microservice (localhost:8000).
 * All methods return CompletableFuture so callers stay non-blocking.
 * Falls back to empty/placeholder data when the API is unreachable.
 */
public class AnalyticsRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsRestClient.class);
    private static final String BASE = "http://localhost:8000/api";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http;
    private final ObjectMapper mapper;

    private boolean lastHealthOk = false;

    public AnalyticsRestClient() {
        this.http   = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        this.mapper = new ObjectMapper();
        // Probe health asynchronously so startup stays fast
        checkHealthAsync();
    }

    // ── Health ────────────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> checkHealth() { return checkHealthAsync(); }

    public CompletableFuture<Boolean> checkHealthAsync() {
        return get("/health")
            .thenApply(node -> {
                lastHealthOk = node != null && "ok".equals(node.path("status").asText());
                return lastHealthOk;
            })
            .exceptionally(e -> { lastHealthOk = false; return false; });
    }

    public boolean isApiOnline() { return lastHealthOk; }

    // ── Heart Zones ───────────────────────────────────────────────────────────

    /**
     * POST /api/analysis/heart-zones
     * Returns node with zone_distribution, time_in_zones, avg_hr, max_hr, hrv fields.
     */
    public CompletableFuture<JsonNode> analyzeHeartZones(
            String athleteId, List<Double> hrSeries, double hrMax, double restingHr) {

        ObjectNode body = mapper.createObjectNode();
        body.put("athlete_id", athleteId);
        body.put("hr_max", hrMax);
        body.put("resting_hr", restingHr);
        body.put("sample_interval_s", 1.0);
        ArrayNode arr = body.putArray("hr_series");
        hrSeries.forEach(arr::add);
        return post("/analysis/heart-zones", body);
    }

    // ── Radar Metrics ─────────────────────────────────────────────────────────

    /**
     * POST /api/analysis/radar-metrics
     * Returns node with metrics: {endurance, speed, recovery, consistency, load, intensity}
     */
    public CompletableFuture<JsonNode> radarMetrics(String athleteId, List<Map<String,Object>> sessions) {
        ObjectNode body = mapper.createObjectNode();
        body.put("athlete_id", athleteId);
        body.set("sessions", mapper.valueToTree(sessions));
        return post("/analysis/radar-metrics", body);
    }

    // ── Fatigue Index ─────────────────────────────────────────────────────────

    /**
     * POST /api/analysis/fatigue-index
     * Returns node with fatigue_index (0-1), acr (Acute:Chronic Ratio), classification.
     */
    public CompletableFuture<JsonNode> fatigueIndex(String athleteId, List<Map<String,Object>> last7Days) {
        ObjectNode body = mapper.createObjectNode();
        body.put("athlete_id", athleteId);
        body.set("last_7_days_sessions", mapper.valueToTree(last7Days));
        return post("/analysis/fatigue-index", body);
    }

    // ── Cardiovascular Risk ───────────────────────────────────────────────────

    /**
     * POST /api/analysis/cardiovascular-risk
     * Returns node with risk_score (0-1), risk_level (LOW/MODERATE/HIGH), factors[].
     */
    public CompletableFuture<JsonNode> cardiovascularRisk(
            String athleteId, Map<String,Object> biometrics, List<Map<String,Object>> sessions) {
        ObjectNode body = mapper.createObjectNode();
        body.put("athlete_id", athleteId);
        body.set("biometrics", mapper.valueToTree(biometrics));
        body.set("last_sessions", mapper.valueToTree(sessions));
        return post("/analysis/cardiovascular-risk", body);
    }

    // ── Training Load ─────────────────────────────────────────────────────────

    /**
     * POST /api/analysis/training-load
     * Returns node with per-athlete load, team_avg_load, overload_flags[].
     */
    public CompletableFuture<JsonNode> teamTrainingLoad(
            String teamId, List<Map<String,Object>> athletes, int periodDays) {
        ObjectNode body = mapper.createObjectNode();
        body.put("team_id", teamId);
        body.put("period_days", periodDays);
        body.set("athletes", mapper.valueToTree(athletes));
        return post("/analysis/training-load", body);
    }

    // ── Session Summary ───────────────────────────────────────────────────────

    /**
     * POST /api/analysis/session-summary
     * Returns avg_hr, max_hr, avg_speed_kmh, zones, hrv.
     */
    public CompletableFuture<JsonNode> sessionSummary(String sessionId, Map<String,Object> sensorData) {
        ObjectNode body = mapper.createObjectNode();
        body.put("session_id", sessionId);
        body.set("all_sensor_data", mapper.valueToTree(sensorData));
        return post("/analysis/session-summary", body);
    }

    // ── PDF Reports ───────────────────────────────────────────────────────────

    /**
     * POST /api/reports/generate-athlete-pdf
     * Returns {pdf_base64, size_bytes} — decode pdf_base64 and write to file.
     */
    public CompletableFuture<byte[]> generateAthletePdf(
            String athleteId, Map<String,Object> athleteInfo,
            Map<String,String> teamInfo, Map<String,String> doctorInfo,
            List<Map<String,Object>> sessions, List<Map<String,Object>> soapNotes,
            Map<String,Object> restriction, Map<String,Object> riskData, String period) {

        ObjectNode body = mapper.createObjectNode();
        body.put("athlete_id", athleteId);
        body.put("period", period != null ? period : "Last 30 days");
        body.put("include_soap", true);
        body.set("athlete_info", mapper.valueToTree(athleteInfo));
        body.set("team_info",    mapper.valueToTree(teamInfo));
        body.set("doctor_info",  mapper.valueToTree(doctorInfo));
        body.set("sessions",     mapper.valueToTree(sessions));
        body.set("soap_notes",   mapper.valueToTree(soapNotes));
        if (restriction != null) body.set("restriction", mapper.valueToTree(restriction));
        if (riskData    != null) body.set("risk_data",   mapper.valueToTree(riskData));

        return post("/reports/generate-athlete-pdf", body)
            .thenApply(node -> {
                if (node == null || !node.has("pdf_base64")) return null;
                return java.util.Base64.getDecoder().decode(node.get("pdf_base64").asText());
            });
    }

    // ── Low-level HTTP helpers ────────────────────────────────────────────────

    private CompletableFuture<JsonNode> get(String path) {
        HttpRequest req;
        try {
            req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(TIMEOUT)
                .GET().build();
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(r -> {
                try {
                    if (r.statusCode() != 200) return null;
                    return mapper.readTree(r.body());
                } catch (Exception e) { return null; }
            })
            .exceptionally(e -> { LOG.debug("GET {} failed: {}", path, e.getMessage()); return null; });
    }

    private CompletableFuture<JsonNode> post(String path, JsonNode body) {
        HttpRequest req;
        try {
            req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(r -> {
                try {
                    if (r.statusCode() != 200) { LOG.warn("POST {} → {}", path, r.statusCode()); return null; }
                    return mapper.readTree(r.body());
                } catch (Exception e) { return null; }
            })
            .exceptionally(e -> { LOG.debug("POST {} failed: {}", path, e.getMessage()); return null; });
    }
}