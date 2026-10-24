package com.medsport;

import com.medsport.services.DatabaseService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Main extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseService.getInstance().initialize();
            LOG.info("Database initialized successfully");

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Login.fxml")
            );
            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm()
            );

            primaryStage.setTitle("MedSport Monitor");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);

            try {
                primaryStage.getIcons().add(
                    new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/assets/logo/medsport-logo.png")
                    ))
                );
            } catch (Exception ignored) {
                // logo not yet available — continue
            }

            primaryStage.show();
            LOG.info("MedSport Monitor started");

        } catch (Exception e) {
            LOG.error("Failed to start application", e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        LOG.info("MedSport Monitor shutting down");
        DatabaseService.getInstance().close();
    }
}
