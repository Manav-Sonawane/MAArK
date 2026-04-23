package com.maark.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maark.manager.HistoryManager;
import com.maark.model.BrowseHistoryEntry;
import com.maark.model.SearchHistoryEntry;
import com.maark.model.SearchResult;
import com.maark.privacy.PrivacyShieldManager;
import com.maark.service.SearchService;
import com.maark.provider.*;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

/**
 * MAArK Backend API Server
 * Exposes all browser logic as a local REST API on port 7070.
 * The Electron frontend communicates with this server.
 */
public class ApiServer {

    private static final int PORT = 7070;
    private static final HistoryManager historyManager = new HistoryManager();
    private static final PrivacyShieldManager privacyShield = new PrivacyShieldManager();
    private static final SearchService searchService = new SearchService(List.of(
            new StartpageProvider(),
            new WikipediaProvider(),
            new DuckDuckGoProvider(),
            new StackOverflowProvider(),
            new RedditProvider(),
            new HackerNewsProvider()
    ));

    public static void main(String[] args) {
        System.out.println("🚀 MAArK Backend starting on port " + PORT + "...");

        Javalin app = Javalin.create(config -> {
            // Allow requests from Electron renderer (localhost:5173 for dev, file:// for prod)
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
            config.useVirtualThreads = true;
        }).start(PORT);

        // ── Health check ─────────────────────────────────────────────────────────
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok", "port", PORT)));

        // ── History routes ────────────────────────────────────────────────────────
        app.get("/api/history/searches", ctx -> {
            int limit = parseIntParam(ctx, "limit", 50);
            List<SearchHistoryEntry> entries = historyManager.getRecentSearches(limit);
            ctx.json(entries);
        });

        app.get("/api/history/browses", ctx -> {
            int limit = parseIntParam(ctx, "limit", 50);
            List<BrowseHistoryEntry> entries = historyManager.getRecentBrowses(limit);
            ctx.json(entries);
        });

        app.post("/api/history/search", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String query = (String) body.get("query");
            if (query != null && !query.isBlank()) {
                historyManager.addSearch(query);
                ctx.json(Map.of("status", "saved"));
            } else {
                ctx.status(400).json(Map.of("error", "query is required"));
            }
        });

        app.post("/api/history/browse", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String url = (String) body.get("url");
            String title = (String) body.get("title");
            if (url != null && !url.isBlank()) {
                historyManager.addBrowse(url, title != null ? title : url);
                ctx.json(Map.of("status", "saved"));
            } else {
                ctx.status(400).json(Map.of("error", "url is required"));
            }
        });

        app.delete("/api/history/clear", ctx -> {
            historyManager.clearAll();
            ctx.json(Map.of("status", "cleared"));
        });

        // ── Search routes ─────────────────────────────────────────────────────────
        app.get("/api/search", ctx -> {
            String query = ctx.queryParam("q");
            if (query == null || query.isBlank()) {
                ctx.status(400).json(Map.of("error", "q param required"));
                return;
            }
            historyManager.addSearch(query);
            List<SearchResult> results = searchService.search(query);
            ctx.json(results);
        });

        app.get("/api/suggestions", ctx -> {
            String query = ctx.queryParam("q");
            if (query == null) query = "";
            String filter = query.toLowerCase();
            List<String> suggestions = historyManager.getRecentSearches(100)
                    .stream()
                    .map(SearchHistoryEntry::getQuery)
                    .filter(q -> filter.isEmpty() || q.toLowerCase().contains(filter))
                    .distinct()
                    .limit(8)
                    .toList();
            ctx.json(suggestions);
        });

        // ── Privacy routes ────────────────────────────────────────────────────────
        app.get("/api/privacy/status", ctx -> {
            PrivacyShieldManager.FingerprintSnapshot snap = privacyShield.getSnapshot();
            ctx.json(Map.of(
                    "shieldActive",    privacyShield.isShieldActive(),
                    "proxyOn",         snap.proxyOn(),
                    "fingerprintOn",   snap.fingerprintMitigationOn(),
                    "ephemeralOn",     snap.ephemeralOn(),
                    "riskScore",       snap.riskScore(),
                    "ipStatus",        snap.ipStatus(),
                    "userAgent",       snap.activeUserAgent()
            ));
        });

        app.post("/api/privacy/toggle", ctx -> {
            boolean active = privacyShield.toggleShield();
            PrivacyShieldManager.FingerprintSnapshot snap = privacyShield.getSnapshot();
            ctx.json(Map.of(
                    "shieldActive",  active,
                    "riskScore",     snap.riskScore(),
                    "ipStatus",      snap.ipStatus(),
                    "userAgent",     snap.activeUserAgent(),
                    "proxyOn",       snap.proxyOn(),
                    "fingerprintOn", snap.fingerprintMitigationOn(),
                    "ephemeralOn",   snap.ephemeralOn()
            ));
        });

        app.get("/api/privacy/useragent", ctx -> {
            ctx.json(Map.of("userAgent", privacyShield.getActiveUserAgent()));
        });

        // ── Shutdown hook ─────────────────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 MAArK Backend shutting down...");
            app.stop();
        }));

        System.out.println("✅ MAArK Backend ready at http://localhost:" + PORT);
    }

    private static int parseIntParam(Context ctx, String name, int defaultVal) {
        try {
            String val = ctx.queryParam(name);
            return val != null ? Integer.parseInt(val) : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
