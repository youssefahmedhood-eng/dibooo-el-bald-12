package com.medsport.services;

import com.medsport.auth.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-wide service registry. Initialised once at startup.
 * All services are singletons accessed via this context.
 */
public final class AppContext {

    private static final Logger LOG = LoggerFactory.getLogger(AppContext.class);
    private static volatile AppContext instance;

    private final DatabaseService    database;
    private final MqttService        mqtt;
    private final AlertService       alerts;
    private final TeamService        teams;
    private final SessionService     sessions;
    private final ChatService        chat;
    private final AnalyticsRestClient analytics;
    private final PdfService         pdf;
    private final SessionManager     sessionManager;

    private AppContext() {
        database       = DatabaseService.getInstance();
        mqtt           = new MqttService();
        alerts         = new AlertService();
        teams          = new TeamService();
        sessions       = new SessionService();
        chat           = new ChatService();
        analytics      = new AnalyticsRestClient();
        pdf            = new PdfService(analytics);
        sessionManager = SessionManager.getInstance();

        // Wire MQTT readings into alert evaluation
        mqtt.addReadingListener(reading -> {
            String teamId = reading.getTeamId();
            if (teamId == null) return;
            database.findTeamById(teamId).ifPresent(team ->
                alerts.evaluate(reading, team,
                    sessions.getActiveSession(teamId)
                            .map(s -> s.getId())
                            .orElse(null))
            );
        });

        LOG.info("AppContext initialised");
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) instance = new AppContext();
            }
        }
        return instance;
    }

    public void connectMqtt() {
        mqtt.connect();
        analytics.checkHealth().thenAccept(ok ->
            LOG.info("Analytics service available: {}", ok)
        );
    }

    public void shutdown() {
        mqtt.disconnect();
        database.close();
        LOG.info("AppContext shut down");
    }

    public DatabaseService     getDatabase()  { return database; }
    public MqttService         getMqtt()      { return mqtt; }
    public AlertService        getAlerts()    { return alerts; }
    public TeamService         getTeams()     { return teams; }
    public SessionService      getSessions()  { return sessions; }
    public ChatService         getChat()      { return chat; }
    public AnalyticsRestClient getAnalytics() { return analytics; }
    public PdfService          getPdf()       { return pdf; }
    public SessionManager      getSession()   { return sessionManager; }
}
