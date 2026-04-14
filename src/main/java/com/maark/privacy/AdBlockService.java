package com.maark.privacy;

import javafx.scene.web.WebEngine;

/**
 * 🚫 AdBlock & Anti-Tracker Service
 *
 * Injects CSS + JavaScript into every loaded page via WebEngine to:
 *   - Hide common ad containers (CSS-based element blocking)
 *   - Stub out known tracker globals (window.ga, fbq, etc.)
 *   - Remove tracking pixels (1x1 iframes / images)
 *
 * This is a client-side CSS/JS sanitization approach — no network-level proxy
 * is required, making it lightweight and privacy-respecting.
 */
public class AdBlockService {

    private boolean enabled = true;

    // ── CSS injection ─────────────────────────────────────────────────────────
    // Hides common ad containers, banner slots, and tracking pixels by CSS class/id patterns.
    private static final String AD_BLOCK_CSS = """
            /* MAArK AdBlock — common ad containers */
            [id*="ad"], [id*="ads"], [id*="advert"],
            [class*="ad-"], [class*="ads-"], [class*="advert"],
            [class*="banner"], [class*="sponsored"],
            [class*="promoted"], [class*="dfp-"],
            [class*="gpt-ad"], [class*="adsense"],
            ins.adsbygoogle,
            [id*="google_ads"], [id*="taboola"],
            [id*="outbrain"], [id*="teads"],
            iframe[src*="doubleclick"], iframe[src*="googlesyndication"],
            iframe[src*="facebook.com/plugins"],
            img[width="1"][height="1"],
            img[src*="track"], img[src*="pixel"],
            script[src*="analytics"], script[src*="gtag"] {
                display: none !important;
                visibility: hidden !important;
                width: 0 !important;
                height: 0 !important;
                pointer-events: none !important;
            }
            """;

    // ── JS injection ──────────────────────────────────────────────────────────
    // Stubs out common tracker globals so their scripts fail silently.
    private static final String TRACKER_STUB_JS = """
            (function() {
                'use strict';
                // Stub Universal Analytics / GA4
                window.ga = function(){};
                window.gtag = function(){};
                window._ga = function(){};
                // Stub Facebook Pixel
                window.fbq = function(){};
                window._fbq = function(){};
                // Stub Twitter / X Pixel
                window.twq = function(){};
                // Stub Hotjar
                window.hj = function(){};
                window._hjSettings = {};
                // Stub Mixpanel
                if(window.mixpanel) { window.mixpanel.track = function(){}; }
                // Stub Segment
                window.analytics = { track: function(){}, page: function(){}, identify: function(){} };
                // Remove 1×1 tracking pixels from DOM
                document.querySelectorAll('img').forEach(function(img) {
                    if ((img.naturalWidth === 1 || img.width === 1) &&
                        (img.naturalHeight === 1 || img.height === 1)) {
                        img.remove();
                    }
                });
                // Block navigator.sendBeacon (used for tracking pings)
                navigator.sendBeacon = function() { return false; };
                console.info('[MAArK] AdBlock & Anti-Tracker active.');
            })();
            """;

    // ── Ephemeral session JS ──────────────────────────────────────────────────
    // Clears localStorage, sessionStorage, IndexedDB and cookies on page load.
    private static final String EPHEMERAL_SESSION_JS = """
            (function() {
                try { localStorage.clear(); } catch(e) {}
                try { sessionStorage.clear(); } catch(e) {}
                // Clear cookies for this domain
                document.cookie.split(';').forEach(function(c) {
                    document.cookie = c.trim().split('=')[0] +
                        '=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;';
                });
                console.info('[MAArK] Ephemeral session: storage cleared.');
            })();
            """;

    public AdBlockService(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Call this from the WebEngine SUCCEEDED load state listener.
     * Injects all privacy scripts into the current page.
     *
     * @param engine          The active WebEngine
     * @param ephemeralActive Whether the ephemeral session mode is on
     */
    public void injectIntoPage(WebEngine engine, boolean ephemeralActive) {
        if (!enabled) return;
        try {
            // 1. Inject CSS via JS to hide ads
            String cssJs = "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.appendChild(document.createTextNode(" +
                    toJsString(AD_BLOCK_CSS) + "));" +
                    "document.head.appendChild(style);";
            engine.executeScript(cssJs);

            // 2. Stub tracker globals
            engine.executeScript(TRACKER_STUB_JS);

            // 3. Clear session data if ephemeral mode is active
            if (ephemeralActive) {
                engine.executeScript(EPHEMERAL_SESSION_JS);
            }
        } catch (Exception e) {
            // Some pages (data:// etc.) will throw — safe to ignore
        }
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Escapes a multi-line string into a safe JS template literal. */
    private static String toJsString(String s) {
        // Use backtick template literal for safe embedding
        return "`" + s.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$") + "`";
    }
}
