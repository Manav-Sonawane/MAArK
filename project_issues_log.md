# MAArK Project - Comprehensive Issue Log

Below is the complete list of issues faced, diagnosed, and resolved across all development sessions for the MAArK project.

---

### Backend & API Issues

**issue:** Backend failed to compile with error `insert ")" to complete SingleMemberAnnotation`.
**reason:** A `System.out.println` statement was incorrectly placed directly inside the `SearchProvider` interface body, which is not allowed in Java. The compiler attempted to parse it as an annotation and failed.
**fix:** Removed the invalid print statement from the interface and placed it correctly inside the `search` method implementations of each provider (e.g., `DuckDuckGoProvider.java`, `WikipediaProvider.java`).

**issue:** Search history was not persisting across application restarts.
**reason:** The Jackson JSON parser could not deserialize the history files because the Model Classes (`SearchHistoryEntry`, `BrowseHistoryEntry`) lacked public no-argument constructors and proper getters/setters.
**fix:** Added default constructors and public getter/setter methods to the model classes. Updated `HistoryManager` with robust save/load logic and absolute file paths.

**issue:** `LocalSearchService` returned incorrect result snippets and URLs for local Lucene index queries.
**reason:** The `url` and `snippet` arguments were accidentally swapped when instantiating the `SearchResult` object.
**fix:** Corrected the argument order in `LocalSearchService` to match the `SearchResult` constructor `(title, url, snippet, source)`.

**issue:** Running `mvn clean compile javafx:run` failed with a cryptic build error (`MojoExecutionException`).
**reason:** The project had been migrated from a JavaFX desktop app to a Javalin REST API backend (`ApiServer`). The `javafx-maven-plugin` was left in `pom.xml` without a `<mainClass>` configured, causing the build to fail since there was no JavaFX entry point to launch.
**fix:** Cleaned up `pom.xml` by removing the unused `javafx-maven-plugin`. Instructed the build to use `mvn clean compile exec:java`. Also, killed the lingering background process on port 7070 using `taskkill` to free up the port.

**issue:** Integrating the complex privacy-centric architecture (Query Enhancer, Privacy Shield, AdBlock, etc.) caused routing and execution flow issues.
**reason:** New privacy features needed to be securely injected into the search lifecycle without blocking parallel execution or breaking existing Java backend logic.
**fix:** Integrated these components modularly into the Javalin backend APIs (`ApiServer`), updated the UI to support them, and ensured the application was correctly configured for build and execution.

---

### UI/UX Desktop (JavaFX - Legacy) Issues

**issue:** The search suggestion dropdown (`suggestionList`) was not visible when interacting with the search bar, or it took up the full width instead of looking like a dropdown overlay.
**reason:** The JavaFX `suggestionList` was initially set to `setManaged(false)`. The JavaFX layout engine ignored it, crushing it to a 0x0 size, and the `WebView` rendered on top of it, completely hiding the list.
**fix:** Explicitly set the `setPrefHeight()` for the dropdown. Changed the logic to trigger `suggestionList.setManaged(true)` only when actively showing suggestions, and brought the container `VBox` to the front (`setViewOrder(-1)`) so it successfully overlaid the center pane.

---

### Frontend (Electron + React/Vite) Issues

**issue:** Frontend build failed, preventing Vite from starting up the development server.
**reason:** The `recharts` dependency was imported in the newly integrated `PrivacyDashboard.jsx` but was not installed in `package.json`.
**fix:** Ran `npm install recharts` to add the missing dependency, allowing Vite to compile.

**issue:** Duplicate `border` key warning emitted by Vite during compilation of `PrivacyDashboard.jsx`.
**reason:** A React button style object had `border: 'none'` followed immediately by a dynamic `border: '1px solid ...'` string.
**fix:** Removed the duplicate `border: 'none'` key to resolve the React/Vite warning.

**issue:** The Electron frontend failed to connect to Vite, displaying a completely blank black screen and throwing an `ERR_FILE_NOT_FOUND` error for `dist/index.html`.
**reason:** A race condition existed where Electron launched and polled for the UI for only 5 seconds. Because Vite took longer to initialize (or had port conflicts), Electron gave up and fell back to a non-existent production build. Furthermore, the `ProfileSelector` component had a dark background, and Electron immediately created a native `BrowserView` tab on top of the UI before a profile was even selected, covering up the React elements.
**fix:** 
1. Replaced `wait-on` with a simple 4-second PowerShell startup delay in `package.json`. 
2. Increased Electron's polling retry window in `loadUI()` to 30 seconds with better port detection and proper socket draining (`res.resume()`).
3. Delayed the creation of the first `BrowserView` tab in `main.js` until the `profile:set` IPC event fired, ensuring the `ProfileSelector` UI was fully visible.
