# Noor Prayer APK Build

The installable Android APK for **Noor Prayer** (`com.aistudio.noorprayer.qmzrxk`) has been built and placed in this folder:

- **APK File**: `release/app-debug.apk`

## Latest Release Highlights
- **Full-Bleed Native Layout**: Removed the top brand bar across all 4 tabs for a clean, edge-to-edge mobile experience.
- **Tabular Monospaced Timers (`tnum`)**: Applied across all live ticking countdowns, digital clocks, and time pills to eliminate horizontal digit jitter.
- **Tactile Haptic Feedback**: Added crisp haptic feedback when marking prayers, stepping/completing Post-Salah Athkaar, switching bottom tabs, and adjusting settings.
- **Smooth Tab Transitions**: Directional horizontal slide + crossfade animations (`AnimatedContent`) when switching between bottom navigation tabs.
- **Deep Midnight Slate & Gold Theme**: Preserved the signature dark slate and radiant gold palette.
- **Bundled Calligraphic Arabic Font (`Amiri`)**: Applied to Arabic prayer titles, Hijri/Arabic day names, and Post-Salah Athkaar subtitles.
- **Live Seconds & AM/PM Pill**: Subtle `:SS` + `AM/PM` pill next to the big digital clock on Screen 1.
- **Interactive Hijri Moonsighting Offset (`-2d` to `+2d`)**: Tap the Hijri date pill on Screen 1 to adjust the local lunar date offset (`-2d`, `-1d`, `0d`, `+1d`, `+2d`), persisted via DataStore.
- **Animated Status Phrase Transitions**: Smooth vertical slide + crossfade animation when cycling between Prayer, Adhan, and Jama'ah status phrases on Screen 1.
- **Interactive Loading Bar Checkpoints**: Tap `▲ Adhan` or `▲ Jama'ah` below the Screen 1 progress bar to toggle exact formatted clock times (`▲ Adhan 05:00 AM`, `▲ Jama'ah 05:20 AM`).
- **Compact Collapsible `Alert Armed` Pill**: Minimized the bottom status pill on Screen 1; tap it to expand and access the test-fire system notification action.
- **Prayed in Jama'ah vs. Prayed Individually Tracking**:
  - Choose between **Prayed in Jama'ah** and **Prayed Individually** when marking a prayer on Screen 1 or Screen 2, or configure **Jama'ah Completion Tracking** in Settings (`Manual Choice`, `Auto ±15m`, `Auto ±20m`, `Auto ±30m` around Jama'ah time).
- **Dynamic Time-of-Day Hero Sky Gradients**: Screen 2's Current Prayer Hero Card dynamically adapts its sky gradient and atmospheric glow across **Fajr** (Dawn Indigo/Rose), **Dhuhr** (Zenith Royal Slate), **Asr** (Warm Afternoon Amber-Slate), **Maghrib** (Dusk Crimson-Indigo), and **Isha** (Starry Midnight Navy).
- **Urgent End-of-Prayer Alarm Sound**: Configurable in Settings between Device Alarm Sound, Default Notification Sound, and Custom Sound Picker.

## How to Download & Install
1. Open the file explorer in the workspace and navigate to the `release/` folder.
2. Right-click **`app-debug.apk`** and select **Download** (or use AI Studio's **Settings -> Export / Generate APK** menu).
3. Transfer the `.apk` file to your Android phone and tap it to install (enable *Install unknown apps* if prompted by Android).
