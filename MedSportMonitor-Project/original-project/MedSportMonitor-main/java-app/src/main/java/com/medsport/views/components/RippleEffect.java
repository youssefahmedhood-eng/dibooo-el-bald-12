package com.medsport.views.components;

import javafx.animation.*;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Attaches a Material-Design-style ink ripple animation to any Button.
 * Call RippleEffect.attach(button) — the button's parent must be a StackPane
 * or the button itself must be wrapped in one.
 *
 * Works by overlaying a transparent StackPane clipped to the button bounds.
 */
public final class RippleEffect {

    private RippleEffect() {}

    /**
     * Attach ripple to a button. The button's graphic container becomes a StackPane
     * that renders the ripple on press.
     */
    public static void attach(Button button, Color rippleColor) {
        button.setOnMousePressed(event -> {
            Circle ripple = new Circle(0, rippleColor);
            ripple.setOpacity(0.35);
            ripple.setMouseTransparent(true);

            StackPane glass = new StackPane(ripple);
            glass.setMouseTransparent(true);
            glass.setPickOnBounds(false);
            glass.setPrefSize(button.getWidth(), button.getHeight());
            glass.setMaxSize(button.getWidth(), button.getHeight());

            // Position circle at click point relative to button centre
            double cx = event.getX() - button.getWidth() / 2;
            double cy = event.getY() - button.getHeight() / 2;
            ripple.setTranslateX(cx);
            ripple.setTranslateY(cy);

            // Insert glass overlay behind button text but inside button
            if (button.getParent() instanceof StackPane sp) {
                sp.getChildren().add(sp.getChildren().indexOf(button), glass);
                animateRipple(ripple, button, glass, sp);
            }
        });
    }

    public static void attach(Button button) {
        attach(button, Color.web("#ffffff"));
    }

    private static void animateRipple(Circle ripple, Button button, StackPane glass, StackPane parent) {
        double maxR = Math.sqrt(button.getWidth() * button.getWidth() +
                                button.getHeight() * button.getHeight()) * 0.7;

        Timeline grow = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ripple.radiusProperty(), 0),
                new KeyValue(ripple.opacityProperty(), 0.35)),
            new KeyFrame(Duration.millis(400),
                new KeyValue(ripple.radiusProperty(), maxR, Interpolator.EASE_OUT),
                new KeyValue(ripple.opacityProperty(), 0, Interpolator.EASE_IN))
        );
        grow.setOnFinished(e -> parent.getChildren().remove(glass));
        grow.play();
    }
}