# Noor Prayer APK Releases

This folder holds the latest built Android debug APK for **Noor Prayer** (`com.aistudio.noorprayer.kxmzqp`):

- `release/NoorPrayer-debug.apk`
- `release/app-debug.apk`

## Automated GitHub Releases

Pushing an updated `.apk` in this directory to `main` or `master` (or running the workflow manually from the **Actions** tab) triggers `.github/workflows/publish-release.yml`, which automatically uploads the APK and its `SHA256SUMS.txt` checksum to the repository's **GitHub Releases** page.

## Latest Build Highlights

- **Bottom Dynamic Island Alert Squircle**: All in-app alerts—persistent Qaza Pending (`Log 1`), locked prayer notices, status feedback, and the 4-second Undo prompt—live in a single floating squircle directly above the **Prayers** and **Analytics** dock buttons and morph smoothly in place.
- **Switchable Wave / Minimal Hero Card**: Restored the calm, multi-layered animated **Wave Card** hero as the default, with a persisted **Settings → Appearance** toggle to switch instantaneously to the borderless **Minimal Editorial** hero.
- **Dual Themes**: Seamless switching between **System Auto**, **Apple White**, and true **OLED Dark** via Settings or the header quick toggle.
- **Bundled Editorial Typography**: Locally bundled **DM Sans**, **IBM Plex Mono** (tabular numerals), and **Amiri** Arabic calligraphy.
- **Astronomical Accuracy**: Muslim World League (MWL 18°/17°) calculation, true solar Islamic midnight tracking (keeping late-night Isha active until Fajr), Friday Jumu'ah override, and collapsible Makruh solar warnings.
- **Smart Multi-Stage Notifications**: Pre-Adhan, Pre-Jama'ah, Post-Jama'ah follow-ups, urgent end-of-window alerts, per-tier custom audio preview, Masjid Auto-Silence (15m), and notification shade quick-mark actions.
- **Habit Analytics & Qaza**: Editorial KPI strip, interactive 7-week consistency grid with historical editing, Qaza make-up counter, and unified Morning/Evening/Bedtime Athkaar trackers.

## Manual Sideload Installation

1. Download `NoorPrayer-debug.apk` (from GitHub Releases or this folder).
2. Open the APK on your Android phone (Android 7.0+).
3. Allow *Install unknown apps* if prompted by Android.
