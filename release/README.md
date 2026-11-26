# Noor Prayer APK Releases

This folder holds the latest built Android debug APK for **Noor Prayer** (`com.aistudio.noorprayer.kxmzqp`):

- `release/NoorPrayer-debug.apk`
- `release/app-debug.apk`

## Automated GitHub Releases

Pushing an updated `.apk` in this directory to `main` or `master` (or running the workflow manually from the **Actions** tab) triggers `.github/workflows/publish-release.yml`, which automatically uploads the APK and its `SHA256SUMS.txt` checksum to the repository's **GitHub Releases** page.

## Latest Build Highlights

- **Zero-Ellipsis Responsive Layout & Display Scaling**: Eliminated all `"text..."` truncations across every screen with auto-fitting typography, a compact single-row Prayers top bar (full city & coordinates on tap), 3-column horological schedule and solar previews, and a persisted **Settings → Appearance → Display & Font Scale** toggle (**Adaptive** vs. **Fixed Scale**).
- **Full Arabic Calligraphy & Crisp Vector Icons**: Dedicated Amiri calligraphy styling (`0sp` letter spacing, generous line height) prevents clipped trailing letters (`الفجر`), and fallback Unicode pencil icons are replaced with crisp Material vector edit icons.
- **Hardened Notification & Exact-Alarm Pipeline**: Upgraded primary prayer triggers to Doze-exempt `setAlarmClock` alarms for Samsung/Android 12–15+ reliability, fixed Snooze epoch preservation, fixed Dismiss re-arm suppression, added a monochrome status-bar notification icon, auto-resynced alarms on permission return, and added a real **Send Test Notification** button in **Lock & Alert** and **Settings**.
- **Bottom Dynamic Island Alert Squircle**: All in-app alerts—persistent Qaza Pending (`Log 1`), locked prayer notices, status feedback, and the 4-second Undo prompt—live in a single floating squircle directly above the **Prayers** and **Analytics** dock buttons and morph smoothly in place.
- **Switchable Wave / Minimal Hero Card**: Calm multi-layered animated **Wave Card** hero by default, with a persisted **Settings → Appearance** toggle to switch instantaneously to the borderless **Minimal Editorial** hero.
- **Dual Themes & Bundled Typography**: Seamless switching between **System Auto**, **Apple White**, and true **OLED Dark**, paired with locally bundled **DM Sans**, **IBM Plex Mono**, and **Amiri**.
- **Astronomical Accuracy & Habit Analytics**: Muslim World League (MWL 18°/17°) calculation, true solar Islamic midnight tracking, Friday Jumu'ah override, collapsible Makruh solar warnings, interactive 7-week consistency grid, Qaza backlog, and daily Athkaar trackers.

## Manual Sideload Installation

1. Download `NoorPrayer-debug.apk` (from GitHub Releases or this folder).
2. Open the APK on your Android phone (Android 7.0+).
3. Allow *Install unknown apps* if prompted by Android.
