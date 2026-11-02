# Noor Prayer — Screen 1 Card, Screen 2 Hero Card & Notification Optimization Plan

This update implements your selected UX and notification optimizations across the **Screen 1 (`Lock & Alert`) Card**, the **Screen 2 (`Prayers`) Hero Card**, and **Android System Notifications**.

## User Review & Critical Decisions

> [!IMPORTANT]
> All confirmed choices from your answers are captured below:

- **1. Screen 1 (`Lock & Alert`) Card Optimizations**:
  - **Lock `"Mark as Prayed"` Before Prayer Start Time (Pre-PST)**: When waiting for the next prayer before its solar start time (`Pre-PST`), the bottom action button is locked and displays **`"Unlocks at <PST Time>"`** (e.g., `"Unlocks at 12:04 PM"`), preventing accidental early completion. As soon as Prayer Start Time begins, it unlocks to **`"Mark as Prayed"`**.
  - **Full `Starts • Adhan • Ends` Summary Line**: Beneath the `"Jumu'ah الجمعة"` title, Screen 1 now displays the complete solar and mosque milestone strip: **`"Starts 12:04 PM • Adhan 01:00 PM • Ends 03:21 PM"`**.
  - **Tappable Top-Right `Jama'ah` Pill**: Tapping the top-right `• Jumu'ah 01:30 PM` (or `• Jama'ah 04:45 PM`) pill on Screen 1 opens the **Adhan & Jama'ah Time Editor** bottom sheet for that prayer, matching Screen 2.
- **2. Screen 2 (`Prayers`) Hero Card Enhancement**:
  - **Coral Red Urgent State in Final 15 Minutes**: When an active unprayed prayer enters the last **15 minutes** before its solar window ends, the Hero Card's border, top-left `"CURRENT PRAYER"` status label, and bottom-right countdown/status text automatically transition to **Coral Red (`CoralAlert`)** to clearly signal urgency.
- **3. System Notification Optimizations**:
  - **Live Ticking Countdown / Elapsed Timer (`Chronometer`)**: Uses Android's native `Chronometer` in the notification header (`setUsesChronometer(true)`) so the countdown (before Jama'ah/window end) or elapsed time (after Jama'ah) ticks live every second in the notification shade alongside the `X% loaded` progress bar.
  - **Automatic `Qaza` Rollover Notification**: If a prayer's window expires unprayed and rolls over to `Qaza` (`MISSED_QADA`), any stale urgent countdown notification is automatically replaced with a clean **`"<Prayer> Marked as Qaza"`** alert featuring a one-tap **`"Log as Prayed (Qaza)"`** action.
  - **Three Separate Notification Sound Channels**:
    1. **Soft Prayer Start & Qaza Channel**: Gentle chime for Prayer Start Time (`PST`) and Qaza rollover summaries.
    2. **Active Adhan, Jama'ah & 10m Reminder Channel**: High-priority alert with vibration for Adhan, Jama'ah, and 10-minute post-Jama'ah nags.
    3. **Urgent 15m End-of-Window Channel**: Alarm tone and strong vibration pattern for the final 15-minute warning.

---

## 1. Overview & Core Concept

- **What It Does**: Harmonizes completion locking, time editing, and urgent 15-minute visual cues across Screen 1 and Screen 2, while upgrading system notifications with live-ticking chronometers, dedicated audio channels per urgency tier, and automatic Qaza rollover cleanup.
- **Target Audience / Persona**: Worshippers relying on both the in-app dashboard and lock-screen notification shade for accurate, real-time prayer tracking without stale alerts or accidental early taps.
- **Key Value**: Prevents premature prayer logging before PST, surfaces complete start/adhan/end window times on Screen 1, lets you edit prayer times from either screen, and keeps notifications live and accurate through Qaza transitions.

---

## 2. User Experience & Visual Design

- **Screen 1 (`Lock & Alert`) Card**:
  - **Top-Right Pill**: `• Jumu'ah 01:30 PM ✎` — tappable with ripple feedback to open the Adhan & Jama'ah Time Editor sheet.
  - **Subtitle Line**: Always shows `"Starts 12:04 PM • Adhan 01:00 PM • Ends 03:21 PM"`.
  - **Bottom Action Button**:
    - *Before PST*: Muted slate locked button showing `🔒 Unlocks at 12:04 PM (Prayer Start Time)`.
    - *During Active Window*: Gold (or Coral Red in final 15m) button showing `✓ Mark as Prayed`.
    - *All 5 Completed*: Emerald button showing `✓ All Today's Prayers Completed`.
- **Screen 2 (`Prayers`) Hero Card**:
  - Retains its exact layout (`CURRENT PRAYER` / `NEXT PRAYER` + `Jumu'ah الجمعة` on top-left, `Shuruq`/`Dhuhr`/`Ghurub` pill on top-right, stage time on bottom-left, `Jama'ah` pill + countdown on bottom-right).
  - During the **last 15 minutes** of an unprayed active prayer window, the card border, `"CURRENT PRAYER"` header, and bottom-right status text shift to **Coral Red (`#FF6B6B`)**.
- **Notification Shade**:
  - **2.4s Discord-Style Fast Sweep → Live Chronometer + `X% loaded`**: Sweeps `18% → 52% → 86% → 100%` silently after the initial chime, then settles at `X% loaded` with Android's live `Chronometer` ticking in real time.
  - **At Window End (if Unprayed)**: Transitions to `"<Prayer> Marked as Qaza (Window ended at 03:21 PM)"` with a `"Log as Prayed"` action button.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Unified Pre-PST Completion Lock Across Screen 1 & Screen 2**
  - *Chosen Approach*: Gate Screen 1's primary button behind `isActiveWindowUnprayed` (which is true only when `effectiveNowMinutes >= pstMin` and the prayer is not yet completed).
  - *Why*: Eliminates the discrepancy where Screen 2 locked checkboxes before PST while Screen 1 allowed marking a future prayer as prayed hours before it began.
- **Decision 2: Native Android `Chronometer` Paired with Static `setOnlyAlertOnce` Progress Updates**
  - *Chosen Approach*: Anchor `setWhen(...)` to the relevant milestone timestamp (counting down to Jama'ah/Window End before Jama'ah, or counting up from Jama'ah timestamp after Jama'ah) with `setUsesChronometer(true)`.
  - *Why*: Provides a zero-battery-drain, OS-rendered second-by-second live timer in the notification shade without waking the CPU every second.
- **Decision 3: Three-Tier Notification Channel Architecture**
  - *Chosen Approach*: Split notification channels into `Soft PST & Qaza`, `Active Adhan/Jama'ah/Nag`, and `Urgent 15m End Warning`.
  - *Why*: Allows users to keep PST notifications gentle while ensuring 10-minute post-Jama'ah nags and 15-minute urgent warnings remain prominent.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                Screen 1: Lock & Alert — Optimized Card                    │
├───────────────────────────────────────────────────────────────────────────┤
│  🔔 NEXT PRAYER                            [• Jumu'ah 01:30 PM ✎ (Tap) ]  │
│                                                                           │
│  Jumu'ah الجمعة                                                           │
│  Starts 12:04 PM • Adhan 01:00 PM • Ends 03:21 PM                         │
│                                                                           │
│  Next prayer in 1h 22m                                        42% loaded  │
│  [████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  │
│                                                                           │
│  Pre-PST:   [        🔒 Unlocks at 12:04 PM (Prayer Start)            ]   │
│  Active:    [                  ✓  Mark as Prayed                      ]   │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│        Screen 2: Prayers — Hero Card (Final 15m Urgent Coral State)       │
├───────────────────────────────────────────────────────────────────────────┤
│  CURRENT PRAYER (Coral Red)                          ☀️ Dhuhr 12:04 PM    │
│  Jumu'ah الجمعة                                                           │
│                                                                           │
│  JAMA'AH TIME                                       [ Jumu'ah 01:30 PM ]  │
│  01:30 PM                        1h 36m elapsed since Jama'ah (Coral Red) │
└───────────────────────────────────────────────────────────────────────────┘
```

- **State & Component Mapping**:
  - **`PrayerDashboardUiState` (`PrayerHabitViewModel`)**:
    - Add `isUrgentEndOfWindow: Boolean` (`isActiveWindowUnprayed && remainingUntilEndMinutes <= urgentThresholdMin`).
    - Set `lockScreenStageTimeText` to `"Starts $featuredPstFormatted • Adhan $featuredAdhanFormatted • Ends $featuredDisplayEndFormatted"` across all stages so all three milestones are always visible on Screen 1.
    - Expose `featuredPrayerStartFormatted` (`featuredPstFormatted`) so Screen 1's locked button displays `"Unlocks at $featuredPstFormatted"`.
  - **Screen 1 (`NotificationLockScreen`)**:
    - Pass `onEditJamaahClicked: (PrayerItemUiModel) -> Unit` (and render `JamaahTimeEditorBottomSheet` when `uiState.editingPrayer != null` or invoke the shared ViewModel editor dialog) when the top-right `minutes_until_jamaah_badge` pill is tapped.
    - When `!uiState.isActiveWindowUnprayed && !uiState.isAllTodayPrayersCompleted`, disable `mark_as_prayed_primary_button` and display a lock icon with `"Unlocks at ${uiState.featuredPrayerStartFormatted}"`.
  - **Screen 2 (`PrayerHomeScreen`)**:
    - Use `uiState.isUrgentEndOfWindow || uiState.isIshaMissedAfterMidnight` to style the Hero Card border, `heroHeaderStatusLabel`, and `hero_countdown_status_text` in `CoralAlert`.
  - **Notifications (`PrayerNotificationHelper` & `PrayerAlarmReceiver`)**:
    - Add `SOFT_PST_CHANNEL_ID = "noor_prayer_pst_soft_channel"` and `STAGE_QAZA_ROLLOVER` in `AlarmTriggerType`.
    - Configure `setUsesChronometer(true)` and `setWhen(chronometerTargetEpochMs)` after the 2.4-second Discord-style loading sweep settles.
    - Trigger `STAGE_QAZA_ROLLOVER` notification when a prayer transitions to `MISSED_QADA` at window end so the user is notified that the prayer was marked as Qaza and can log it when prayed.
