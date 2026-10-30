# Noor Prayer — Hero Card Sizing, Solar/Makruh Rows, Fajr–Isha Row Polish & Snooze/Notification Sync Plan

This update restores the Hero Card's spacious proportions with a clean `"CURRENT PRAYER"` / `"NEXT PRAYER"` header, replaces the top-left badge with the current Day & Date while moving GPS into the City selector pill, adds the three solar/makruh rows (`Sun Rising`, `Top of Head Sun`, `Sun Setting`) and time-of-day icons + start-to-end time ranges to the Fajr–Isha rows, and fixes the Snooze and notification elapsed-time synchronization bugs.

## User Review & Critical Decisions

> [!IMPORTANT]
> All choices below reflect your feedback and confirmed preferences:

- **Confirmed Decision 1 — All Three Solar / Makruh Rows (`Sun Rising`, `Top of Head Sun`, `Sun Setting`)**:
  - Interleave three non-prayer solar/makruh rows in chronological order using the reference image's subtle rose/coral warning style (`WarningIcon` + start time `—————` end time):
    1. **`Sun Rising`** (after **Fajr**): `Sunrise` to `Sunrise + 15m` (e.g., `06:13 AM ————— 06:28 AM`)
    2. **`Top of Head Sun`** (before **Dhuhr / Jumu'ah**): `Dhuhr PST - 8m` to `Dhuhr PST` (e.g., `11:56 AM ————— 12:04 PM`)
    3. **`Sun Setting`** (between **Asr** and **Maghrib**): `Maghrib PST - 15m` to `Maghrib PST` (e.g., `05:39 PM ————— 05:54 PM`)
- **Confirmed Decision 2 — Improved Fajr to Isha Row Labels & Time-of-Day Icons**:
  - Each of the 5 Fardh prayer rows displays a distinctive **time-of-day sky icon** on the left (Dawn cloud for Fajr, Sun for Jumu'ah/Dhuhr, Afternoon sun/cloud for Asr, Sunset horizon for Maghrib, Crescent moon for Isha) and the **Start–End prayer window range** (e.g., `03:23 PM – 05:53 PM`) underneath the prayer name, while keeping the interactive **Adhan** and **Jama'ah** pills and completion button on the right.
- **Confirmed Decision 3 — Top Bar Layout (Day & Date Left Pill + GPS Inside City Pill)**:
  - **Left Pill**: Displays the **Day and Date** (e.g., `Friday, 30 Oct`).
  - **Right Pill**: Displays the **GPS / Location icon + City Name** (e.g., `[GPS Icon] Mumbai ▾`), with one-tap Live GPS detection inside the location selector.
- **Confirmed Decision 4 — Hero Card Proportions & Header**:
  - Restore larger, more breathable Hero Card vertical padding and typography.
  - Simplify the top-left header label to **`"CURRENT PRAYER"`** (when in an active prayer window) or **`"NEXT PRAYER"`** (when waiting for the next prayer) so it never clips or truncates (`JAMA'AH STA...`).
- **Confirmed Decision 5 — Snooze & Notification Elapsed-Time Synchronization**:
  - **Snooze Fix**: Tapping `Snooze 5m` delays the next notification reminder by 5 minutes without adding `+5m` to the mosque's actual Jama'ah clock time (`featured.jamaahMinutes`). Jama'ah at `04:45 PM` remains `04:45 PM` even when snoozed.
  - **Notification vs. Hero Card Sync Fix**: Remove the artificial `.coerceAtLeast(10)` floor on elapsed Jama'ah minutes in `PrayerAlarmReceiver` and `PrayerNotificationHelper`, and evaluate the actual time relative to `jamaahEpochMs` when a snoozed alarm triggers so the notification banner always matches the Hero Card's exact elapsed minutes.

---

## 1. Overview & Core Concept

- **What It Does**: Elevates the main Prayers dashboard with clearer visual hierarchy, complete solar/makruh awareness (`Sun Rising`, `Top of Head Sun`, `Sun Setting`), richer Fajr–Isha row metadata, and rock-solid synchronization between the Hero Card countdown/elapsed timer, Snooze actions, and Android system notifications.
- **Key Value**: Eliminates truncated labels (`CURRENT PRAYER • JAMA'AH STA...`), gives users immediate visibility into both valid prayer windows (`Start – End`) and prohibited solar windows (`Sun Rising`, `Top of Head Sun`, `Sun Setting`), and guarantees that snoozing or receiving a notification never shows conflicting Jama'ah times or elapsed minutes.

---

## 2. User Experience & Visual Design

- **Top Bar (Above Hero Card)**:
  - **Left Badge**: Clean pill showing a calendar icon and the formatted day + date: **`Friday, 30 Oct`**.
  - **Right Badge**: Interactive location pill showing the GPS reticle icon (`Icons.Filled.GpsFixed`), the active city name (`Mumbai`), and a dropdown chevron. Tapping opens the Location modal where `"Use Live Device GPS"` is featured at the top.
- **Larger Hero Card**:
  - Increased vertical padding (`18.dp–20.dp`) and row spacing (`12.dp–14.dp`) for a spacious, balanced look.
  - Top-left status label reads **`CURRENT PRAYER`** (or **`NEXT PRAYER`** before PST), leaving plenty of room for the **`Shuruq 06:38 • Mid 11:45`** badge on the top-right.
- **Fajr–Isha Rows & Solar Warning Rows**:
  - **Fardh Prayer Rows (`Fajr`, `Jumu'ah`/`Dhuhr`, `Asr`, `Maghrib`, `Isha`)**:
    - Left side: Time-of-day icon + Prayer Name (`Fajr`, `Jumu'ah`, `Asr`, `Maghrib`, `Isha`) on the top line, and the solar window range (`05:01 AM – 06:12 AM`) on the subtitle line below.
    - Right side: **Adhan** time pill, **Jama'ah** time pill, and the completion checkmark/lock icon.
  - **Solar / Makruh Rows (`Sun Rising`, `Top of Head Sun`, `Sun Setting`)**:
    - Styled with a soft rose/coral surface tint and border (`CoralAlert.copy(alpha = 0.14f)` background, `CoralAlert.copy(alpha = 0.45f)` border), a left warning triangle icon, the solar phase title + start time on the left, a horizontal connector line in the middle, and the end time on the right (matching your reference screenshot).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Decoupling Reminder Snooze from Mosque Jama'ah Clock Time**
  - *Chosen Approach*: Keep `jamaahSec = featured.jamaahMinutes * 60` invariant when `Snooze 5m` is tapped, and use snooze exclusively to schedule an exact alarm for `System.currentTimeMillis() + 5 * 60_000L` while suppressing intermediate nags during those 5 minutes.
  - *Why*: Previously, adding `combinedSnooze` to `featured.jamaahMinutes` shifted the mosque's Jama'ah time into the future (`4:45 PM + 10m = 4:55 PM`), causing the UI to falsely say `"Jama'ah in 10m"` after Jama'ah had already passed.
- **Decision 2: Exact Elapsed Minutes in System Notifications**
  - *Chosen Approach*: Compute `minPassedSinceJamaah = ((nowMs - effectiveJamaahEpoch) / 60_000L).toInt().coerceAtLeast(1)` without `.coerceAtLeast(10)`, and in `ACTION_SNOOZE_5M` dynamically pick `STAGE_PRE_JAMAAH` if `nextEpoch < jamaahEpochMs` or `STAGE_3_POST_JAMAAH_NAG` if `nextEpoch >= jamaahEpochMs`.
  - *Why*: Ensures that if a user snoozes a pre-Jama'ah alert at 4:41 PM and it fires at 4:46 PM (1 minute after 4:45 PM Jama'ah), both the notification and the Hero Card display **`1 min elapsed since Jama'ah`**.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                           Prayers Screen Layout                           │
├───────────────────────────────────────────────────────────────────────────┤
│  [ 📅 Friday, 30 Oct ]                         [ 🎯 Mumbai ▾ (GPS/City) ] │
├───────────────────────────────────────────────────────────────────────────┤
│  HERO CARD (Spacious Proportions)                                         │
│  CURRENT PRAYER                                 ☀️ Shuruq 06:38 • Mid 11:45│
│  Asr العصر                                            [ Jama'ah 04:45 PM ] │
│  JAMA'AH TIME                                                             │
│  04:45 PM                                      2 mins elapsed since Jama'ah│
├───────────────────────────────────────────────────────────────────────────┤
│  Daily Prayers                                     Adhan   Jama'ah   Done │
│  ☁️ Fajr (05:01 AM – 06:12 AM)                  [05:00 AM] [05:20 AM] [✓] │
│  ⚠️ Sun Rising   06:13 AM ──────────────────────── 06:28 AM               │
│  ⚠️ Top of Head Sun  11:56 AM ──────────────────── 12:04 PM               │
│  ☀️ Jumu'ah (12:04 PM – 03:22 PM)               [01:00 PM] [01:30 PM] [✓] │
│  🌤️ Asr (03:23 PM – 05:53 PM)                   [04:30 PM] [04:45 PM] [✓] │
│  ⚠️ Sun Setting  05:39 PM ──────────────────────── 05:54 PM               │
│  🌅 Maghrib (05:54 PM – 07:05 PM)               [06:08 PM] [06:18 PM] [🔒] │
│  🌙 Isha (07:06 PM – 11:28 PM)                  [08:15 PM] [08:30 PM] [🔒] │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Data & State Updates**:
  - Add `dayAndDateBadgeText` (e.g., `"Friday, 30 Oct"`) and `solarTransitionItems` (`Sun Rising`, `Top of Head Sun`, `Sun Setting` start/end times) to `PrayerDashboardUiState`.
  - Add formatted solar window start and end strings (`windowStartFormatted`, `windowEndFormatted`, e.g., `"05:01 AM – 06:12 AM"`) to each `PrayerItemUiModel` (where Fajr ends at Sunrise `06:12 AM`, Dhuhr/Jumu'ah ends at `Asr PST - 1m`, Asr ends at `Maghrib PST - 1m`, Maghrib ends at `Isha PST - 1m`, and Isha ends at `Islamic Midnight`).
  - Update `PrayerHabitViewModel`, `PrayerAlarmReceiver`, and `PrayerNotificationHelper` so Snooze only delays reminder alerts by 5 minutes without mutating Jama'ah time, and notifications always display the exact real-time minutes until or since Jama'ah.
