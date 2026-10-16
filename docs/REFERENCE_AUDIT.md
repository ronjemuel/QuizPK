# Visual reference inspection — M0

All 19 PNGs were opened with the image viewer on 2026-10-12.
No reference PNG is copied into runtime drawable resources.

| PNG | Observations and implementation direction |
| --- | --- |
| 01 Official logo | Crowned question shield and sparkle; recreate a clean scalable vector, preserve QuizPK spelling. |
| 02 Navigation icons | Coherent violet duotone family; labelled native navigation and original vectors. |
| 03 Category icons | Twelve categories with geometric symbols; catalogue preview without invented question counts. |
| 04 Gameplay/achievements | Distinct result/timer/ranking symbols; add text semantics, never rely on color alone. |
| 05 Home | Branded header, hero, modes, categories, recent activity; adapt to scrolling small phones and real empty progress. |
| 06 Offline quiz | Category/options/game/results sequence; defer gameplay to tested M2. |
| 07 AI quiz | Topic/configuration/saved quiz flows; no fake generation, require a server integration. |
| 08 Multiplayer lobby | Private create/join/code and eight seats; no hardcoded room code or fake presence. |
| 09 Multiplayer match | Shared question, submission, rankings, history; illustrated unrevealed correctness is disallowed by brief. |
| 10 Profile/history | Personal progress and history hierarchy; no fake user/avatar/XP or invented statistics. |
| 11 Settings/additional | Appearance selector and grouped settings; implement persisted theme, omit shop/coins/unconfigured notifications. |
| 12 Moderator setup | Explicit nonplaying moderator plus eight contestants; bank/custom questions and private-only setup. |
| 13 Question library | Search/filter, private preview, selected questions; ordered deduplicated queue planned for M5. |
| 14 Custom builder | Four choices, exactly one correct, optional explanation/image; strict input validation and confirmed destructive actions. |
| 15 Queue/waiting lobby | Ordered queue and independent moderator; room code only after real server creation. |
| 16 Live moderator | Ready/live/closed/revealed controls; start-match must not start Q1, keys stay private. |
| 17 Round/match summary | Distribution, order, fastest correct and final rankings; visual example data is not authoritative. |
| Additional Quiz UI Reference | Alternative two-column cards and difficulty hierarchy; no advertised counts until a bank exists. |
| Alternate Logo Branding Board | Violet duotone, crown/shield identity and light preview; primary brief palette takes precedence. |

Do not reproduce phone frames, iOS status bars, screenshot glows, fake sample trivia,
shop, coins, public matchmaking, friends network or a public leaderboard as approved
features. Use native Android status/navigation bars, accessible text and touch sizes.

## M3 Asset 07 review (2026-10-12)

Reopened `07_AI_Quiz.png` before implementation. Retained the approved violet,
card hierarchy, topic/difficulty/settings form, indeterminate generation state,
saved-quiz metadata and replay actions using native Java/XML components. M2's
existing immersive gameplay/results/review are reused. No reference example trivia,
fake progress/checkmarks, favorites/share placeholders, phone frames or raster UI
screens were copied. Both themes use the established semantic tokens. JVM tests
check state and compact geometry; actual visual/device comparison remains pending.
