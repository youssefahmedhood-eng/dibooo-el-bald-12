package com.medsport.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsport.models.SensorReading;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * MQTT consumer service. Subscribes to all medsport sensor topics and dispatches
 * SensorReading objects to registered listeners.
 *
 * Topic pattern: medsport/{team_id}/{athlete_id}/{sensor_type}
 */
public class MqttService {

    private static final Logger LOG = LoggerFactory.getLogger(MqttService.class);

    public static final String BROKER_URL  = "tcp://localhost:1883";
    public static final String CLIENT_ID   = "medsport-java-" + UUID.randomUUID().toString().substring(0, 8);
    public static final String TOPIC_ROOT  = "medsport/#";

    private MqttClient client;
    private final ObjectMapper mapper;
    private final List<Consumer<SensorReading>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> mqttLogListeners = new CopyOnWriteArrayList<>();
    private boolean connected = false;

    // In-memory cache of latest readings per athlete per sensor type
    private final Map<String, Map<SensorReading.SensorType, SensorReading>> latestReadings
        = new ConcurrentHashMap<>();

    private final DatabaseService db = DatabaseService.getInstance();

    public MqttService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void connect() {
        if (connected) return;
        try {
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setConnectionTimeout(5);
            opts.setKeepAliveInterval(30);
            opts.setAutomaticReconnect(true);

            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            client.setCallback(new MqttCallbackExtended() {
                @Override public void connectionLost(Throwable cause) {
                    connected = false;
                    LOG.warn("MQTT connection lost: {}", cause.getMessage());
                }
                @Override public void messageArrived(String topic, MqttMessage msg) {
                    handleMessage(topic, new String(msg.getPayload()));
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
                @Override public void connectComplete(boolean reconnect, String uri) {
                    connected = true;
                    LOG.info("MQTT connected to {}", uri);
                    subscribeAll();
                }
            });

            client.connect(opts);
            connected = true;
            subscribeAll();
            LOG.info("MQTT service started — broker: {}", BROKER_URL);

        } catch (MqttException e) {
            connected = false;
            LOG.warn("MQTT broker not available ({}). Running without live data.", e.getMessage());
        }
    }

    private void subscribeAll() {
        try {
            client.subscribe(TOPIC_ROOT, 1);
            LOG.info("Subscribed to {}", TOPIC_ROOT);
        } catch (MqttException e) {
            LOG.error("MQTT subscribe error", e);
        }
    }

    private void handleMessage(String topic, String payload) {
        try {
            // topic: medsport/{team_id}/{athlete_id}/{sensor_type}
            String[] parts = topic.split("/");
            if (parts.length < 4) return;

            String teamId    = parts[1];
            String athleteId = parts[2];
            String sensorStr = parts[3];

            JsonNode node = mapper.readTree(payload);
            double value  = node.path("value").asDouble();
            String unit   = node.path("unit").asText("");
            double quality= node.path("quality").asDouble(1.0);
            String tsStr  = node.path("timestamp").asText(Instant.now().toString());
            Instant ts    = Instant.parse(tsStr);

            SensorReading.SensorType type;
            try {
                type = SensorReading.SensorType.fromString(sensorStr);
            } catch (IllegalArgumentException e) {
                return; // unknown sensor type — ignore
            }

            SensorReading reading = new SensorReading(athleteId, null, ts, type, value, unit);
            reading.setQuality(quality);
            reading.setTeamId(teamId);

            // Cache latest
            latestReadings.computeIfAbsent(athleteId, k -> new ConcurrentHashMap<>()).put(type, reading);

            // Persist to DB (best-effort, non-blocking)
            CompletableFuture.runAsync(() -> {
                try { db.saveSensorReading(reading); }
                catch (Exception e) { LOG.debug("Sensor save error: {}", e.getMessage()); }
            });

            // Notify UI listeners
            for (Consumer<SensorReading> l : listeners) {
                try { l.accept(reading); } catch (Exception e) { LOG.error("Sensor listener error", e); }
            }

            // Log to MQTT panel listeners
            String logLine = String.format("[%s] %s → %.2f %s", ts.toString().substring(11, 19), topic, value, unit);
            for (Consumer<String> l : mqttLogListeners) {
                try { l.accept(logLine); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            LOG.debug("Error handling MQTT message: {}", e.getMessage());
        }
    }

    public void addReadingListener(Consumer<SensorReading> listener) {
        listeners.add(listener);
    }

    public void removeReadingListener(Consumer<SensorReading> listener) {
        listeners.remove(listener);
    }

    public void addMqttLogListener(Consumer<String> listener) {
        mqttLogListeners.add(listener);
    }

    public Optional<SensorReading> getLatest(String athleteId, SensorReading.SensorType type) {
        Map<SensorReading.SensorType, SensorReading> byType = latestReadings.get(athleteId);
        if (byType == null) return Optional.empty();
        return Optional.ofNullable(byType.get(type));
    }

    public Map<SensorReading.SensorType, SensorReading> getAllLatest(String athleteId) {
        return latestReadings.getOrDefault(athleteId, Collections.emptyMap());
    }

    public boolean isConnected() { return connected && client != null && client.isConnected(); }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
                connected = false;
                LOG.info("MQTT disconnected");
            } catch (MqttException e) {
                LOG.error("MQTT disconnect error", e);
            }
        }
    }
}
