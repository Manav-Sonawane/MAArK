<p align="center">
  <img src="assets/logo.png" alt="MAArK Browser Logo" width="200" onerror="this.style.display='none'"/>
</p>

# 🎯 MAArK: Privacy-First Parallel Meta-Search Browser

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-success.svg)](https://maven.apache.org/)

MAArK is a high-performance, privacy-first meta-search engine and browser built entirely in **Java 17** and **JavaFX**. Designed to keep your data secure, MAArK searches multiple engines simultaneously without compromising your identity, aggregating results locally via a powerful multithreaded architecture.

---

## ✨ Features

### 🛡️ Privacy-First Search
- **No Tracking:** Queries are performed directly from your local machine to external APIs without intermediaries storing your search history.
- **Parallel Meta-Search:** Instantly aggregates results from top providers:
  - DuckDuckGo
  - Wikipedia
  - StackOverflow
  - Reddit
  - HackerNews

### 🚀 High-Performance Architecture
- **Shared ExecutorService Thread Pool:** Efficient parallel network requests with minimal overhead.
- **Shared HTTP Context:** Optimized `HttpClient` and Jackson `ObjectMapper` instances reduce memory footprint and improve search latency.

### 🧩 Local History Management
- **Search & Browsing History:** Complete local JSON-based history storage (`search_history.json`, `browse_history.json`).
- **Data Limits:** Automatically caps history entries to keep the application fast and lightweight.

### 🎨 Modern UI (JavaFX)
- **Built-in WebView:** Render search results and surf the web natively.
- **Dark/Light Mode:** Responsive themes.
- **Interactive Results:** Clean, list-based results rendering using custom JavaFX `ListCell` formatting.

---

## 🏗️ System Architecture

MAArK employs a modular Java-based architecture:

```text
JavaFX UI (MaarkApp, SearchController)
        │
        ▼
Service Layer (SearchService, HistoryManager)
        │
        ├─► Multi-threaded Provider Execution (ExecutorService)
        │     ├── WikipediaProvider
        │     ├── DuckDuckGoProvider
        │     ├── RedditProvider
        │     └── ...
        │
        ▼
External APIs (HTTP/2 Client)
```

---

## 🛠️ Tech Stack
- **Language:** Java 17
- **UI Framework:** JavaFX 21 (Controls, Graphics, Web)
- **Build Tool:** Maven
- **JSON Processing:** Jackson (`jackson-databind`)
- **Networking:** Java `java.net.http.HttpClient`

---

## 📦 Installation & Setup

### Prerequisites
- JDK 17+ installed on your system.
- Maven 3.6+ installed.

### Build Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/Manav-Sonawane/MAArK.git
   cd MAArK
   ```
2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

### Run Instructions
To launch the MAArK browser, run the JavaFX Maven plugin:
```bash
mvn javafx:run
```

---

## 🗺️ Roadmap
- [x] **Phase 1:** Core UI & Browser Engine Setup.
- [x] **Phase 2:** Parallel API Search Integrations.
- [x] **Phase 3:** Result Parsing & Modeling.
- [x] **Phase 4:** Local History System & Architectural Optimizations.
- [ ] **Phase 5:** Proxy-Based IP Masking & Ad-blocking at the WebView Level.
- [ ] **Phase 6:** AI Summarization Integration (Groq API).

---

## 🤝 Contributing
Contributions are welcome! Please ensure that your code adheres to our modular architecture and keeps privacy at the forefront. 
1. Fork the project.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📜 License
Distributed under the MIT License. Built for security, speed, and intelligence.
