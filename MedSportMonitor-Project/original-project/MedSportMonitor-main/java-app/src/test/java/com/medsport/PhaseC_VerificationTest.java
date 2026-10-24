package com.medsport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medsport.models.Alert;
import com.medsport.models.SensorReading;
import com.medsport.services.*;

public class PhaseC_VerificationTest {

    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;

        // 1. DatabaseService singleton
        try {
            DatabaseService db = DatabaseService.getInstance();
            pass += check("DatabaseService instantiates", db != null);
        } catch (Exception e) { System.out.println("FAIL DatabaseService: " + e.getMessage()); fail++; }

        // 2. AlertService
        try {
            AlertService as = new AlertService();
            pass += check("AlertService instantiates", as != null);
        } catch (Exception e) { System.out.println("FAIL AlertService: " + e.getMessage()); fail++; }

        // 3. MqttService (broker optional)
        try {
            MqttService mqtt = new MqttService();
            pass += check("MqttService instantiates", mqtt != null);
        } catch (Exception e) { System.out.println("FAIL MqttService: " + e.getMessage()); fail++; }

        // 4. MQTT JSON payload parsing
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String payload = "{\"timestamp\":\"2026-06-17T00:30:00Z\",\"athlete_id\":\"athlete_1\","
                + "\"team_id\":\"team_atl001\",\"value\":142.5,\"unit\":\"bpm\",\"quality\":0.97}";
            String topic = "medsport/team_atl001/athlete_1/heart_rate";
            String[] parts = topic.split("/");
            ObjectNode node = (ObjectNode) mapper.readTree(payload);
            double value = node.get("value").asDouble();
            String athleteId = node.get("athlete_id").asText();
            String teamId = node.get("team_id").asText();
            SensorReading.SensorType st = SensorReading.SensorType.fromString(parts[3]);

            pass += check("MQTT value parsed: " + value, Math.abs(value - 142.5) < 0.01);
            pass += check("SensorType HR: " + st, st == SensorReading.SensorType.HR);
            pass += check("athlete_id: " + athleteId, athleteId.equals("athlete_1"));
            pass += check("team_id: " + teamId, teamId.equals("team_atl001"));
        } catch (Exception e) { System.out.println("FAIL MQTT parsing: " + e.getMessage()); fail++; }

        // 5. Alert model severity
        try {
            Alert a = new Alert();
            a.setSeverity(Alert.Severity.CRITICAL);
            pass += check("Alert CRITICAL", a.getSeverity() == Alert.Severity.CRITICAL);
            a.setSeverity(Alert.Severity.WARNING);
            pass += check("Alert WARNING", a.getSeverity() == Alert.Severity.WARNING);
        } catch (Exception e) { System.out.println("FAIL Alert model: " + e.getMessage()); fail++; }

        // 6. TeamService + ChatService
        try {
            TeamService ts = new TeamService();
            ChatService cs = new ChatService();
            pass += check("TeamService instantiates", ts != null);
            pass += check("ChatService instantiates", cs != null);
        } catch (Exception e) { System.out.println("FAIL Services: " + e.getMessage()); fail++; }

        System.out.println();
        System.out.println("=========================================");
        System.out.printf("  Phase C: %d PASS / %d FAIL%n", pass, fail);
        System.out.println("=========================================");
        if (fail > 0) System.exit(1);
    }

    static int check(String name, boolean ok) {
        System.out.println((ok ? "  PASS" : "  FAIL") + "  " + name);
        return ok ? 1 : 0;
    }
}