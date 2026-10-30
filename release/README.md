# Noor Prayer — APK Release

- **APK File**: `/release/app-debug.apk`
- **Package ID**: `com.aistudio.noorprayer.qmzrxk`
- **Included Features**:
  - **4-Phase Hero Card Progression**:
    1. **Waiting for Next Prayer (PST)**: Displays `ADHAN TIME` on the left and `"Next prayer in Xm Ys"` on the right.
    2. **Prayer Time Started (PST reached, before Adhan)**: Displays `ADHAN TIME` on the left and `"Adhan in Xm Ys"` on the right.
    3. **Adhan Time Done (waiting for Jama'ah)**: Displays `ADHAN TIME` on the left and `"Jama'ah in Xm Ys"` on the right.
    4. **10+ Minutes After Jama'ah (if unticked)**: Switches the left label to `JAMA'AH TIME` (showing the Jama'ah clock time) and displays `"X mins elapsed since Jama'ah"` on the right.
  - **Compact Daily Prayer Rows**: Displays only the status dot, Prayer Name, tappable **Adhan** and **Jama'ah** pills, and the completion checkmark (with PST shown in the tap-to-edit dialog).
  - **Snooze Gating**: **Snooze 5m** is hidden prior to the Pre-Jama'ah reminder threshold (default `15m` before Jama'ah) and unlocks only once that reminder window is reached.
  - **Settings Tab & Real-Time Notifications**: Clean **Settings** tab for configuring Pre-Adhan (`5m` default), Pre-Jama'ah (`15m` default), Post-Jama'ah periodic reminders (starting `+10m` after Jama'ah and repeating every `10m` by default), and Urgent End-of-Window warnings (`30m` default).

## How to Download from AI Studio
1. Open the **File Explorer** panel in the left sidebar of the AI Studio code editor.
2. Expand the **`release`** folder at the root of the workspace.
3. Right-click **`app-debug.apk`** and select **Download**.
