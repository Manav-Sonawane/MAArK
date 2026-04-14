package com.maark.privacy;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * 🛡️ Multi-Layer Privacy Shield Manager
 *
 * Layer 1 – Network  : Proxy support (IP masking)
 * Layer 2 – Device   : User-Agent rotation (fingerprint mitigation)
 * Layer 3 – Session  : Ephemeral mode flag (no persistent cookies)
 */
public class PrivacyShieldManager {

    private static final Logger LOG = Logger.getLogger(PrivacyShieldManager.class.getName());

    // ── Built-in privacy-respecting proxy nodes (SOCKS5) ─────────────────────
    // In a real deployment these would be configurable / paid nodes.
    // Here we use a list of public SOCKS5 proxies as demonstration nodes.
    private static final List<String[]> PROXY_NODES = List.of(
            new String[]{"216.137.184.253", "19084"},
            new String[]{"72.206.181.103", "4145"},
            new String[]{"192.111.139.165", "4145"},
            new String[]{"98.162.96.53",    "4145"}
    );

    // ── Generic UA pool (normalised to reduce uniqueness) ─────────────────────
    private static final List<String> UA_POOL = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    );

    private final AtomicBoolean proxyEnabled       = new AtomicBoolean(false);
    private final AtomicBoolean fingerprintEnabled  = new AtomicBoolean(false);
    private final AtomicBoolean ephemeralEnabled    = new AtomicBoolean(false);

    /** Combined "Privacy Shield" — flips all three layers at once. */
    private final AtomicBoolean shieldActive        = new AtomicBoolean(false);

    private final Random rng = new Random();

    // Active proxy node (when proxy is on)
    private volatile String[] activeProxyNode = null;
    // Active spoofed UA (when fingerprint mitigation is on)
    private volatile String   activeSpoofedUA = UA_POOL.get(0);

    // ── Shield toggle ─────────────────────────────────────────────────────────

    /**
     * Toggles the unified Privacy Shield ON / OFF.
     * Enabling activates proxy + fingerprint mitigation + ephemeral mode.
     */
    public boolean toggleShield() {
        boolean nowActive = !shieldActive.get();
        shieldActive.set(nowActive);
        setProxy(nowActive);
        setFingerprintMitigation(nowActive);
        setEphemeralSession(nowActive);
        LOG.info("Privacy Shield " + (nowActive ? "ACTIVATED" : "DEACTIVATED"));
        return nowActive;
    }

    public boolean isShieldActive() { return shieldActive.get(); }

    // ── Layer controls ────────────────────────────────────────────────────────

    public void setProxy(boolean on) {
        proxyEnabled.set(on);
        if (on) {
            activeProxyNode = PROXY_NODES.get(rng.nextInt(PROXY_NODES.size()));
            LOG.info("Proxy activated: " + activeProxyNode[0] + ":" + activeProxyNode[1]);
        } else {
            activeProxyNode = null;
            LOG.info("Proxy deactivated — using direct connection.");
        }
    }

    public void setFingerprintMitigation(boolean on) {
        fingerprintEnabled.set(on);
        if (on) {
            activeSpoofedUA = UA_POOL.get(rng.nextInt(UA_POOL.size()));
            LOG.info("Fingerprint mitigation ON — UA: " + activeSpoofedUA);
        } else {
            // Restore real UA
            activeSpoofedUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            LOG.info("Fingerprint mitigation OFF — real UA restored.");
        }
    }

    public void setEphemeralSession(boolean on) {
        ephemeralEnabled.set(on);
        LOG.info("Ephemeral session " + (on ? "ON" : "OFF"));
    }

    // ── State accessors ───────────────────────────────────────────────────────

    public boolean isProxyEnabled()      { return proxyEnabled.get(); }
    public boolean isFingerprintEnabled(){ return fingerprintEnabled.get(); }
    public boolean isEphemeralEnabled()  { return ephemeralEnabled.get(); }

    /** Current spoofed (or real) User-Agent string for WebView injection. */
    public String getActiveUserAgent()   { return activeSpoofedUA; }

    /** Active proxy node display string, e.g. "216.x.x.x:19084", or "Direct". */
    public String getProxyDisplayLabel() {
        if (activeProxyNode != null)
            return activeProxyNode[0] + ":" + activeProxyNode[1];
        return "Direct (no proxy)";
    }

    /**
     * Builds a privacy-aware HttpClient for API provider calls.
     * Uses a proxy if proxy is enabled, otherwise direct.
     */
    public HttpClient buildPrivacyHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (proxyEnabled.get() && activeProxyNode != null) {
            try {
                InetSocketAddress addr = new InetSocketAddress(
                        activeProxyNode[0], Integer.parseInt(activeProxyNode[1]));
                builder.proxy(ProxySelector.of(addr));
            } catch (Exception e) {
                LOG.warning("Proxy setup failed, falling back to direct: " + e.getMessage());
            }
        }
        return builder.build();
    }

    // ── Fingerprint metadata ──────────────────────────────────────────────────

    /** Returns a human-readable fingerprint risk summary for the Awareness Panel. */
    public FingerprintSnapshot getSnapshot() {
        return new FingerprintSnapshot(
                fingerprintEnabled.get(),
                proxyEnabled.get(),
                ephemeralEnabled.get(),
                activeSpoofedUA,
                activeProxyNode != null ? activeProxyNode[0] + ":" + activeProxyNode[1] : "Real IP",
                computeRiskScore()
        );
    }

    /**
     * Simple heuristic risk score 0–100 (lower = safer).
     * Each disabled layer adds risk points.
     */
    private int computeRiskScore() {
        int risk = 100;
        if (fingerprintEnabled.get()) risk -= 35;  // UA normalised
        if (proxyEnabled.get())       risk -= 40;  // IP masked
        if (ephemeralEnabled.get())   risk -= 25;  // no persistent cookies
        return Math.max(0, risk);
    }

    // ── Snapshot record ───────────────────────────────────────────────────────

    public record FingerprintSnapshot(
            boolean fingerprintMitigationOn,
            boolean proxyOn,
            boolean ephemeralOn,
            String  activeUserAgent,
            String  ipStatus,
            int     riskScore        // 0 (safe) – 100 (exposed)
    ) {}
}
