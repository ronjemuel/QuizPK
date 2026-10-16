# M4.2 Multiplayer refresh and draft-state correction

This patch keeps the approved Java/XML design, common v1 protocol, backend,
authentication and Android SQLite v3. Only lobby UI/coordinator behavior, one
progress visibility attribute and the app update version change. Matches and LAN
remain deferred. No live backend/app installation or credential change is included.

## Confirmed causes

The original code failed both focused reproduction tests:

1. Every 5-second read set the same busy flag used for commands. render disabled
   the display-name/code/spinner controls on each read, including the focused
   EditText, then re-enabled them after the reply. That interrupts the input
   connection/focus. A GONE/VISIBLE progress bar also changed the status card's
   height on each cycle, prompting scroll/layout movement.
2. render selected the page from any retained snapshot, ignoring connection and
   confirmation state. A paused/unconfirmed cached room publication therefore
   hid Create Room and showed the cached lobby/roster. This can make the visible
   name appear replaced by the server's roster name. There was no direct
   setText(serverName) path into the editable display-name field.

The cold/back-stack tests also exposed selection/focus restoration ordering:
restoring a selection before framework focus restoration could move the cursor,
and capturing only at view destruction could miss focus already lost during
detachment. The full suite also reproduced TextView's first-layout initialization
moving an already restored selection to the end. Restoration now finishes once
after that first layout, with the posted fallback sharing the same completion
guard. Framework view-state restoration is distinct from the fragment's
page/form state; see [Android fragment state guidance](https://developer.android.com/guide/fragments/saving-state).

Review covered the single view-lifecycle LiveData observer, explicit connection
save/reload and account comparison, request callbacks, poll/heartbeat scheduling,
MainActivity replacement/back stack/insets, and fragment recreation. There is no
separate connection preference observer repeatedly replacing this fragment.
MainActivity's existing keyboard insets and M1 navigation remain unchanged.

## Resulting behavior

- Read busy and action busy are separate. Polls/reconnect reads never disable or
  rewrite form editors. Accepted commands and uncertain pending retries still
  lock their immutable input, preserving existing idempotency behavior. A rejected
  local submission does not hide the keyboard.
- Passive polls retain status/progress geometry. Progress reserves its small
  space while idle and appears for initial connection/foreground commands.
  Text, visibility and enabled properties are changed only when values differ;
  roster DiffUtil updates changed members instead of invalidating the whole list.
- Rendering no longer chooses navigation. A success counter advances only for
  validated accepted snapshots; confirmed membership/terminal/no-room results
  reconcile the page. User Back/Create/Join/terminal actions remain explicit.
  Errors/loading/paused cached snapshots cannot switch forms to lobbies. Auth
  revocation clears private room data; failed room reads wait for authenticated
  current membership before leaving the disabled lobby.
- Lifecycle/account epochs plus request sequence checks reject delayed callbacks.
  Lower room revisions and previously retired authority boot epochs are rejected.
  Legitimate server clock adjustments are not used to order room state.
- Raw Create/Join text, selection ranges, focused field, mode/settings and scroll
  are captured before pause/detach and in fragment saved state, restored once
  after hierarchy restoration. Deferred layout restoration is tied to that view
  and page and cannot replace a new text edit. Polling never restores a draft.
  Tokens, authoritative roles and pending command envelopes are not saved in this
  draft, and no preferences/database schema or credential format is added.

Validation and signed artifact details are recorded in [TEST_NOTES](TEST_NOTES.md)
and `docs/validation/m4.2-refresh-fix/`. The before-fix failures and intermediate
failures are retained. No physical keyboard, OEM autofill or radio result is
claimed from JVM checks. Use the [physical acceptance plan](M4_2_REFRESH_DEVICE_TEST_PLAN.md)
and stop for review before any next milestone.
