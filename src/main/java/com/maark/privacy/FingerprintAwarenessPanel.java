package com.maark.privacy;

import com.maark.privacy.PrivacyShieldManager.FingerprintSnapshot;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 🧩 Fingerprint Awareness Panel
 *
 * A live side-panel that gives the user transparent insight into:
 *   - Current risk score (0–100)
 *   - IP / Proxy status
 *   - Active User-Agent
 *   - Layer-by-layer protection indicators
 *
 * Refreshes every 2 seconds via a scheduled background thread.
 */
public class FingerprintAwarenessPanel extends VBox {

    private final PrivacyShieldManager shield;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "fp-awareness-refresh");
                t.setDaemon(true);
                return t;
            });

    // ── Live labels ───────────────────────────────────────────────────────────
    private final Label  riskValueLabel   = new Label("100");
    private final Label  riskLevelLabel   = new Label("EXPOSED");
    private final ProgressBar riskBar     = new ProgressBar(1.0);
    private final Label  ipStatusLabel    = new Label("Real IP");
    private final Label  uaLabel          = new Label("Real UA");
    private final Label  proxyDot         = statusDot();
    private final Label  fpDot            = statusDot();
    private final Label  ephemeralDot     = statusDot();
    private final Label  proxyLabel       = new Label("Proxy (IP Mask)");
    private final Label  fpLabel          = new Label("Fingerprint Shield");
    private final Label  ephemeralLabel   = new Label("Ephemeral Session");

    public FingerprintAwarenessPanel(PrivacyShieldManager shield) {
        this.shield = shield;

        getStyleClass().add("fp-panel");
        setPadding(new Insets(12, 10, 12, 10));
        setSpacing(8);
        setPrefWidth(230);

        Label title = new Label("🧠 Fingerprint Awareness");
        title.getStyleClass().add("fp-panel-title");

        // Risk meter section
        VBox riskSection = buildRiskSection();

        // Layer status section
        VBox layerSection = buildLayerSection();

        // Network section
        VBox netSection = buildNetSection();

        getChildren().addAll(title, riskSection, sep(), layerSection, sep(), netSection);

        // Live refresh every 2 seconds
        scheduler.scheduleAtFixedRate(this::refresh, 0, 2, TimeUnit.SECONDS);
    }

    // ── Build helpers ──────────────────────────────────────────────────────────

    private VBox buildRiskSection() {
        Label heading = sectionHeading("Risk Score");

        riskBar.setMaxWidth(Double.MAX_VALUE);
        riskBar.getStyleClass().add("risk-bar");

        HBox scoreRow = new HBox(8, riskValueLabel, riskLevelLabel);
        riskValueLabel.getStyleClass().add("risk-value");
        riskLevelLabel.getStyleClass().add("risk-level");

        VBox box = new VBox(4, heading, riskBar, scoreRow);
        box.getStyleClass().add("fp-section");
        return box;
    }

    private VBox buildLayerSection() {
        Label heading = sectionHeading("Protection Layers");

        HBox proxyRow     = new HBox(8, proxyDot,    proxyLabel);
        HBox fpRow        = new HBox(8, fpDot,        fpLabel);
        HBox ephemeralRow = new HBox(8, ephemeralDot, ephemeralLabel);

        proxyRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        fpRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        ephemeralRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        proxyLabel.getStyleClass().add("fp-layer-label");
        fpLabel.getStyleClass().add("fp-layer-label");
        ephemeralLabel.getStyleClass().add("fp-layer-label");

        VBox box = new VBox(6, heading, proxyRow, fpRow, ephemeralRow);
        box.getStyleClass().add("fp-section");
        return box;
    }

    private VBox buildNetSection() {
        Label heading = sectionHeading("Network Identity");

        Label ipTitle  = smallCap("IP / Proxy");
        Label uaTitle  = smallCap("User-Agent");

        ipStatusLabel.getStyleClass().add("fp-detail");
        ipStatusLabel.setWrapText(true);
        uaLabel.getStyleClass().add("fp-detail");
        uaLabel.setWrapText(true);
        uaLabel.setMaxWidth(200);

        VBox box = new VBox(4, heading, ipTitle, ipStatusLabel, uaTitle, uaLabel);
        box.getStyleClass().add("fp-section");
        return box;
    }

    // ── Refresh (background → Platform.runLater) ───────────────────────────────

    private void refresh() {
        FingerprintSnapshot snap = shield.getSnapshot();
        Platform.runLater(() -> applySnapshot(snap));
    }

    private void applySnapshot(FingerprintSnapshot snap) {
        int risk = snap.riskScore();

        // Risk bar — invert: low risk = green fill, high = red
        riskBar.setProgress(risk / 100.0);
        riskValueLabel.setText(String.valueOf(risk));

        if (risk <= 20) {
            riskLevelLabel.setText("PROTECTED");
            riskLevelLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            riskBar.setStyle("-fx-accent: #4CAF50;");
        } else if (risk <= 50) {
            riskLevelLabel.setText("MODERATE");
            riskLevelLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            riskBar.setStyle("-fx-accent: #FF9800;");
        } else {
            riskLevelLabel.setText("EXPOSED");
            riskLevelLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            riskBar.setStyle("-fx-accent: #F44336;");
        }

        // Layer dots
        setDot(proxyDot,    snap.proxyOn());
        setDot(fpDot,       snap.fingerprintMitigationOn());
        setDot(ephemeralDot,snap.ephemeralOn());

        // Dim / brighten layer labels
        layerLabelStyle(proxyLabel,    snap.proxyOn());
        layerLabelStyle(fpLabel,       snap.fingerprintMitigationOn());
        layerLabelStyle(ephemeralLabel,snap.ephemeralOn());

        // Network details
        ipStatusLabel.setText(snap.ipStatus());
        // Truncate long UA for display
        String ua = snap.activeUserAgent();
        uaLabel.setText(ua.length() > 55 ? ua.substring(0, 52) + "…" : ua);
    }

    private void setDot(Label dot, boolean active) {
        dot.setText(active ? "🟢" : "🔴");
    }

    private void layerLabelStyle(Label lbl, boolean active) {
        lbl.setStyle(active
                ? "-fx-text-fill: #4CAF50;"
                : "-fx-text-fill: #F44336; -fx-opacity: 0.8;");
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private static Label statusDot() {
        Label l = new Label("🔴");
        l.setStyle("-fx-font-size: 10px;");
        return l;
    }

    private static Label sectionHeading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("fp-section-heading");
        return l;
    }

    private static Label smallCap(String text) {
        Label l = new Label(text.toUpperCase());
        l.getStyleClass().add("fp-small-cap");
        return l;
    }

    private static Region sep() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.getStyleClass().add("fp-sep");
        return r;
    }

    /** Shut down the background refresh scheduler cleanly. */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
