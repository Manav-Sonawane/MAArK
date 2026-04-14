package com.maark.privacy;

import com.maark.util.SearchContext;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔴 Breach Alert Service
 *
 * When the user navigates to a login page, this service checks the domain
 * against the HaveIBeenPwned API (v3) breach database.
 *
 * If a breach is found, it fires an alert callback on the JavaFX thread with a
 * human-readable message. The alert is shown ONCE per domain per session to
 * avoid spamming the user.
 */
public class BreachAlertService {

    private static final Logger LOG = Logger.getLogger(BreachAlertService.class.getName());

    // HIBP API — requires a free API key for full access; uses public search endpoint here
    private static final String HIBP_API =
            "https://haveibeenpwned.com/api/v3/breachedaccount/";

    // Login-page detection patterns (heuristic)
    private static final Pattern LOGIN_PATTERN = Pattern.compile(
            "(login|signin|sign-in|account|auth|password|credential)",
            Pattern.CASE_INSENSITIVE
    );

    // Domain extraction
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "https?://([^/]+)");

    private final Consumer<String> alertCallback;  // UI callback (JavaFX thread safe)
    private final java.util.Set<String> checkedDomains =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "breach-alert");
                t.setDaemon(true);
                return t;
            });

    /**
     * @param alertCallback Called on the JavaFX thread when a breach is detected.
     *                      Receives a user-friendly alert message.
     */
    public BreachAlertService(Consumer<String> alertCallback) {
        this.alertCallback = alertCallback;
    }

    /**
     * Inspect the current URL. If it looks like a login page, check for breaches.
     * Call this from the WebEngine load-state listener.
     */
    public void inspectUrl(String url) {
        if (url == null || url.isBlank() || url.startsWith("data:")) return;

        String domain = extractDomain(url);
        if (domain == null) return;

        // Only check pages that look like login/auth pages
        if (!LOGIN_PATTERN.matcher(url).find()) return;

        // Don't double-check the same domain in this session
        if (checkedDomains.contains(domain)) return;
        checkedDomains.add(domain);

        scheduler.submit(() -> checkBreachAsync(domain));
    }

    /** Clears the per-session cache (call on new ephemeral session). */
    public void clearSessionCache() {
        checkedDomains.clear();
        LOG.info("BreachAlertService: session cache cleared.");
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void checkBreachAsync(String domain) {
        try {
            // HIBP v3 /breachedaccount/ endpoint — check by domain (no email needed)
            // Using the /breaches?domain= endpoint (public, no API key required)
            String encoded = URLEncoder.encode(domain, StandardCharsets.UTF_8);
            String apiUrl  = "https://haveibeenpwned.com/api/v3/breaches?domain=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "MAArK-Privacy-Browser")
                    .header("hibp-api-key", "demo") // placeholder — real key needed for full access
                    .GET()
                    .build();

            HttpResponse<String> response = SearchContext.CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // If the JSON array is non-empty, there are breaches for this domain
                if (!body.equals("[]") && !body.isBlank()) {
                    // Count approximate breach count
                    long breachCount = body.chars().filter(c -> c == '{').count();
                    String message = "⚠️ BREACH ALERT — " + domain + "\n"
                            + breachCount + " known data breach(es) detected for this site.\n"
                            + "Your credentials may have been compromised. Use a unique password!";
                    Platform.runLater(() -> alertCallback.accept(message));
                } else {
                    LOG.info("No known breaches for: " + domain);
                }
            } else if (response.statusCode() == 404) {
                LOG.info("HIBP: No breaches found for " + domain);
            } else {
                LOG.warning("HIBP API returned status " + response.statusCode() + " for " + domain);
            }
        } catch (Exception e) {
            LOG.warning("Breach check failed for " + domain + ": " + e.getMessage());
        }
    }

    private String extractDomain(String url) {
        Matcher m = DOMAIN_PATTERN.matcher(url);
        if (m.find()) {
            String host = m.group(1);
            // Remove www. prefix
            return host.startsWith("www.") ? host.substring(4) : host;
        }
        return null;
    }
}
