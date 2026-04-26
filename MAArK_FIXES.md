# 🛡️ MAArK Evolution: Deep Technical Documentation

MAArK is a high-fidelity, production-grade browser designed with an uncompromising focus on privacy and intelligence. This document provides a granular, "deep-dive" analysis of the critical technical issues resolved throughout the project's development.

---

## 1. 📡 Network & Proxy Architecture

### 1.1 Inconsistent IP Masking & Routing Hangs
- **issue**: Occasional IP exposure and navigation hangs when routing through SOCKS5/HTTP nodes.
- **reason**: Public proxy nodes are inherently volatile. Static selection led to "dead-end" requests, while a lack of real-time verification meant users didn't know if their shield was actually working.
- **fix**: Implemented a **Triple-Tier IP Management System**:
    - **Elite Proxy Scraping**: On every application startup, the engine fetches a fresh list of **Elite Anonymity** proxies from `proxyscrape.com`. This ensures we only use high-tier nodes that don't leak original headers.
    - **Real-Time Identity Verification**: Integrated the `ipwho.is` API directly into the Privacy Dashboard. This provides a "live proof" of the masked IP, showing the specific City, Country, and ISP of the proxy node rather than the user's real location.
    - **Per-Tab Session Isolation**: Leveraged Electron's `partition` system to ensure that every tab can hold its own unique proxy configuration. This prevents "cross-session linkage" where one tab's activity could be associated with another via a shared IP.
    - **TCP Heartbeat**: A pre-flight reachability check using a 2000ms timeout socket probe to prune dead nodes before they are assigned.

### 1.2 JVM-Level WebKit Proxy Leakage
- **issue**: The browser's internal API calls were masked, but the `WebView` (WebKit) component continued to leak the real client IP.
- **reason**: JavaFX `WebEngine` defaults to the system-level network stack. It does not automatically inherit `HttpClient` proxy configurations.
- **fix**: Forced **JVM-wide Proxy Injection** via `System.setProperty` (`socksProxyHost`, `socksProxyPort`). This ensures the underlying WebKit engine is fully encapsulated within the proxy tunnel.

---

## 2. 🎭 Privacy & Identity Sanitization

### 2.1 Fingerprint Uniqueness via User-Agent
- **issue**: Using the default browser UA made the client easily identifiable via header inspection.
- **reason**: The default JavaFX UA is distinct and rare, acting as a "beacon" for fingerprinting scripts.
- **fix**: Built a **UA Normalization Pool** that rotates between top-tier generic Chrome and Firefox strings. This blends the client into the "noise" of the most common internet traffic, significantly reducing the entropy of the browser fingerprint.

### 2.2 Behavioral Biometric Tracking
- **issue**: Websites tracking user-specific physical interactions (click speed, scroll rhythm).
- **reason**: Advanced fingerprinting scripts monitor `mousemove` and `scroll` events to create a behavioral ID that survives even IP and cookie clearing.
- **fix**: Developed a **Behavioral Telemetry Dashboard** that monitors these metrics in real-time. It provides the user with visibility into the "invisible" data they expose and includes a **Mock Injection Shield** to poison tracking scripts with randomized hardware concurrency and memory metadata.

---

## 3. 🚫 Content Filtering & Ad-Blocking

### 3.1 Dynamic Ad-Segment Leakage (YouTube)
- **issue**: Preroll and midroll ads appearing despite standard URL filters.
- **reason**: Modern ad-tech serves video segments from the same dynamic CDN as the content, making simple domain blacklisting ineffective.
- **fix**: Implemented a **Deep Packet Inspection (DPI) lifecycle** at the `onBeforeRequest` stage. By identifying specific encrypted ad-segment patterns, the engine neutralizes the ad payloads while maintaining the integrity of the primary video stream.

### 3.2 Invisible Tracking Pixels & 1x1 Iframes
- **issue**: Privacy-invasive trackers loading via microscopic 1x1 elements.
- **reason**: These elements are often dynamically injected by scripts after the initial page load, bypassing static HTML filters.
- **fix**: Integrated a **Mutation-Aware DOM Sanitizer** that:
    - Automatically removes images and iframes with 1x1 dimensions.
    - Stubs out global tracker objects (e.g., `window.ga`, `fbq`, `mixpanel`) to prevent script execution.
    - Disables `navigator.sendBeacon` to block background tracking pings.

---

## 4. 💾 Data Persistence & Profile Isolation

### 4.1 Volatile History & Search Loss
- **issue**: Total loss of user data and session history upon application restart.
- **reason**: History was maintained in a non-persistent memory cache.
- **fix**: Engineered an **Atomic JSON Persistence Engine** (`HistoryManager`). It uses profile-based serialization to write `browse_history.json` and `search_history.json` in real-time, ensuring data survives crashes and restarts.

### 4.2 Multi-Profile Data Leakage
- **issue**: Cookies and site data from a "Work" profile appearing in a "Personal" profile.
- **reason**: Use of a shared global cache directory for all browser instances.
- **fix**: Implemented **Filesystem-Level Profile Sandboxing**. Each profile (Personal, Work, Guest) is assigned a unique, encrypted subdirectory for its cache, IndexedDB, and localStorage, ensuring absolute session isolation.

---

## 5. 🔍 Search & UX Engineering

### 5.1 Address Bar "Enter" Key Failure
- **issue**: Navigation required a manual button click; pressing 'Enter' did nothing.
- **reason**: Missing keyboard event listeners in the Electron-to-Java IPC bridge.
- **fix**: Standardized the **Global Search Controller** with unified keyboard event hooks. Integrated **Startpage** as the default provider, combining Google-tier results with strict privacy-preserving proxying for all queries.

---

## 6. 🚨 Security & Breach Monitoring

### 6.1 Compromised Credential Awareness
- **issue**: Users unknowingly logging into sites where their passwords had been previously leaked.
- **reason**: No proactive checks against known data breach repositories.
- **fix**: Developed the **Breach Alert Service**. It uses a heuristic pattern-matching engine to detect login pages and cross-references the domain against the **HaveIBeenPwned (HIBP)** database. If a breach is found, a non-intrusive JavaFX alert is fired once per session to warn the user.

---
*Deep Technical Audit for the MAArK Project — Complete.*
