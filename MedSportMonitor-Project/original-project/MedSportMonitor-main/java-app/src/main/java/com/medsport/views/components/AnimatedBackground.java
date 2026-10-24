package com.medsport.views.components;

import javafx.animation.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated particle field Canvas for role-specific backgrounds.
 * Renders floating orbs/particles in the role accent colour — fits behind
 * login cards and Dashboard headers.
 *
 * Usage:
 *   Pane bgPane = AnimatedBackground.create(600, 400, "#0A84FF");
 *   // Add bgPane behind your content StackPane child
 */
public final class AnimatedBackground {

    private AnimatedBackground() {}

    private static final int PARTICLE_COUNT = 42;

    public static Pane create(double width, double height, String hexColor) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Color accent = Color.web(hexColor);
        List<Particle> particles = buildParticles(width, height, accent);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, width, height);

                // Gradient base
                gc.setFill(Color.web("#0d1117"));
                gc.fillRect(0, 0, width, height);

                // Draw ambient glow orbs (large, slow)
                for (Particle p : particles) {
                    p.update(width, height);
                    gc.setFill(Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), p.alpha));
                    if (p.large) {
                        // Soft orb
                        javafx.scene.effect.Bloom b = new javafx.scene.effect.Bloom();
                        double r = p.radius * 3;
                        gc.fillOval(p.x - r, p.y - r, r*2, r*2);
                    } else {
                        gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius*2, p.radius*2);
                    }
                }

                // Subtle grid
                gc.setStroke(Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.04));
                gc.setLineWidth(0.5);
                for (double x = 0; x < width; x += 40)
                    gc.strokeLine(x, 0, x, height);
                for (double y = 0; y < height; y += 40)
                    gc.strokeLine(0, y, width, y);
            }
        };
        timer.start();

        Pane pane = new Pane(canvas);
        pane.setPrefSize(width, height);
        // Stop timer when removed from scene to prevent memory leaks
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) timer.stop();
        });
        return pane;
    }

    private static List<Particle> buildParticles(double w, double h, Color accent) {
        Random rng = new Random();
        List<Particle> list = new ArrayList<>();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x      = rng.nextDouble() * w;
            p.y      = rng.nextDouble() * h;
            p.vx     = (rng.nextDouble() - 0.5) * 0.4;
            p.vy     = (rng.nextDouble() - 0.5) * 0.4;
            p.radius = 1 + rng.nextDouble() * 2.5;
            p.alpha  = 0.04 + rng.nextDouble() * 0.12;
            p.large  = i < 4; // first 4 are ambient glow orbs
            if (p.large) {
                p.radius = 40 + rng.nextDouble() * 60;
                p.alpha  = 0.03 + rng.nextDouble() * 0.05;
                p.vx *= 0.2; p.vy *= 0.2;
            }
            list.add(p);
        }
        return list;
    }

    private static class Particle {
        double x, y, vx, vy, radius, alpha;
        boolean large;

        void update(double w, double h) {
            x += vx; y += vy;
            if (x < -radius*2) x = w + radius;
            if (x > w + radius*2) x = -radius;
            if (y < -radius*2) y = h + radius;
            if (y > h + radius*2) y = -radius;
        }
    }
}