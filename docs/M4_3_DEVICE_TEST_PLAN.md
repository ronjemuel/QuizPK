# M4.3 two-device Classic acceptance — pending

M4.2 lobbies and the Create Room refresh correction already passed user-reported
real two-device tests. These checks apply to the new Classic gameplay. They have
not been performed on physical phones during implementation.

Install `artifacts/QuizPK-M4.3-classic-debug.apk` as an update on each phone, with
the existing package/signer. Never uninstall or clear data. The APK alone does not
upgrade the backend; use a separately reviewed/deployed M4.3 backend and two
distinct existing individual logins. Do not show credentials in recordings/logs.

1. Recheck M1 navigation/themes, M2 saved/resumable quizzes and history, M3 saved
   AI replay offline, and M4.2 continuous 30-second form typing/selection/keyboard
   stability. Create/join, ready, settings reset, invite sharing, reconnect and
   separate Moderator lobby must retain their approved behavior.
2. Classic: Science/Normal, five questions, ten seconds. Owner alone cannot Start.
   Join the second phone; leave one unready, then ready both. Only the owner sees
   enabled Start when everyone is online/ready. Double-tap Start: one match only.
3. Verify both see the preparation, same question/options, round number and same
   deadline. Phones may be on different internet networks. Record the ordinary
   visible timer difference/latency; no exact millisecond equivalence is promised.
4. Submit once on each device. Rapid double taps and changing choices after Submit
   cannot create another accepted answer. Before reveal, only received/thinking
   status and previous scores appear; no correct color, explanation, pending points
   or other player's option. The accepted own choice remains locked.
5. Deliberately submit a known wrong choice first and a correct choice second.
   Reveal must distinguish First submission from Fastest correct. Inspect times,
   zero wrong points and the documented 100–150 correct score. Repeat with both
   wrong and one unanswered: no fastest-correct badge for a wrong answer.
6. Leave one player unanswered until zero. Attempts at/after the deadline cannot
   score. Both see the permitted answer/explanation and updated standings, then
   the next round automatically. Finish all five; verify rank, points, correct
   count, accuracy, fastest wins, duration and tied-winner behavior when totals tie.
7. During a LIVE round, disconnect the contestant before submitting, then recover
   before deadline. Controls stay disabled while stale; accepted submissions cannot
   be lost/replaced. Disconnect just after Submit: explicit Retry uses the same
   command. Reconnect after deadline/reveal and confirm the authoritative result.
8. Background/rotate the phone with an unsubmitted selected choice; it remains
   selected without auto-submission. Repeat after acceptance and during reveal.
   Settings/Back and leaving Home retain the reserved seat. Classic continues when
   the owner backgrounds/disconnects; nobody gains host authority.
9. After a completed match, restart the Android app. Its undismissed result returns
   through authenticated current membership. Done returns Online after server
   confirmation. A new Classic room can be created. M2/M3 history remains intact.
10. In a separately approved test-backend restart window, restart during LIVE after
    one accepted answer. Clients recover an interrupted final result: no unrevealed
    key or new points, accepted audit row retained, prior revealed scores unchanged.
    No stale timer or automatic resumed match. Start another room explicitly.
11. Try All Categories with 10/15/20 questions; finish at least one longer match.
    Single-category counts above available stock show a recoverable stock message
    and leave the ready lobby intact. Moderator still has no gameplay Start/answer.
12. Repeat key screens in dark/light, narrow width, 130% font and normal Android
    keyboard settings. Question/options wrap, Submit is reachable by scrolling,
    system bars are clear, and roster/status updates do not jump the viewport.

For security acceptance, use only authorized test accounts/tools on the reviewed
test backend: outsider reads/writes and role/score/timing forgeries must fail;
contestant snapshots/receipts/events cannot yield unrevealed answer keys. Automated
tests cover these boundaries, but do not substitute for two real phones.

Record devices/OS, app/backend versions, network types and pass/fail without tokens
or private invitation codes. Stop and report M4.3 results. Moderator authoring,
manual moderator rounds, LAN and monetization remain outside this acceptance.
