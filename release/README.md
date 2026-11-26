# Noor Prayer APK Releases

This folder holds the latest built Android debug APK for **Noor Prayer** (`com.aistudio.noorprayer.kxmzqp`):

- `release/NoorPrayer-debug.apk`
- `release/app-debug.apk`

## Automated GitHub Releases

Pushing an updated `.apk` in this directory to `main` or `master` (or running the workflow manually from the **Actions** tab) triggers `.github/workflows/publish-release.yml`, which automatically uploads the APK and its `SHA256SUMS.txt` checksum to the repository's **GitHub Releases** page.

## Latest Build Highlights

- **4-Step First-Time Onboarding Flow**: Guides first-time setup for **Location / Live GPS** (requests permission and opens Android Location Settings automatically if GPS is off), **Asr Madhab** (**Standard** 1× vs. **Hanafi** 2× shadow) & **Calculation Method**, **Time-Aware Prayer Catch-Up** (today's passed prayers, or yesterday's 5 prayers if first opened past midnight before Fajr), and **Analytics Starting Point** (also re-runnable anytime from Settings).
- **12 Global Calculation Methods + Manual Custom Angles**: Added **MWL**, **Karachi**, **ISNA**, **Umm al-Qura (Makkah)**, **Egyptian Authority**, **Dubai / Gulf**, **Qatar**, **Kuwait**, **Turkey (Diyanet)**, **Singapore (MUIS)**, **Tehran**, **Moonsighting**, and **Manual / Custom** Fajr & Isha twilight angles (or fixed minutes after Maghrib) with live Fajr/Asr/Isha previews across Onboarding, Settings, and background alarms.
- **Honest Analytics & Muted Prior Days**: Removed seeded dummy history; when **Ignore Prior Days** is enabled, squares prior to your start date appear distinctly muted (`–`), are excluded from Qaza/streak/consistency stats, and remain tappable to log manually anytime.
- **Zero-Ellipsis Responsive Layout & Display Scaling**: Eliminated all `"text..."` truncations across every screen with auto-fitting typography, a compact single-row Prayers top bar (full city & coordinates on tap), 3-column horological schedule and solar previews, and a persisted **Settings → Appearance → Display & Font Scale** toggle (**Adaptive** vs. **Fixed Scale**).
- **Full Arabic Calligraphy & Crisp Vector Icons**: Dedicated Amiri calligraphy styling (`0sp` letter spacing, generous line height) prevents clipped trailing letters (`الفجر`), and fallback Unicode pencil icons are replaced with crisp Material vector edit icons.
- **Hardened Notification & Exact-Alarm Pipeline**: Upgraded primary prayer triggers to Doze-exempt `setAlarmClock` alarms for Samsung/Android 12–15+ reliability, fixed Snooze epoch preservation, fixed Dismiss re-arm suppression, added a monochrome status-bar notification icon, auto-resynced alarms on permission return, and added a real **Send Test Notification** button in **Lock & Alert** and **Settings**.
- **Bottom Dynamic Island Alert Squircle**: All in-app alerts—persistent Qaza Pending (`Log 1`), locked prayer notices, status feedback, and the 4-second Undo prompt—live in a single floating squircle directly above the **Prayers** and **Analytics** dock buttons and morph smoothly in place.
- **Switchable Wave / Minimal Hero Card & Dual Themes**: Calm multi-layered animated **Wave Card** hero by default (switchable to **Minimal Editorial**), with **System Auto**, **Apple White**, and true **OLED Dark** themes paired with locally bundled **DM Sans**, **IBM Plex Mono**, and **Amiri**.

## Manual Sideload Installation

1. Download `NoorPrayer-debug.apk` (from GitHub Releases or this folder).
2. Open the APK on your Android phone (Android 7.0+).
3. Allow *Install unknown apps* if prompted by Android.
