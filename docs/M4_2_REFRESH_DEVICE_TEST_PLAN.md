# M4.2 refresh fix — physical acceptance pending

Use `artifacts/QuizPK-M4.2-refresh-fix-debug.apk` as an update with the existing
signer (`adb install -r`, or Android's update installer). Never uninstall or clear
storage. This task builds the APK; it does not install it, modify the live backend,
rotate credentials or change app login. Use the already reviewed room backend.

Record phone/OS, theme/font, app/backend version and pass/fail without tokens or
private invitation codes. Repeat the editing checks in dark/light mode, narrow
width/large font and with the phone's normal keyboard/autofill settings.

1. Confirm there is no active membership for the account, then open Online → Create
   Room. Type/edit continuously for **30 real seconds**, keeping the name under
   40 characters by inserting/deleting in its middle. At each 5-second interval,
   verify the exact text, selection/composing word, keyboard focus and scroll
   position remain stable while background requests complete. The screen remains
   Create Room; no server name is inserted into the editor.
2. Switch Classic/Moderator and every settings field, then return to the name.
   Selected values must remain unchanged by polling. Open/close the keyboard
   several times, scroll while typing and select a text range. Keyboard resizing
   may move the viewport to keep the field visible; subsequent status refreshes
   must not reset it or steal focus.
3. Open Join Room and alternate between name/code. Verify both unfinished fields
   and each selection survive polling, keyboard toggles and Back/Create/Join
   navigation. Nothing is submitted until the explicit Create/Join action.
4. While editing, disconnect networking and reconnect. The draft/page stays put;
   status/actions reflect connectivity and no poll disables the editors. Submit
   remains unavailable while verification is pending. An unavailable/rejected
   action must not hide the keyboard or erase input. Existing authentication and
   explicit uncertain-command retry behavior must remain unchanged.
5. Rotate the phone during an unfinished form, background/reopen, and open Settings
   then Back. Verify text, selected range, focused field, mode/settings and scroll
   are restored. Repeat with OS process recreation (without clearing app data).
   A submitted uncertain command must retain its exact retry semantics rather
   than sending changed form contents automatically.
6. Submit a valid Create/Join. Exactly one command is accepted; only its validated
   fresh membership snapshot opens the lobby. Background/reconnect an existing
   seat: same participant/mode/role. A confirmed server membership/closed/expired
   transition may change the screen; loading/errors/cached state alone must not.
7. In Classic and nonplaying Moderator, check ready toggles, settings/reset/conflict,
   copy/share, presence/reserved seats and confirmed leave/close on two phones.
   Ordinary roster changes should update the changed rows without jumping to top.
   No live match or LAN action is added. Use the existing full two-device plan too.
8. Recheck M1 navigation/themes, M2 offline draft/history/play, and M3 saved AI replay
   with networking disabled. Existing login and saved data must remain present.

JVM tests model 30 seconds of editing, refresh/cancellation, IME insets, lifecycle
and SQLite/HTTP behavior. They do not certify real keyboard composition, OEM
autofill, physical rendering, radio reconnection or OS process termination.
Stop and report physical results before any further milestone.
