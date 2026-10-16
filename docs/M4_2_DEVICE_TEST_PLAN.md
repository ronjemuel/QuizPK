# M4.2 two-device lobby acceptance plan

Status: automated tests are separate evidence; these physical steps are pending.
No ADB device was attached during this implementation. Online lobby acceptance
does not certify matches or offline LAN. Stop after lobbies for M4.2 review.

## Prerequisites

- Use two Android phones, each with a **different individual M3 user token** for
  the same HTTPS backend. Sharing a login intentionally recovers the same seat.
  Provision additional identities only under the separately approved operator plan.
- Deploy the approved M4.1 backend only after reviewing and authorizing
  [the migration/deployment plan](M4_2_DEPLOYMENT_PLAN.md). The untouched live M3
  host cannot serve lobby endpoints. Keep the backend/tunnel available throughout.
- Install `artifacts/QuizPK-M4.2-online-lobby-debug.apk` as an **update** on each
  phone (`adb install -r` or the Android update installer). Never uninstall or clear
  app storage. The package and signer must match M3. Note a saved M2 history entry,
  saved AI quiz and any resumable solo draft before updating.
- Use Wi-Fi on one phone and mobile data on the other for at least one run. Check
  dark/light mode, keyboard, TalkBack, large font and rotation on both. Share codes
  privately; never capture tokens in screenshots, logs or test reports.

## Classic with two contestants

1. Launch/update, check Home/Profile and saved history, resume the solo draft if
   present, and replay a saved AI quiz with networking disabled. Restore networking.
   Existing AI connection should remain saved. No new AI generation is needed.
2. Multiplayer opens the connection choice. Online opens the room entry; Local is
   visibly unavailable. An old M3 host must show the unsupported-room message while
   its existing AI endpoints remain usable. After approved backend deployment, use
   the same HTTPS origin and each phone's own token in Connect/Update connection.
3. Phone A creates Classic using a display name and chosen lobby settings. A is a
   playing contestant: roster `1 / 8`, own host label, not ready. Copy the six-character
   code, then inspect the share sheet: code/mode/origin only, no access token.
4. Phone B joins with its own name and the code (lowercase/pasted surrounding spaces
   normalize). Both rosters should show two real contestants within a normal 5s poll.
   B cannot edit settings or close the room for everyone. Duplicate rapid taps must
   create one room/seat/action, with a busy indication while waiting.
5. Toggle each contestant ready/not ready. Both phones converge on ready counts.
   A changes settings: B sees the new settings and all readiness resets. If another
   update changes the revision while A edits, saving must show a conflict and require
   reviewing fresh state rather than silently replacing it.
6. Background B for more than 45s. A sees “Disconnected · Seat reserved”; B's ready
   state resets, count stays two. Return B: same participant/name/seat, not a duplicate.
   Repeat with process death (force-stop, then relaunch and choose Online) and rotation.
   Do not clear app data. Switching away stops polling; room membership stays reserved.
7. Disconnect A for more than 45s. B sees the host-disconnected notice, existing seats
   remain, and no host transfer occurs. A's authenticated reconnect restores authority.
   A third new identity, if available, cannot join while the host is absent.
8. Interrupt a create/join/ready write's network response on a private staging host.
   Restore networking and explicitly Retry pending action. Confirm one room/seat and
   one logical effect. After process death, Online recovers from authenticated current
   membership without requiring the user to remember a room ID or receive a new role.
9. Back from the lobby offers Keep seat and go Home, Leave/Close and Cancel. Test
   Cancel, Keep seat/reconnect and B's confirmed Leave. A sees the freed seat. A's
   confirmed Close invalidates the code; B sees a closed/ended room or missing code
   on rejoin, never stale actionable controls. There is no Start Match/answer screen.

## Nonplaying Moderator with two phones

1. A creates Moderator. A appears in the separate **MODERATOR · NONPLAYING** card;
   contestant count is `0 / 8`. A has no contestant Ready button or answer UI.
2. B joins by code and becomes a contestant, never a moderator. Count is `1 / 8`;
   B can ready, A can change settings/close, and settings changes reset B's readiness.
   Moderator presence/reconnect must preserve that role and separate capacity.
3. Repeat background/reconnect, theme/rotation, keyboard and leave/close checks.
   This two-phone test covers one moderator and one contestant. Add a **third phone**
   for moderator plus two contestants before full moderator acceptance in later stages.

## Capacity and failures

- On an isolated staging backend, add distinct synthetic test identities to fill
  eight contestant seats. Classic includes its playing host in eight; Moderator
  permits its separate moderator plus eight contestants. A ninth contestant gets a
  clear Room full error and no seat. Free one seat and join again. Mark automated
  identities as synthetic in the report; this is not an eight-phone acceptance claim.
- Invalid, missing, closed and expired codes show the same safe missing-room result.
  Verify expiry with a two-hour wait or an isolated test clock; never edit the live DB
  or device clock to manufacture it. Do not spam the live origin to trigger quotas.
- In staging, exercise 429/cooldown, revoked token, malformed response, TLS failure
  and server outage. Revocation removes code/roster, stale states disable writes,
  and retry respects cooldown. Never bypass certificate validation. Check changing
  connection/token clears prior room/pending display and recovers only the new identity.
- Check 320dp width and large fonts: scroll to every field/action, read wrapped
  roster/status text, use 48dp+ controls, switch dark/light and rotate without hidden
  code/ready controls or duplicate commands. Verify TalkBack labels and logical order.

Record phone/OS, theme/font, backend build, steps, observed results and any failures
without secrets or private invitation codes. Recheck offline M2 gameplay/history,
M3 saved AI replay, Settings and navigation afterward. Submit results for review;
M4.3 and LAN runtime remain outside this APK.
