module com.medsport {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.eclipse.paho.client.mqttv3;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    requires java.desktop;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;

    opens com.medsport to javafx.fxml;
    opens com.medsport.auth to javafx.fxml;
    opens com.medsport.models to javafx.fxml, com.fasterxml.jackson.databind;
    opens com.medsport.services to javafx.fxml;
    opens com.medsport.controllers.athlete to javafx.fxml;
    opens com.medsport.controllers.coach to javafx.fxml;
    opens com.medsport.controllers.medical to javafx.fxml;
    opens com.medsport.controllers.shared to javafx.fxml;
    opens com.medsport.views.components to javafx.fxml;

    exports com.medsport;
    exports com.medsport.models;
    exports com.medsport.services;
    exports com.medsport.auth;
}
