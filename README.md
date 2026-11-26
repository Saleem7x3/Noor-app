# Noor Prayer

**Noor Prayer** is a modern, minimalist Android Islamic prayer time and spiritual habit tracker crafted in Kotlin and Jetpack Compose. Designed with an **Editorial Swiss Minimal** interface, it pairs precision astronomical prayer calculations with calm typography, dual Light/OLED themes, intelligent congregation (Jama'ah) reminders, Qaza tracking, and daily Athkaar.

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![MinSDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-111111?style=flat-square)
![TargetSDK](https://img.shields.io/badge/Target%20SDK-36-111111?style=flat-square)

---

## Features

### Astronomical Prayer Engine
- **Muslim World League (MWL 18°/17°)** solar calculation engine for Fajr, Dhuhr, Asr, Maghrib, and Isha.
- **Three-Tier Schedule**: Independently configure **Prayer Start Time (PST)**, **Mosque Adhan**, and **Jama'ah (Congregation)** times using either fixed clock times or minute offsets (with keyboard and clock dial pickers).
- **Friday Jumu'ah Support**: Separate daily Dhuhr and Friday Jumu'ah timing configurations.
- **True Islamic Midnight & Late-Night Isha**: Calculates true solar Islamic midnight (midpoint between Maghrib and Fajr) and keeps unprayed Isha actionable on the hero screen until Fajr.
- **Prohibited Solar Windows (Makruh)**: Collapsible daily tracker highlighting Sunrise, Zawal (solar noon), and Sunset windows when active.

### Editorial Swiss Minimal UI, Scaling & Themes
- **Dual Theme System**: Switch seamlessly between **System Auto**, **Apple White** (warm gallery off-white `#F6F6F4` & pure white inset surfaces), and true **OLED Dark** (`#000000` canvas & obsidian surfaces), persisted via DataStore.
- **Switchable Prayer Hero Style**: Choose between the framed **Wave Card** with calm multi-layered animated solar waves (default) or the borderless **Minimal Editorial** typographic hero in **Settings → Appearance**.
- **Bottom Dynamic Island Alert Squircle**: Persistent Qaza Pending (`Log 1`), locked prayer alerts, status notices, and 4-second Undo prompts live inside a floating squircle positioned above the **Prayers** and **Analytics** navigation buttons, morphing smoothly in place.
- **Zero-Ellipsis Responsive Layout & Display Scaling**: Auto-fitting single-line headers, compact top bar (with full city & coordinates on tap), 3-column horological schedule and solar strips, and a **Display & Font Scale** toggle in Settings (**Adaptive** capped scaling vs. **Fixed Scale** locking app typography to 100% regardless of system display zoom).
- **Crafted Typography & Full Arabic Rendering**: Locally bundled **DM Sans** (editorial display & UI), **IBM Plex Mono** (tabular `tnum` horological countdowns & timestamps), and **Amiri** (Arabic calligraphy configured with full line height and zero negative tracking so sweeping glyphs like `الفجر` never clip).

### Smart Alerts & Masjid Etiquette
- **Live Lock Screen Preview & Timeline**: Live digital clock with inline seconds, interactive **Hijri moonsighting offset (`-2d` to `+2d`)**, 3-column Start/Adhan/End strip, and a checkpoint progress track for Adhan and Jama'ah.
- **Doze-Exempt Multi-Stage Notification Engine**:
  - Uses Android `AlarmManager.setAlarmClock` for primary prayer milestones so alerts fire reliably on time even under aggressive OEM Doze / battery optimization (e.g. Samsung One UI).
  - Configurable **Pre-Adhan** (`Off`, `5m`, `10m`, `15m`) and **Pre-Jama'ah** (`5m`–`30m`) alerts.
  - Periodic **Unticked Prayer Follow-Up** reminders starting +10m after Jama'ah until marked prayed, with working **5m Snooze** (preserving exact window progress) and **Dismiss** suppression for the dismissed window.
  - **Urgent End-of-Window Warnings** (`Last 15m`–`45m`) before the prayer window closes, plus automatic re-sync whenever permissions are granted on app resume.
  - Per-tier audio customization (**Alarm**, **Chime**, **Silent**, or **Custom** device ringtone/audio file) with live in-app preview, crisp monochrome status-bar icon, and a **Send Test Notification** button in **Lock & Alert** and **Settings** to verify delivery at any hour.
  - Direct notification shade actions that auto-detect Jama'ah vs. Individual within your configured window.
- **Masjid Auto-Silence**: Automatically silences sound and vibration for 15 minutes starting at Jama'ah time.

### Analytics, Qaza Backlog & Daily Athkaar
- **Editorial KPI Strip**: Instant glance at Today's goal completion, active streak, Jama'ah rate, and on-time consistency.
- **7-Week Consistency Grid**: Tap any day square to inspect or edit historical prayers (`Jama'ah`, `Individual`, `Qaza Done`, or `Missed`).
- **Qaza Make-Up Backlog**: Tracks auto-detected missed prayers and manual make-up completions, surfaced in both Analytics and the persistent bottom alert squircle.
- **Daily Athkaar**: Unified Morning, Evening, and Bedtime Athkaar trackers with tactile step counters and completion haptics.

### Worldwide Location & GPS
- Searchable built-in world cities database, live place search, custom latitude/longitude entry, and one-tap Fused Location GPS resolution.

---

## Architecture & Tech Stack

```text
MainActivity (Edge-to-Edge + Dynamic Light/Dark System Bars)
 └── NoorPrayerAppRoot (Floating Minimal Dock + 4 Screens)
      ├── NotificationLockScreenContent   (Alerts & Lock Screen Preview)
      ├── PrayerHomeScreenContent         (Open Hero, Daily Prayers, Makruh, Modals)
      ├── AnalyticsScreenContent          (KPI Strip, Qaza Backlog, 7w Grid, Athkaar)
      └── ArchitectureBlueprintScreen     (Appearance, Reminders, Audio, Masjid, GPS)
                    │
                    ▼
           PrayerHabitViewModel (StateFlow<PrayerDashboardUiState>)
          ├── PrayerTimeCalculator        (Astronomical MWL + Islamic Midnight)
          ├── PrayerHabitDatabase (Room)  (Daily logs, custom timings, Athkaar, history)
          ├── PrayerAlarmDataStore        (Preferences, theme mode, Hijri offset, intervals)
          └── PrayerAlarmScheduler        (Exact AlarmManager + BroadcastReceiver loop)
```

- **UI**: Jetpack Compose, Material 3, Edge-to-Edge WindowInsets
- **State & Concurrency**: MVVM, `ViewModel`, Kotlin Coroutines, `StateFlow`
- **Persistence**: Room (SQLite via KSP) + Jetpack DataStore Preferences
- **Location**: Google Play Services Fused Location Provider
- **Testing**: JUnit 4, Robolectric, Compose UI Test

---

## Download & Install

Pre-built Android APKs are published automatically to the **[GitHub Releases](../../releases)** page (and mirrored in the `/release` folder).

1. Go to the **Releases** section on GitHub and download the latest `NoorPrayer-*.apk`.
2. Transfer or open the `.apk` on your Android device (Android 7.0 / API 24+).
3. Allow **Install unknown apps** if prompted by Android, then install.
4. For reliable background prayer alerts on Android 12+, grant **Notifications** and **Alarms & Reminders** permissions when prompted.

---

## Building Locally

### Prerequisites
- **JDK 11** or higher
- **Android SDK** with `compileSdk 36`

### Build & Test Commands

```bash
# Compile and assemble the debug APK
gradle :app:assembleDebug

# Run JVM and Robolectric unit tests
gradle :app:testDebugUnitTest
```

The built debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and copied to `release/NoorPrayer-debug.apk`.

---

## Automated GitHub Releases

This repository includes `.github/workflows/publish-release.yml`, which automatically publishes the APK from `/release` to **GitHub Releases** along with a generated `SHA256SUMS.txt` file whenever:

1. An updated `.apk` is pushed to `release/` on `main` or `master`.
2. The workflow is triggered manually via **Actions → Publish APK to GitHub Releases → Run workflow** (with optional custom tag, release title, and pre-release toggle).
