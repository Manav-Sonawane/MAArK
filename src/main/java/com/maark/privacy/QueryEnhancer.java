package com.maark.privacy;

import java.util.*;

/**
 * 🧠 Rule-based Query Enhancer
 * Improves search relevance without any tracking, personalization, or external AI.
 *
 * Pipeline:
 *   1. Clean  — lowercase + strip filler words
 *   2. Detect — identify intent (Travel, Food, Tech, Shopping, General)
 *   3. Enhance — add high-signal domain keywords
 */
public class QueryEnhancer {

    // ── Filler words removed during cleaning ─────────────────────────────────
    private static final Set<String> FILLER_WORDS = Set.of(
            "to", "in", "the", "a", "an", "of", "for", "is", "are",
            "was", "were", "be", "at", "on", "with", "by", "from",
            "and", "or", "it", "that", "this", "these", "those", "how", "what"
    );

    // ── Intent signal keywords ────────────────────────────────────────────────
    private static final Map<Intent, List<String>> INTENT_SIGNALS = new EnumMap<>(Intent.class);
    // ── Enhancement suffixes appended per intent ──────────────────────────────
    private static final Map<Intent, String[]> INTENT_BOOSTS    = new EnumMap<>(Intent.class);

    public enum Intent {
        TRAVEL, FOOD, TECH, SHOPPING, CODE, HEALTH, NEWS, GENERAL
    }

    static {
        INTENT_SIGNALS.put(Intent.TRAVEL,   List.of("travel", "trip", "visit", "flight", "hotel",
                "tour", "destination", "itinerary", "vacation", "holiday"));
        INTENT_SIGNALS.put(Intent.FOOD,     List.of("recipe", "food", "cook", "dish", "meal",
                "restaurant", "cuisine", "eat", "bake", "ingredients"));
        INTENT_SIGNALS.put(Intent.TECH,     List.of("technology", "software", "hardware", "computer",
                "gadget", "device", "specification", "review", "benchmark", "tech"));
        INTENT_SIGNALS.put(Intent.SHOPPING, List.of("buy", "price", "shop", "deal", "discount",
                "offer", "product", "affordable", "best", "compare"));
        INTENT_SIGNALS.put(Intent.CODE,     List.of("code", "programming", "java", "python", "error",
                "exception", "function", "algorithm", "library", "api", "debug"));
        INTENT_SIGNALS.put(Intent.HEALTH,   List.of("symptoms", "disease", "treatment", "medicine",
                "diet", "exercise", "health", "doctor", "cure", "vitamin"));
        INTENT_SIGNALS.put(Intent.NEWS,     List.of("news", "latest", "today", "breaking", "report",
                "update", "current", "event", "headline", "announcement"));

        INTENT_BOOSTS.put(Intent.TRAVEL,   new String[]{"travel guide", "best places", "things to do"});
        INTENT_BOOSTS.put(Intent.FOOD,     new String[]{"easy recipe", "step by step", "ingredients"});
        INTENT_BOOSTS.put(Intent.TECH,     new String[]{"review specifications", "pros cons"});
        INTENT_BOOSTS.put(Intent.SHOPPING, new String[]{"best price", "buy online"});
        INTENT_BOOSTS.put(Intent.CODE,     new String[]{"solution example", "how to fix"});
        INTENT_BOOSTS.put(Intent.HEALTH,   new String[]{"symptoms causes treatment"});
        INTENT_BOOSTS.put(Intent.NEWS,     new String[]{"latest updates 2024"});
        INTENT_BOOSTS.put(Intent.GENERAL,  new String[]{"overview information"});
    }

    private final boolean enabled;

    public QueryEnhancer(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Full pipeline: clean → detect → enhance.
     * Returns the original query if the enhancer is disabled or input is blank.
     */
    public String enhance(String rawQuery) {
        if (!enabled || rawQuery == null || rawQuery.isBlank()) return rawQuery;

        String cleaned  = clean(rawQuery);
        Intent intent   = detectIntent(cleaned);
        return buildEnhanced(cleaned, intent);
    }

    /** Returns only the detected intent (for UI display). */
    public Intent detectIntent(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return Intent.GENERAL;
        String lower = rawQuery.toLowerCase();

        for (Map.Entry<Intent, List<String>> entry : INTENT_SIGNALS.entrySet()) {
            for (String signal : entry.getValue()) {
                if (lower.contains(signal)) return entry.getKey();
            }
        }
        return Intent.GENERAL;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String clean(String raw) {
        String lower = raw.trim().toLowerCase();
        String[] tokens = lower.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!FILLER_WORDS.contains(token)) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(token);
            }
        }
        return sb.isEmpty() ? lower : sb.toString();
    }

    private String buildEnhanced(String cleaned, Intent intent) {
        String[] boosts = INTENT_BOOSTS.getOrDefault(intent, INTENT_BOOSTS.get(Intent.GENERAL));
        // Pick the first boost (most generic) — keeps query compact
        return cleaned + " " + boosts[0];
    }
}
