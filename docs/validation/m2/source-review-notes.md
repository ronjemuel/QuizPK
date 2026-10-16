# M2 seed review notes — 2026-10-12

The canonical asset and the per-ID attribution table are the reviewable content.
Questions and explanations use original wording, with factual references rather
than copied trivia. Review was performed by the coding assistant, not an independent
human editor. Difficulty labels are editorial; there was no player calibration.

Review covered answer/wording consistency, four distinct choices, stable IDs,
normalized duplicate prompts, category/difficulty coverage and ambiguous qualifiers.
Examples: Euclidean triangle; pure water at approximately one atmosphere; highest
summit above sea level; standard advantage tennis; standard FIBA five-on-five;
no penalty for unsent selected answers; Pluto distinguished from the eight planets.

Source families reviewed:

- UN, US National Archives, UNESCO and Egypt's Ministry of Tourism and Antiquities
  for historical documents, dates and sites.
- BIPM for SI definitions, RSC for element data, OpenStax for algebra/statistics,
  biology and physical science; simple numeric answers were also recalculated.
- NOAA, National Geographic and official geographic/government references for
  oceans, coordinates, geographic features and capitals.
- W3C/MDN, IETF RFCs and SQLite documentation for web/protocol/data facts.
- IFAB, MCC, ITF, BWF, FIBA, World Athletics and Olympic sources for sports rules.
- Pixar, Disney and the Academy's ceremony/statuette pages for films and awards.
- Open Music Theory for notation, keys, scales, intervals and triads.
- Original books referenced through Project Gutenberg for literature facts.
- Smithsonian, San Diego Zoo Wildlife Alliance, NOAA and NPS for animals.
- Kew, Cambridge Dictionary, OpenStax, NIH, ACS, Guelph dairy-science material and
  King Arthur's baking guide for food/plants/fermentation/protein facts.
- NASA planetary, moon and mission pages for space.

Review corrections made before delivery:

- Replaced moved/404 NASA Europa, Ganymede and Io facts paths; used the actual
  Europa `/europa/europa-facts/` URL rather than the other moons' `/facts/` pattern.
- Replaced a missing San Diego general-penguin URL with the zoo's Penguin page.
- Replaced MDN's removed Byte glossary page with Uint8Array documentation.
- Replaced the removed NPS bats link with Yosemite's bat reference.
- Updated four MCC Laws of Cricket URLs; they redirect to the current laws site.
- Used actual Open Music Theory note/clef, meter and notation chapters rather
  than guessed chapter slugs; interval inversion cites the intervals chapter.
- Replaced inaccessible secondary food summaries with specific botanical,
  dictionary, dairy-science, baking and chemistry references.
- Reworded rennet as a preparation added to coagulate milk, not an inoculating
  culture. Changed a circular rice question to sushi's vinegar seasoning, citing Breville's actual sushi-rice recipe rather than a general sushi definition.
- Clarified the panda wrist-bone question as the name pseudo-thumb, and JSON null
  as deliberate absence, distinguishing it from an omitted property.

`final-source-url-check.json` records direct HTTP checks of canonical references.
Earlier `source-url-check.json`/txt retain the first attempt, including broken URLs
subsequently corrected. Some direct requests are blocked (403), reset, or return
503/504; available browser search/open results were also consulted. A blocked URL
is not presented as a successful full-page fetch. HTTP 200 alone does not establish
factual correctness, and these checks are not an independent content certification.
Sources are never fetched during app use. No source-page body is bundled.
