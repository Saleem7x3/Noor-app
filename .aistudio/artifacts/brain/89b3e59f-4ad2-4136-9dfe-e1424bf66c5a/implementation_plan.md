# Noor Prayer — Hero Card & Lock Screen Header, Title & Countdown Layout Refinement Plan

This update repositions the prayer title (`"Jumu'ah الجمعة"`) directly beneath the top-left status label on the Hero Card, removes the extra `"Prayer"` suffix on Screen 1 (`Lock & Alert`) so it displays `"Jumu'ah الجمعة"`, and moves `"Next prayer in xxxy"` to sit above the left side of the loading bar on Screen 1.

## User Review & Critical Decisions

> [!IMPORTANT]
> Your latest layout refinements for the Hero Card (Screen 2) and Lock & Alert card (Screen 1) are captured below alongside all existing features:

- **1. Hero Card Top-Left Prayer Title Placement (Screen 2 — Prayers)**:
  - Move **`"Jumu'ah الجمعة"`** (`<Prayer Name> <Arabic Name>`) from the bottom-left cluster up to the **top-left cluster**, positioned **directly below `"NEXT PRAYER"` / `"CURRENT PRAYER"`**.
  - The bottom-left cluster now cleanly displays only the stage time label (`STARTS AT` / `ADHAN TIME` / `JAMA'AH TIME`) and large time readout (`01:00 PM`), while the bottom-right keeps the `Jumu'ah 01:30 PM` pill and live countdown (`Next prayer in xxxy`).
- **2. Screen 1 Prayer Title Format (Screen 1 — Lock & Alert)**:
  - Remove the extra word `"Prayer"` from `"$prayerName Prayer"` so it is never two English words (`"Jumu'ah Prayer"`).
  - Instead, always display **`"Jumu'ah الجمعة"`** (English prayer name + Arabic calligraphy right beside it).
- **3. Screen 1 Countdown Placement Above Loading Bar (Screen 1 — Lock & Alert)**:
  - Move **`"Next prayer in xxxy"`** (the live stage countdown / elapsed text that previously sat on the right below the `Jumu'ah 01:30 PM` pill) to **above the loading bar on the left**.
  - Remove the previous left-side text above the loading bar, leaving `"Next prayer in xxxy"` on the left and `"X% loaded"` on the right directly above the loading bar.

---

## 1. Overview & Core Concept

- **What It Does**: Refines the visual hierarchy of both the Hero Card on the Prayers screen and the Lock & Alert notification card on Screen 1 so the prayer identity (`"Jumu'ah الجمعة"`) anchors the top-left of the card and the live countdown (`"Next prayer in xxxy"`) sits cleanly above the left side of Screen 1's progress bar.
- **Target Audience / Persona**: Daily worshippers who want an immediately scannable, uncluttered prayer card across both the lock-screen notification preview and the main dashboard.
- **Key Value**: Eliminates redundant wording (`"Jumu'ah Prayer"`), balances top-left and bottom-left spacing inside the Hero Card, and consolidates progress/countdown status directly above the loading bar on Screen 1.

---

## 2. User Experience & Visual Design

- **Hero Card (Screen 2 — Prayers)**:
  - **Top-Left Cluster**: `NEXT PRAYER` (or `CURRENT PRAYER`) header label on top, with **`Jumu'ah الجمعة`** directly underneath it.
  - **Top-Right Cluster**: Time-based solar pill (`Shuruq` / `Dhuhr` / `Ghurub`).
  - **Bottom-Left Cluster**: Stage time label (`STARTS AT` / `ADHAN TIME` / `JAMA'AH TIME`) + large gold time value (`01:00 PM`).
  - **Bottom-Right Cluster**: Plump `Jumu'ah 01:30 PM` pill + live countdown/elapsed text below it.
- **Lock & Alert Notification Card (Screen 1)**:
  - **Top Row**: Status header (`NEXT PRAYER` / `CURRENT PRAYER`) on the left + `Jumu'ah 01:30 PM` pill on the right.
  - **Middle Section**: **`Jumu'ah الجمعة`** (without the word `"Prayer"`) + schedule summary line (`Starts 12:04 PM • Adhan 01:00 PM • Jama'ah 01:30 PM`) underneath.
  - **Loading Bar Header Row**: **`Next prayer in xxxy`** (or active stage countdown/elapsed text) on the **left above the loading bar**, and **`X% loaded`** on the **right above the loading bar** (the old left label above the loading bar is removed).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Grouping `"Jumu'ah الجمعة"` Below `"NEXT PRAYER"` / `"CURRENT PRAYER"` in the Hero Card**
  - *Chosen Approach*: Place the English + Arabic prayer title inside a top-left `Column` directly beneath the status header (`CURRENT PRAYER` / `NEXT PRAYER`), leaving the bottom-left strictly for the time label and clock value.
  - *Why*: Anchors the prayer identity immediately beneath its status label while giving the bottom sky wave and time readout extra breathing room.
- **Decision 2: Unified `"Jumu'ah الجمعة"` Title & Left-Aligned Countdown Above Progress Bar on Screen 1**
  - *Chosen Approach*: Render `prayerName` + `prayerArabic` (`"Jumu'ah الجمعة"`) on all display sizes without appending `"Prayer"`, remove the right-aligned countdown from the middle row, and place `uiState.heroCountdownSubtitleText` (`"Next prayer in xxxy"`) on the left directly above the loading bar in place of the old `progressLeftLabelText`.
  - *Why*: Prevents horizontal crowding beside the prayer title and pairs the live time remaining/elapsed directly with the `X% loaded` progress bar.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                   Screen 2: Prayers — Hero Card Layout                    │
├───────────────────────────────────────────────────────────────────────────┤
│  NEXT PRAYER                                         ☀️ Dhuhr 12:04 PM    │
│  Jumu'ah الجمعة                                                           │
│                                                                           │
│  STARTS AT                                          [ Jumu'ah 01:30 PM ]  │
│  01:00 PM                                      Next prayer in 1h 22m 57s  │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│                Screen 1: Lock & Alert — Notification Card                 │
├───────────────────────────────────────────────────────────────────────────┤
│  🔔 NEXT PRAYER                                     [• Jumu'ah 01:30 PM ] │
│                                                                           │
│  Jumu'ah الجمعة                                                           │
│  Starts 12:04 PM • Adhan 01:00 PM • Jama'ah 01:30 PM                      │
│                                                                           │
│  Next prayer in 1h 22m 57s                                    42% loaded  │
│  [████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  │
│                                                                           │
│  [                        ✓  Mark as Prayed                            ]  │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Component & State Mapping**:
  - **Hero Card Top-Left Cluster**: Moves the `Row` displaying `featuredPrayer?.nameEnglish` (`"Jumu'ah"`) and `featuredPrayer?.nameArabic` (`"الجمعة"`) into a `Column` directly under `uiState.heroHeaderStatusLabel` (`"NEXT PRAYER"` / `"CURRENT PRAYER"`).
  - **Screen 1 Title Row**: Replaces `"$prayerName Prayer"` with `prayerName` and ensures `prayerArabic` is always displayed beside it (`"Jumu'ah الجمعة"`), with the trailing right-side countdown text removed from that row.
  - **Screen 1 Loading Bar Label Row**: Replaces `uiState.progressLeftLabelText` on the left above the loading bar with `uiState.heroCountdownSubtitleText` (`"Next prayer in xxxy"` / stage countdown / elapsed text), paired with `"$pctLoaded% loaded"` on the right.
