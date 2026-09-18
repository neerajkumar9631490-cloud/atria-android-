# Atria — Professional Android Chat

Native Android client for the Atria API (`https://api.atria-asi.ai/v1/chat/completions`,
model `Atria-Dawn-Preview`), ported from `server.py`.

## Features (matches the web Pro UI)

- **Conversations** — new / rename / delete, search, grouped by Today · Yesterday · 7 days · 30 days · Older, persisted on-device
- **Streaming chat** — live SSE token rendering, Stop button, timing + model metadata
- **Professional markdown** — headers, lists, tables, quotes, inline code, dark code blocks with language label + copy button, selectable text
- **Welcome screen** — time-aware greeting + 4 suggestion cards
- **Composer** — rounded pro input, `/` command palette (`/new /clear /model /export /theme /stop /help`), send/stop FAB
- **Settings** — API key (device-only), model, system prompt, dark/light theme (Atria tokens: `#0B0E13` + `#E2A45C`)
- **Export** — share any chat as Markdown via Android Sharesheet
- **Error cards** — Retry + Open settings, same copy as the web UI

## Get the APK (phone only — no PC needed)

1. Open this repo in Chrome on your phone → **Actions** tab → tap the latest **Build APK** run →
   **Artifacts** → download **atria-debug-apk** → install it.
   - First install: Android will ask to allow “Install unknown apps” for Chrome/Files — allow it.
   - The debug APK is signed automatically by GitHub Actions. No keystore needed.
2. Open **Atria** → tap **☰ → Settings (gear)** → paste your **Atria API key** → Save.
   - Key is stored only on your device (`DataStore`) and sent only to `api.atria-asi.ai`.
3. Start chatting. Switch model anytime with `/model <id>` or in Settings.

Every push to `main` (or **Actions → Build APK → Run workflow**) builds a fresh APK.

## Project notes

- Package: `com.atria.chat` · minSdk 26 · target/compile 34 · AGP 8.5.2 · Kotlin 2.0.21 · Compose BOM 2024.10.00 · Gradle 8.7 · JDK 17
- Network: `OkHttp` SSE client (`AtriaApi.kt`) — same contract as `server.py::_upstream_events`
- Storage: `DataStore` (settings) + JSON file (conversations) — no server needed
- No API key is committed. Never commit one.
