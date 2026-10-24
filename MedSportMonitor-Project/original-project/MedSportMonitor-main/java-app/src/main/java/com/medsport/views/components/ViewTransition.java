package com.medsport.views.components;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Reusable entrance/exit animations for view nodes.
 * All methods are fire-and-forget (no return value needed).
 */
public final class ViewTransition {

    private ViewTransition() {}

    /** Fade + slide-up entrance — use on view root after loading */
    public static void slideUp(Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(280), node);
        tt.setFromY(18); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Fade + slide-right entrance — use for panels that appear from left */
    public static void slideRight(Node node) {
        node.setOpacity(0);
        node.setTranslateX(-14);
        FadeTransition ft = new FadeTransition(Duration.millis(260), node);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(260), node);
        tt.setFromX(-14); tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Gentle pop scale entrance for cards */
    public static void popIn(Node node) {
        node.setScaleX(0.92); node.setScaleY(0.92); node.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), node);
        st.setFromX(0.92); st.setFromY(0.92);
        st.setToX(1.0);    st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0); ft.setToValue(1);
        new ParallelTransition(st, ft).play();
    }

    /** Staggered entrance for a list of nodes — each 60 ms after the last */
    public static void staggerSlideUp(java.util.List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            n.setOpacity(0);
            n.setTranslateY(12);
            PauseTransition pause = new PauseTransition(Duration.millis(i * 60L));
            pause.setOnFinished(e -> slideUp(n));
            pause.play();
        }
    }

    /**
     * Animates a label from startVal → endVal in count-up style over 600ms.
     * Formats with the given suffix (e.g. " bpm", "%", " AU").
     */
    public static void countUp(javafx.scene.control.Label label, double startVal,
                                double endVal, String suffix, int decimals) {
        if (label == null) return;
        final int STEPS = 20;
        final long MS_PER_STEP = 30;
        String fmt = "%." + decimals + "f" + suffix;

        Timeline tl = new Timeline();
        for (int i = 1; i <= STEPS; i++) {
            final double val = startVal + (endVal - startVal) * i / (double) STEPS;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * MS_PER_STEP),
                e -> label.setText(String.format(fmt, val))));
        }
        tl.play();
    }

    /**
     * Pulse-glow effect — briefly scale up and restore. Good for alert badges.
     */
    public static void pulse(Node node, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), node);
        st.setFromX(1); st.setFromY(1);
        st.setToX(scale); st.setToY(scale);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    /** Shake animation — for invalid form fields */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setFromX(0); tt.setByX(6);
        tt.setInterpolator(Interpolator.LINEAR);
        tt.play();
    }
}