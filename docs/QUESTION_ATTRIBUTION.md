# M2 question attribution and review

Bank revision: M2 initial seed, reviewed 2026-10-12. Total: 240 questions; twelve
categories, four editorial difficulty labels, five questions per category/difficulty.
Each question has four distinct options and exactly one keyed answer. The original
question and explanation wording was authored for QuizPK by the coding assistant;
no external trivia dataset, reference-image trivia or long source passages were imported.

The URLs below identify factual references, not a license to redistribute those
websites. Their authors retain their source text and artwork. The bank uses original
short questions about facts; source material is not bundled. Literature references
point to original works via Project Gutenberg; only facts about authors/characters
are used. No third-party images, book extracts or copyrighted question bank is included.

Assistant review checked answer/wording against the cited references and stable
facts, distinct options and duplicate prompts. This is **not independent human
expert review**, empirical difficulty calibration or a guarantee that every website
remains accessible. The linked bank is reviewable in full. Before a larger public
content release, an independent editor should review difficulty and cultural coverage.

Source checking corrected moved/broken NASA, MCC, MDN, NPS and music-theory URLs.
Some sites block this host (403), reset connections, or return temporary 503/504;
those HTTP outcomes are recorded separately from factual review. URL success alone
is not a truth check. See `validation/m2/final-source-url-check.json` and
`validation/m2/source-review-notes.md` for access results and reviewed corrections.
The app reads bundled data only and never fetches these URLs to run a quiz.

Structural audit: `python3 tools/validate_question_bank.py`. It checks unique IDs
and normalized prompts/options, counts, answer indices and attribution metadata.
Canonical questions/options: `app/src/main/assets/questions.json`.

| ID | Difficulty | Correct answer | Explanation | Factual source | Review date |
| --- | --- | --- | --- | --- | --- |
| qpk-general-001 | Easy | 12 | A Gregorian year has twelve calendar months. | [Reference](https://www.timeanddate.com/calendar/gregorian-calendar.html) | 2026-10-12 |
| qpk-general-002 | Easy | 3 | A triangle is a polygon with three sides. | [Reference](https://openstax.org/books/prealgebra-2e/pages/9-3-use-properties-of-angles-triangles-and-the-pythagorean-theorem) | 2026-10-12 |
| qpk-general-003 | Easy | 50 | Dividing 100 by two gives 50. | [Reference](https://openstax.org/books/prealgebra-2e/pages/1-5-divide-whole-numbers) | 2026-10-12 |
| qpk-general-004 | Easy | 12 | A dozen is a group of twelve. | [Reference](https://www.merriam-webster.com/dictionary/dozen) | 2026-10-12 |
| qpk-general-005 | Easy | United Nations | The United Nations is an international organization. | [Reference](https://www.un.org/en/about-us) | 2026-10-12 |
| qpk-general-006 | Normal | 6 | The six are Arabic, Chinese, English, French, Russian and Spanish. | [Reference](https://www.un.org/en/our-work/official-languages) | 2026-10-12 |
| qpk-general-007 | Normal | 180 degrees | The three interior angles of a plane triangle add to 180 degrees. | [Reference](https://openstax.org/books/prealgebra-2e/pages/9-3-use-properties-of-angles-triangles-and-the-pythagorean-theorem) | 2026-10-12 |
| qpk-general-008 | Normal | kilo | The prefix kilo denotes a factor of 10 to the third power. | [Reference](https://www.bipm.org/en/measurement-units/si-prefixes) | 2026-10-12 |
| qpk-general-009 | Normal | 36 square cm | The area of a square is side length multiplied by itself. | [Reference](https://openstax.org/books/prealgebra-2e/pages/9-4-use-properties-of-rectangles-triangles-and-trapezoids) | 2026-10-12 |
| qpk-general-010 | Normal | 1/2 | There are two equally likely outcomes and one is heads. | [Reference](https://openstax.org/books/introductory-statistics-2e/pages/3-1-terminology) | 2026-10-12 |
| qpk-general-011 | Hard | 1/36 | Only the pair 1 and 1 works among 36 equally likely pairs. | [Reference](https://openstax.org/books/introductory-statistics-2e/pages/3-1-terminology) | 2026-10-12 |
| qpk-general-012 | Hard | 9 | The median is the middle value in an ordered data set. | [Reference](https://openstax.org/books/introductory-statistics-2e/pages/2-5-measures-of-the-center-of-the-data) | 2026-10-12 |
| qpk-general-013 | Hard | 29 | The only positive divisors of 29 are 1 and 29. | [Reference](https://openstax.org/books/prealgebra-2e/pages/2-5-prime-factorization-and-the-least-common-multiple) | 2026-10-12 |
| qpk-general-014 | Hard | 13 | The Pythagorean theorem gives the square root of 25 plus 144. | [Reference](https://openstax.org/books/prealgebra-2e/pages/9-3-use-properties-of-angles-triangles-and-the-pythagorean-theorem) | 2026-10-12 |
| qpk-general-015 | Hard | 24 | There are 4 times 3 times 2 times 1 possible orders. | [Reference](https://openstax.org/books/college-algebra-2e/pages/9-5-counting-principles) | 2026-10-12 |
| qpk-general-016 | Expert | candela | The candela is the SI base unit for luminous intensity. | [Reference](https://www.bipm.org/en/measurement-units/si-base-units) | 2026-10-12 |
| qpk-general-017 | Expert | mole | Amount of substance is measured in moles. | [Reference](https://www.bipm.org/en/measurement-units/si-base-units) | 2026-10-12 |
| qpk-general-018 | Expert | Planck constant | The kilogram definition fixes the numerical value of the Planck constant. | [Reference](https://www.bipm.org/en/measurement-units/si-base-units) | 2026-10-12 |
| qpk-general-019 | Expert | nano | Nano denotes one billionth of the base unit. | [Reference](https://www.bipm.org/en/measurement-units/si-prefixes) | 2026-10-12 |
| qpk-general-020 | Expert | kelvin | Thermodynamic temperature has the SI base unit kelvin. | [Reference](https://www.bipm.org/en/measurement-units/si-base-units) | 2026-10-12 |
| qpk-science-001 | Easy | H | Hydrogen has the symbol H and atomic number 1. | [Reference](https://periodic-table.rsc.org/element/1/hydrogen) | 2026-10-12 |
| qpk-science-002 | Easy | Oxygen | O is the symbol for oxygen. | [Reference](https://periodic-table.rsc.org/element/8/oxygen) | 2026-10-12 |
| qpk-science-003 | Easy | Carbon | Carbon is element 6, represented by C. | [Reference](https://periodic-table.rsc.org/element/6/carbon) | 2026-10-12 |
| qpk-science-004 | Easy | Na | Sodium has the symbol Na. | [Reference](https://periodic-table.rsc.org/element/11/sodium) | 2026-10-12 |
| qpk-science-005 | Easy | Iron | Fe is the chemical symbol for iron. | [Reference](https://periodic-table.rsc.org/element/26/iron) | 2026-10-12 |
| qpk-science-006 | Normal | Mitochondrion | Cellular respiration produces ATP in mitochondria. | [Reference](https://openstax.org/books/biology-2e/pages/4-3-eukaryotic-cells) | 2026-10-12 |
| qpk-science-007 | Normal | Chloroplast | Chloroplasts contain the photosynthetic machinery. | [Reference](https://openstax.org/books/biology-2e/pages/8-1-overview-of-photosynthesis) | 2026-10-12 |
| qpk-science-008 | Normal | Thymine | In DNA, adenine pairs with thymine. | [Reference](https://openstax.org/books/biology-2e/pages/14-2-dna-structure-and-sequencing) | 2026-10-12 |
| qpk-science-009 | Normal | Nucleus | Most eukaryotic DNA is housed in the nucleus. | [Reference](https://openstax.org/books/biology-2e/pages/4-3-eukaryotic-cells) | 2026-10-12 |
| qpk-science-010 | Normal | Oxygen | Oxygenic photosynthesis releases oxygen from water. | [Reference](https://openstax.org/books/biology-2e/pages/8-1-overview-of-photosynthesis) | 2026-10-12 |
| qpk-science-011 | Hard | Acceleration | Newton's second law relates net force to mass and acceleration. | [Reference](https://openstax.org/books/college-physics-2e/pages/4-3-newtons-second-law-of-motion-concept-of-a-system) | 2026-10-12 |
| qpk-science-012 | Hard | 299792458 | The SI fixes this value exactly at 299792458 metres per second. | [Reference](https://physics.nist.gov/cgi-bin/cuu/Value?c) | 2026-10-12 |
| qpk-science-013 | Hard | 0 degrees Celsius | At normal atmospheric pressure pure water freezes near zero Celsius. | [Reference](https://openstax.org/books/chemistry-2e/pages/10-4-phase-diagrams) | 2026-10-12 |
| qpk-science-014 | Hard | Number of protons | Each element is defined by its proton count. | [Reference](https://openstax.org/books/chemistry-2e/pages/2-3-atomic-structure-and-symbolism) | 2026-10-12 |
| qpk-science-015 | Hard | Positive | Protons carry positive elementary charge. | [Reference](https://openstax.org/books/chemistry-2e/pages/2-3-atomic-structure-and-symbolism) | 2026-10-12 |
| qpk-science-016 | Expert | Gold | Au is the symbol for gold. | [Reference](https://periodic-table.rsc.org/element/79/gold) | 2026-10-12 |
| qpk-science-017 | Expert | K | Potassium has the symbol K. | [Reference](https://periodic-table.rsc.org/element/19/potassium) | 2026-10-12 |
| qpk-science-018 | Expert | Tungsten | W represents tungsten, historically called wolfram. | [Reference](https://periodic-table.rsc.org/element/74/tungsten) | 2026-10-12 |
| qpk-science-019 | Expert | Mercury | Mercury melts well below room temperature. | [Reference](https://periodic-table.rsc.org/element/80/mercury) | 2026-10-12 |
| qpk-science-020 | Expert | Bromine | Bromine is liquid at room temperature. | [Reference](https://periodic-table.rsc.org/element/35/bromine) | 2026-10-12 |
| qpk-history-001 | Easy | 1945 | The UN Charter entered into force on 24 October 1945. | [Reference](https://www.un.org/en/about-us/history-of-the-un) | 2026-10-12 |
| qpk-history-002 | Easy | 1776 | Congress adopted the Declaration on 4 July 1776. | [Reference](https://www.archives.gov/milestone-documents/declaration-of-independence) | 2026-10-12 |
| qpk-history-003 | Easy | Ancient Egyptians | The Giza pyramids are royal monuments of ancient Egypt. | [Reference](https://whc.unesco.org/en/list/86/) | 2026-10-12 |
| qpk-history-004 | Easy | China | The Great Wall is a historic defensive system in China. | [Reference](https://whc.unesco.org/en/list/438/) | 2026-10-12 |
| qpk-history-005 | Easy | Berlin | The Wall divided Berlin from 1961 to 1989. | [Reference](https://www.britannica.com/event/Berlin-Wall) | 2026-10-12 |
| qpk-history-006 | Normal | 1989 | Border crossings opened on 9 November 1989. | [Reference](https://www.britannica.com/event/Berlin-Wall) | 2026-10-12 |
| qpk-history-007 | Normal | Inca | Machu Picchu was built during the Inca Empire. | [Reference](https://whc.unesco.org/en/list/274/) | 2026-10-12 |
| qpk-history-008 | Normal | Nabataeans | Petra was the Nabataean capital and a trading center. | [Reference](https://whc.unesco.org/en/list/326/) | 2026-10-12 |
| qpk-history-009 | Normal | Khmer Empire | Angkor preserves monuments of the Khmer Empire. | [Reference](https://whc.unesco.org/en/list/668/) | 2026-10-12 |
| qpk-history-010 | Normal | 1787 | The Constitution was signed on 17 September 1787. | [Reference](https://www.archives.gov/milestone-documents/constitution) | 2026-10-12 |
| qpk-history-011 | Hard | San Francisco | The Charter was signed in San Francisco on 26 June 1945. | [Reference](https://www.un.org/en/about-us/history-of-the-un) | 2026-10-12 |
| qpk-history-012 | Hard | 51 | The UN began with 51 original member states. | [Reference](https://www.un.org/en/about-us) | 2026-10-12 |
| qpk-history-013 | Hard | John | King John agreed to Magna Carta in June 1215. | [Reference](https://www.parliament.uk/about/living-heritage/evolutionofparliament/originsofparliament/birthofparliament/overview/magnacarta/) | 2026-10-12 |
| qpk-history-014 | Hard | Articles of Confederation | The Articles of Confederation preceded the present Constitution. | [Reference](https://www.archives.gov/milestone-documents/list) | 2026-10-12 |
| qpk-history-015 | Hard | 1863 | Lincoln's proclamation took effect on 1 January 1863. | [Reference](https://www.archives.gov/milestone-documents/emancipation-proclamation) | 2026-10-12 |
| qpk-history-016 | Expert | Khufu | The Great Pyramid was built for Khufu. | [Reference](https://egymonuments.gov.eg/monuments/the-great-pyramid/) | 2026-10-12 |
| qpk-history-017 | Expert | Memphis | The World Heritage property is Memphis and its Necropolis. | [Reference](https://whc.unesco.org/en/list/86/) | 2026-10-12 |
| qpk-history-018 | Expert | International Court of Justice | The ICJ Statute is annexed to the UN Charter. | [Reference](https://www.un.org/en/about-us/un-charter/full-text) | 2026-10-12 |
| qpk-history-019 | Expert | 15th century | Machu Picchu was built in the fifteenth century. | [Reference](https://whc.unesco.org/en/list/274/) | 2026-10-12 |
| qpk-history-020 | Expert | 1297 | The Archives displays Edward I's 1297 reissue. | [Reference](https://www.archives.gov/exhibits/featured-documents/magna-carta) | 2026-10-12 |
| qpk-geography-001 | Easy | Pacific Ocean | The Pacific is Earth's largest ocean basin. | [Reference](https://oceanservice.noaa.gov/facts/biggestocean.html) | 2026-10-12 |
| qpk-geography-002 | Easy | Asia | Asia is the largest continent. | [Reference](https://education.nationalgeographic.org/resource/Continent/) | 2026-10-12 |
| qpk-geography-003 | Easy | Equator | The Equator is the reference line for latitude. | [Reference](https://education.nationalgeographic.org/resource/equator/) | 2026-10-12 |
| qpk-geography-004 | Easy | Tokyo | Tokyo is Japan's capital. | [Reference](https://www.geonames.org/countries/) | 2026-10-12 |
| qpk-geography-005 | Easy | Islamabad | Islamabad is the capital of Pakistan. | [Reference](https://www.geonames.org/countries/) | 2026-10-12 |
| qpk-geography-006 | Normal | Mount Everest | Everest has the highest elevation above mean sea level. | [Reference](https://education.nationalgeographic.org/resource/mount-everest/) | 2026-10-12 |
| qpk-geography-007 | Normal | Antarctica | Antarctica has research stations but no indigenous permanent population. | [Reference](https://education.nationalgeographic.org/resource/antarctica/) | 2026-10-12 |
| qpk-geography-008 | Normal | Ottawa | Ottawa is Canada's national capital. | [Reference](https://www.geonames.org/countries/) | 2026-10-12 |
| qpk-geography-009 | Normal | Lake Baikal | Lake Baikal is the deepest lake on Earth. | [Reference](https://whc.unesco.org/en/list/754/) | 2026-10-12 |
| qpk-geography-010 | Normal | Andes | The Andes extend along South America's western edge. | [Reference](https://www.britannica.com/place/Andes-Mountains) | 2026-10-12 |
| qpk-geography-011 | Hard | Strait of Gibraltar | Gibraltar separates southern Spain from northern Morocco. | [Reference](https://www.britannica.com/place/Strait-of-Gibraltar) | 2026-10-12 |
| qpk-geography-012 | Hard | Prime meridian | Longitude is measured east or west of the prime meridian. | [Reference](https://education.nationalgeographic.org/resource/longitude/) | 2026-10-12 |
| qpk-geography-013 | Hard | Pacific Ocean | Challenger Deep is in the Mariana Trench in the Pacific. | [Reference](https://oceanservice.noaa.gov/facts/oceandepth.html) | 2026-10-12 |
| qpk-geography-014 | Hard | Ulaanbaatar | Ulaanbaatar is Mongolia's capital. | [Reference](https://www.geonames.org/countries/) | 2026-10-12 |
| qpk-geography-015 | Hard | Southern Hemisphere | South latitude places a location south of the Equator. | [Reference](https://education.nationalgeographic.org/resource/hemisphere/) | 2026-10-12 |
| qpk-geography-016 | Expert | 180 degrees | The Date Line broadly follows the 180-degree meridian. | [Reference](https://education.nationalgeographic.org/resource/date-line/) | 2026-10-12 |
| qpk-geography-017 | Expert | 60 | A degree is divided into 60 minutes of arc. | [Reference](https://education.nationalgeographic.org/resource/latitude/) | 2026-10-12 |
| qpk-geography-018 | Expert | 90 degrees north | The North Pole is at 90 degrees north latitude. | [Reference](https://education.nationalgeographic.org/resource/longitude/) | 2026-10-12 |
| qpk-geography-019 | Expert | Northern and Eastern | N indicates north of the Equator and E indicates east longitude. | [Reference](https://education.nationalgeographic.org/resource/hemisphere/) | 2026-10-12 |
| qpk-geography-020 | Expert | Lake Baikal | UNESCO describes Baikal as holding about 20 percent of unfrozen freshwater reserves. | [Reference](https://whc.unesco.org/en/list/754/) | 2026-10-12 |
| qpk-technology-001 | Easy | HTML | HTML is a markup language used to structure web documents. | [Reference](https://www.w3.org/TR/html401/intro/intro.html) | 2026-10-12 |
| qpk-technology-002 | Easy | Presentation and styling | CSS controls the presentation of structured documents. | [Reference](https://www.w3.org/Style/CSS/Overview.en.html) | 2026-10-12 |
| qpk-technology-003 | Easy | Tim Berners-Lee | Berners-Lee invented the Web at CERN in 1989. | [Reference](https://www.w3.org/People/Berners-Lee/) | 2026-10-12 |
| qpk-technology-004 | Easy | 8 | A byte conventionally contains eight bits. | [Reference](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Uint8Array) | 2026-10-12 |
| qpk-technology-005 | Easy | Uniform Resource Locator | A URL identifies the location of a resource. | [Reference](https://developer.mozilla.org/en-US/docs/Glossary/URL) | 2026-10-12 |
| qpk-technology-006 | Normal | 0 and 1 | Base-two notation uses just zero and one. | [Reference](https://docs.python.org/3/library/functions.html#bin) | 2026-10-12 |
| qpk-technology-007 | Normal | 16 | The first place beyond the units in base sixteen has value sixteen. | [Reference](https://docs.python.org/3/library/functions.html#hex) | 2026-10-12 |
| qpk-technology-008 | Normal | Not Found | 404 indicates that the target resource was not found. | [Reference](https://www.rfc-editor.org/rfc/rfc9110.html) | 2026-10-12 |
| qpk-technology-009 | Normal | DNS | The Domain Name System maps names to resource records. | [Reference](https://www.rfc-editor.org/rfc/rfc1034.html) | 2026-10-12 |
| qpk-technology-010 | Normal | JSON | JSON represents structured data with objects and arrays. | [Reference](https://www.rfc-editor.org/rfc/rfc8259.html) | 2026-10-12 |
| qpk-technology-011 | Hard | 128 | IPv6 addresses are 128 bits long. | [Reference](https://www.rfc-editor.org/rfc/rfc8200.html) | 2026-10-12 |
| qpk-technology-012 | Hard | GET | GET requests transfer of a current representation of the target resource. | [Reference](https://www.rfc-editor.org/rfc/rfc9110.html) | 2026-10-12 |
| qpk-technology-013 | Hard | A uniquely identified row | A primary key identifies rows in a table. | [Reference](https://www.sqlite.org/lang_createtable.html) | 2026-10-12 |
| qpk-technology-014 | Hard | WHERE | WHERE specifies the row selection condition. | [Reference](https://www.sqlite.org/lang.html) | 2026-10-12 |
| qpk-technology-015 | Hard | 201 | 201 Created reports that the request created one or more resources. | [Reference](https://www.rfc-editor.org/rfc/rfc9110.html) | 2026-10-12 |
| qpk-technology-016 | Expert | 1 to 4 | UTF-8 uses sequences of one through four bytes. | [Reference](https://www.rfc-editor.org/rfc/rfc3629.html) | 2026-10-12 |
| qpk-technology-017 | Expert | HEAD | HEAD requests headers without transferring the representation content. | [Reference](https://www.rfc-editor.org/rfc/rfc9110.html) | 2026-10-12 |
| qpk-technology-018 | Expert | 255 | Eight bits allow 256 values, numbered zero through 255. | [Reference](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Uint8Array) | 2026-10-12 |
| qpk-technology-019 | Expert | Atomicity | Atomicity means a transaction's changes occur as one unit. | [Reference](https://www.sqlite.org/transactional.html) | 2026-10-12 |
| qpk-technology-020 | Expert | null | JSON defines the literal null; undefined is not a JSON value. | [Reference](https://www.rfc-editor.org/rfc/rfc8259.html) | 2026-10-12 |
| qpk-sports-001 | Easy | 11 | The Laws of the Game specify a maximum of eleven players per team. | [Reference](https://www.theifab.com/laws/latest/the-players/) | 2026-10-12 |
| qpk-sports-002 | Easy | 6 | An over contains six valid balls; wides and no-balls do not count. | [Reference](https://www.lords.org/mcc/laws-of-cricket/laws/law-17-the-over/) | 2026-10-12 |
| qpk-sports-003 | Easy | Badminton | A shuttle is used in badminton. | [Reference](https://corporate.bwfbadminton.com/statutes/) | 2026-10-12 |
| qpk-sports-004 | Easy | Zero | Love denotes a score of zero. | [Reference](https://www.itftennis.com/en/about-us/governance/rules-and-regulations/) | 2026-10-12 |
| qpk-sports-005 | Easy | 5 | The Olympic symbol consists of five interlaced rings. | [Reference](https://olympics.com/ioc/olympic-rings) | 2026-10-12 |
| qpk-sports-006 | Normal | 42.195 km | The marathon is run over 42.195 kilometres. | [Reference](https://worldathletics.org/disciplines/road-running/marathon) | 2026-10-12 |
| qpk-sports-007 | Normal | 3 | A wicket comprises three stumps with two bails on top. | [Reference](https://www.lords.org/mcc/laws-of-cricket/laws/law-8-the-wickets/) | 2026-10-12 |
| qpk-sports-008 | Normal | 90 minutes | A match has two equal halves of 45 minutes. | [Reference](https://www.theifab.com/laws/latest/the-duration-of-the-match/) | 2026-10-12 |
| qpk-sports-009 | Normal | 1 | A free throw scores one point. | [Reference](https://about.fiba.basketball/en/our-sport/official-basketball-rules) | 2026-10-12 |
| qpk-sports-010 | Normal | Deuce | At 40-all the score is deuce. | [Reference](https://www.itftennis.com/en/about-us/governance/rules-and-regulations/) | 2026-10-12 |
| qpk-sports-011 | Hard | 22 yards | The pitch is 22 yards long. | [Reference](https://www.lords.org/mcc/laws-of-cricket/laws/law-6-the-pitch/) | 2026-10-12 |
| qpk-sports-012 | Hard | 11 metres | The penalty mark is eleven metres from the midpoint between the posts. | [Reference](https://www.theifab.com/laws/latest/the-field-of-play/) | 2026-10-12 |
| qpk-sports-013 | Hard | 10 | The decathlon combines ten track and field events. | [Reference](https://worldathletics.org/disciplines/combined-events/decathlon) | 2026-10-12 |
| qpk-sports-014 | Hard | 3000 metres steeplechase | The steeplechase combines barriers with a water jump. | [Reference](https://worldathletics.org/disciplines/middlelong/3000-metres-steeplechase) | 2026-10-12 |
| qpk-sports-015 | Hard | 5 | Five players from each team are on the playing court. | [Reference](https://about.fiba.basketball/en/our-sport/official-basketball-rules) | 2026-10-12 |
| qpk-sports-016 | Expert | 7 | A match may not start or continue with fewer than seven players on a team. | [Reference](https://www.theifab.com/laws/latest/the-players/) | 2026-10-12 |
| qpk-sports-017 | Expert | 9 inches | The three stumps form a wicket nine inches wide. | [Reference](https://www.lords.org/mcc/laws-of-cricket/laws/law-8-the-wickets/) | 2026-10-12 |
| qpk-sports-018 | Expert | No | There is no offside offence when receiving directly from a throw-in. | [Reference](https://www.theifab.com/laws/latest/offside/) | 2026-10-12 |
| qpk-sports-019 | Expert | 2 | A player must win the advantage point and then the next point. | [Reference](https://www.itftennis.com/en/about-us/governance/rules-and-regulations/) | 2026-10-12 |
| qpk-sports-020 | Expert | 35 | The race has 28 hurdle jumps and seven water jumps. | [Reference](https://worldathletics.org/disciplines/middlelong/3000-metres-steeplechase) | 2026-10-12 |
| qpk-movies-001 | Easy | Toy Story | Woody and Buzz are the central toy characters in Toy Story. | [Reference](https://www.pixar.com/toy-story) | 2026-10-12 |
| qpk-movies-002 | Easy | Clownfish | Nemo and his father Marlin are clownfish. | [Reference](https://www.pixar.com/finding-nemo) | 2026-10-12 |
| qpk-movies-003 | Easy | Inside Out | Inside Out personifies Riley's emotions. | [Reference](https://www.pixar.com/inside-out) | 2026-10-12 |
| qpk-movies-004 | Easy | Monsters, Inc. | Mike Wazowski and James P. Sullivan work at Monsters, Inc. | [Reference](https://www.pixar.com/monsters-inc) | 2026-10-12 |
| qpk-movies-005 | Easy | Lightning McQueen | Lightning McQueen is the red racing car at the center of Cars. | [Reference](https://www.pixar.com/cars) | 2026-10-12 |
| qpk-movies-006 | Normal | Wings | Wings received the award at the first Oscar ceremony. | [Reference](https://www.oscars.org/oscars/ceremonies/1929) | 2026-10-12 |
| qpk-movies-007 | Normal | Shrek | Shrek won the inaugural Animated Feature award in 2002. | [Reference](https://www.oscars.org/oscars/ceremonies/2002) | 2026-10-12 |
| qpk-movies-008 | Normal | James Cameron | Cameron won the directing Oscar for Titanic. | [Reference](https://www.oscars.org/oscars/ceremonies/1998) | 2026-10-12 |
| qpk-movies-009 | Normal | Hayao Miyazaki | Miyazaki directed the 2003 Animated Feature Oscar winner. | [Reference](https://www.oscars.org/oscars/ceremonies/2003) | 2026-10-12 |
| qpk-movies-010 | Normal | Peter Jackson | Peter Jackson won the directing Oscar for the film. | [Reference](https://www.oscars.org/oscars/ceremonies/2004) | 2026-10-12 |
| qpk-movies-011 | Hard | 1937 | Disney's first full-length animated feature premiered in 1937. | [Reference](https://d23.com/a-to-z/snow-white-and-the-seven-dwarfs-film/) | 2026-10-12 |
| qpk-movies-012 | Hard | Titanic | Titanic won Best Picture at the 70th Academy Awards. | [Reference](https://www.oscars.org/oscars/ceremonies/1998) | 2026-10-12 |
| qpk-movies-013 | Hard | Finding Nemo | Finding Nemo won the 2004 Animated Feature Oscar. | [Reference](https://www.oscars.org/oscars/ceremonies/2004) | 2026-10-12 |
| qpk-movies-014 | Hard | 11 | It won all eleven categories in which it was nominated. | [Reference](https://www.oscars.org/oscars/ceremonies/2004) | 2026-10-12 |
| qpk-movies-015 | Hard | Academy Award of Merit | The Academy Award of Merit is commonly called the Oscar. | [Reference](https://www.oscars.org/oscars/statuette) | 2026-10-12 |
| qpk-movies-016 | Expert | Emil Jannings | Jannings won for The Last Command and The Way of All Flesh. | [Reference](https://www.oscars.org/oscars/ceremonies/1929) | 2026-10-12 |
| qpk-movies-017 | Expert | Janet Gaynor | Gaynor received the first Actress Oscar for three performances. | [Reference](https://www.oscars.org/oscars/ceremonies/1929) | 2026-10-12 |
| qpk-movies-018 | Expert | Hollywood Roosevelt Hotel | The first ceremony was held in the Roosevelt's Blossom Room. | [Reference](https://www.oscars.org/oscars/statuette) | 2026-10-12 |
| qpk-movies-019 | Expert | Cedric Gibbons | MGM art director Cedric Gibbons designed the statuette. | [Reference](https://www.oscars.org/oscars/statuette) | 2026-10-12 |
| qpk-movies-020 | Expert | Painted plaster | A wartime metal shortage led to painted plaster Oscars. | [Reference](https://www.oscars.org/oscars/statuette) | 2026-10-12 |
| qpk-music-001 | Easy | 5 | A standard staff has five lines and four spaces. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/notation-of-notes-clefs-and-ledger-lines/) | 2026-10-12 |
| qpk-music-002 | Easy | Treble clef | The treble clef identifies the G line. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/notation-of-notes-clefs-and-ledger-lines/) | 2026-10-12 |
| qpk-music-003 | Easy | Speed of the beat | Tempo describes how fast or slowly the beat moves. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/other-aspects-of-notation/) | 2026-10-12 |
| qpk-music-004 | Easy | Loud | F stands for forte, meaning loud. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/other-aspects-of-notation/) | 2026-10-12 |
| qpk-music-005 | Easy | 1 | In 4/4 the quarter note is the beat unit. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/simple-meter-and-time-signatures/) | 2026-10-12 |
| qpk-music-006 | Normal | Bass clef | The bass clef identifies the F line. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/notation-of-notes-clefs-and-ledger-lines/) | 2026-10-12 |
| qpk-music-007 | Normal | Gradually louder | Crescendo calls for a gradual increase in loudness. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/other-aspects-of-notation/) | 2026-10-12 |
| qpk-music-008 | Normal | 12 | An octave spans twelve equal semitone steps. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/intervals/) | 2026-10-12 |
| qpk-music-009 | Normal | Allegro | Allegro is a fast tempo marking. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/other-aspects-of-notation/) | 2026-10-12 |
| qpk-music-010 | Normal | 3 | A triad contains a root, a third and a fifth. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/triads/) | 2026-10-12 |
| qpk-music-011 | Hard | Perfect fifth | C to G spans a perfect fifth. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/intervals/) | 2026-10-12 |
| qpk-music-012 | Hard | 0 | C major uses no sharps or flats in its key signature. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/major-scales/) | 2026-10-12 |
| qpk-music-013 | Hard | A minor | A natural minor shares C major's key signature. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/minor-scales/) | 2026-10-12 |
| qpk-music-014 | Hard | Melodic minor | Traditional melodic minor raises degrees six and seven ascending. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/minor-scales/) | 2026-10-12 |
| qpk-music-015 | Hard | Minor third | A minor triad has a minor third above the root. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/triads/) | 2026-10-12 |
| qpk-music-016 | Expert | Minor sixth | Inverted interval sizes add to nine, and major becomes minor. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/intervals/) | 2026-10-12 |
| qpk-music-017 | Expert | 6 | An augmented fourth is a tritone of six semitones. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/intervals/) | 2026-10-12 |
| qpk-music-018 | Expert | C, E-flat, G-flat | A diminished triad combines two minor thirds. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/triads/) | 2026-10-12 |
| qpk-music-019 | Expert | E major | E major has F-sharp, C-sharp, G-sharp and D-sharp. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/major-scales/) | 2026-10-12 |
| qpk-music-020 | Expert | Fifth | The dominant is the fifth scale degree. | [Reference](https://viva.pressbooks.pub/openmusictheory/chapter/major-scales/) | 2026-10-12 |
| qpk-literature-001 | Easy | Jane Austen | Pride and Prejudice is a novel by Jane Austen. | [Reference](https://www.gutenberg.org/ebooks/1342) | 2026-10-12 |
| qpk-literature-002 | Easy | Mary Shelley | Mary Shelley wrote Frankenstein. | [Reference](https://www.gutenberg.org/ebooks/84) | 2026-10-12 |
| qpk-literature-003 | Easy | William Shakespeare | Romeo and Juliet is a tragedy by Shakespeare. | [Reference](https://www.gutenberg.org/ebooks/1513) | 2026-10-12 |
| qpk-literature-004 | Easy | Charles Dickens | Dickens wrote the novel set in London and Paris. | [Reference](https://www.gutenberg.org/ebooks/98) | 2026-10-12 |
| qpk-literature-005 | Easy | Lewis Carroll | Lewis Carroll is the pen name used by Charles Dodgson. | [Reference](https://www.gutenberg.org/ebooks/11) | 2026-10-12 |
| qpk-literature-006 | Normal | Sherlock Holmes | Holmes shares the Baker Street lodgings with Dr Watson. | [Reference](https://www.gutenberg.org/ebooks/1661) | 2026-10-12 |
| qpk-literature-007 | Normal | Bennet | The novel follows the Bennet family. | [Reference](https://www.gutenberg.org/ebooks/1342) | 2026-10-12 |
| qpk-literature-008 | Normal | Ahab | Captain Ahab pursues the white whale. | [Reference](https://www.gutenberg.org/ebooks/2701) | 2026-10-12 |
| qpk-literature-009 | Normal | A vampire | Stoker's Count Dracula is a vampire. | [Reference](https://www.gutenberg.org/ebooks/345) | 2026-10-12 |
| qpk-literature-010 | Normal | Great Expectations | Pip is the narrator of Great Expectations. | [Reference](https://www.gutenberg.org/ebooks/1400) | 2026-10-12 |
| qpk-literature-011 | Hard | Oscar Wilde | Wilde's novel centers on Dorian Gray and his portrait. | [Reference](https://www.gutenberg.org/ebooks/174) | 2026-10-12 |
| qpk-literature-012 | Hard | H.G. Wells | Wells published The Time Machine in 1895. | [Reference](https://www.gutenberg.org/ebooks/35) | 2026-10-12 |
| qpk-literature-013 | Hard | Robert Louis Stevenson | Stevenson wrote the novella about Jekyll and Hyde. | [Reference](https://www.gutenberg.org/ebooks/43) | 2026-10-12 |
| qpk-literature-014 | Hard | Emily Bronte | Wuthering Heights is Emily Bronte's novel. | [Reference](https://www.gutenberg.org/ebooks/768) | 2026-10-12 |
| qpk-literature-015 | Hard | George Eliot | George Eliot was the pen name of Mary Ann Evans. | [Reference](https://www.gutenberg.org/ebooks/145) | 2026-10-12 |
| qpk-literature-016 | Expert | Victor Frankenstein | Victor Frankenstein creates the unnamed creature. | [Reference](https://www.gutenberg.org/ebooks/84) | 2026-10-12 |
| qpk-literature-017 | Expert | The Tempest | Prospero is the exiled duke and magician in The Tempest. | [Reference](https://www.gutenberg.org/ebooks/23042) | 2026-10-12 |
| qpk-literature-018 | Expert | Hispaniola | The Hispaniola carries the treasure-seeking expedition. | [Reference](https://www.gutenberg.org/ebooks/120) | 2026-10-12 |
| qpk-literature-019 | Expert | Middlemarch | Dorothea Brooke is a central character in Middlemarch. | [Reference](https://www.gutenberg.org/ebooks/145) | 2026-10-12 |
| qpk-literature-020 | Expert | Ishmael | Ishmael is the novel's first-person narrator. | [Reference](https://www.gutenberg.org/ebooks/2701) | 2026-10-12 |
| qpk-animals-001 | Easy | Bamboo | Giant pandas feed chiefly on bamboo. | [Reference](https://nationalzoo.si.edu/animals/giant-panda) | 2026-10-12 |
| qpk-animals-002 | Easy | Cheetah | Cheetahs are specialized for short bursts of high-speed running. | [Reference](https://nationalzoo.si.edu/animals/cheetah) | 2026-10-12 |
| qpk-animals-003 | Easy | Giraffe | Giraffes are the tallest living land animals. | [Reference](https://animals.sandiegozoo.org/animals/giraffe) | 2026-10-12 |
| qpk-animals-004 | Easy | Birds | Penguins are flightless marine birds. | [Reference](https://zoo.sandiegozoo.org/animals/penguin) | 2026-10-12 |
| qpk-animals-005 | Easy | 8 | An octopus has eight arms. | [Reference](https://www.montereybayaquarium.org/animals/animals-a-to-z/giant-pacific-octopus) | 2026-10-12 |
| qpk-animals-006 | Normal | Platypus | The platypus is an egg-laying monotreme. | [Reference](https://animals.sandiegozoo.org/animals/platypus) | 2026-10-12 |
| qpk-animals-007 | Normal | Eucalyptus | Koalas mainly eat eucalyptus leaves. | [Reference](https://animals.sandiegozoo.org/animals/koala) | 2026-10-12 |
| qpk-animals-008 | Normal | Whale shark | Whale sharks are the largest living fish. | [Reference](https://oceanservice.noaa.gov/facts/bigfish.html) | 2026-10-12 |
| qpk-animals-009 | Normal | Bats | Bats are the only mammals with true powered flight. | [Reference](https://www.nps.gov/yose/learn/nature/bats.htm) | 2026-10-12 |
| qpk-animals-010 | Normal | Butterflies | A caterpillar is the larval stage of a butterfly. | [Reference](https://www.fs.usda.gov/wildflowers/pollinators/animals/butterflies.shtml) | 2026-10-12 |
| qpk-animals-011 | Hard | Pigments in their diet | Dietary carotenoids contribute to flamingos' pink plumage. | [Reference](https://nationalzoo.si.edu/animals/american-flamingo) | 2026-10-12 |
| qpk-animals-012 | Hard | Platypus | The platypus detects weak electric fields with receptors in its bill. | [Reference](https://animals.sandiegozoo.org/animals/platypus) | 2026-10-12 |
| qpk-animals-013 | Hard | 3 | Two hearts serve the gills and one serves the rest of the body. | [Reference](https://www.montereybayaquarium.org/animals/animals-a-to-z/giant-pacific-octopus) | 2026-10-12 |
| qpk-animals-014 | Hard | Marsupial | Koalas are marsupials that rear young in a pouch. | [Reference](https://animals.sandiegozoo.org/animals/koala) | 2026-10-12 |
| qpk-animals-015 | Hard | Pollination | Bees carry pollen while visiting flowers. | [Reference](https://www.fs.usda.gov/wildflowers/pollinators/animals/bees.shtml) | 2026-10-12 |
| qpk-animals-016 | Expert | Axolotl | Axolotls retain larval features including external gills. | [Reference](https://animals.sandiegozoo.org/animals/axolotl) | 2026-10-12 |
| qpk-animals-017 | Expert | Xochimilco | Wild axolotls are native to the Xochimilco water system. | [Reference](https://animals.sandiegozoo.org/animals/axolotl) | 2026-10-12 |
| qpk-animals-018 | Expert | A pseudo-thumb | An enlarged wrist bone functions like an opposable thumb. | [Reference](https://nationalzoo.si.edu/animals/giant-panda) | 2026-10-12 |
| qpk-animals-019 | Expert | Acinonyx jubatus | The cheetah belongs to the genus Acinonyx. | [Reference](https://nationalzoo.si.edu/animals/cheetah) | 2026-10-12 |
| qpk-animals-020 | Expert | Asian elephant | Asian elephants generally have smaller ears than African elephants. | [Reference](https://nationalzoo.si.edu/animals/asian-elephant) | 2026-10-12 |
| qpk-food-001 | Easy | Soybean | Tofu is made from coagulated soy milk. | [Reference](https://dictionary.cambridge.org/dictionary/english/tofu) | 2026-10-12 |
| qpk-food-002 | Easy | Rice vinegar | Sushi uses rice seasoned with vinegar, commonly rice vinegar. | [Reference](https://www.breville.com/content/dam/breville/nz/assets/miscellaneous/instruction-manual/cookers/LRC480-instruction-manual.pdf) | 2026-10-12 |
| qpk-food-003 | Easy | Saffron | Saffron comes from the stigmas of Crocus sativus. | [Reference](https://mpns.science.kew.org/mpns-portal/drugDetail?drugName=saffron) | 2026-10-12 |
| qpk-food-004 | Easy | Bark | Cinnamon is obtained from the inner bark of certain trees. | [Reference](https://www.kew.org/plants/cinnamon) | 2026-10-12 |
| qpk-food-005 | Easy | Avocado | Guacamole is an avocado-based preparation. | [Reference](https://www.kew.org/plants/avocado) | 2026-10-12 |
| qpk-food-006 | Normal | Yeast | Yeast ferments sugars and produces carbon dioxide. | [Reference](https://openstax.org/books/biology-2e/pages/24-1-characteristics-of-fungi) | 2026-10-12 |
| qpk-food-007 | Normal | Chickpea | Hummus is commonly made with chickpeas and tahini. | [Reference](https://dictionary.cambridge.org/dictionary/english/hummus) | 2026-10-12 |
| qpk-food-008 | Normal | Sesame | Tahini is ground sesame seed paste. | [Reference](https://dictionary.cambridge.org/dictionary/english/tahini) | 2026-10-12 |
| qpk-food-009 | Normal | Cacao | Cocoa is produced from cacao beans. | [Reference](https://www.kew.org/plants/cacao-tree) | 2026-10-12 |
| qpk-food-010 | Normal | Vitamin C | Citrus fruits are well-known dietary sources of vitamin C. | [Reference](https://ods.od.nih.gov/factsheets/VitaminC-Consumer/) | 2026-10-12 |
| qpk-food-011 | Hard | Gluten | Hydrated wheat proteins form gluten in dough. | [Reference](https://www.kingarthurbaking.com/learn/guides/sourdough/faq) | 2026-10-12 |
| qpk-food-012 | Hard | Miso | Miso is a fermented soybean seasoning. | [Reference](https://dictionary.cambridge.org/dictionary/english/miso) | 2026-10-12 |
| qpk-food-013 | Hard | Cloves | Cloves are the dried unopened buds of the clove tree. | [Reference](https://www.kew.org/read-and-watch/christmas-spices) | 2026-10-12 |
| qpk-food-014 | Hard | Rhizome | Culinary ginger is an underground stem called a rhizome. | [Reference](https://www.kew.org/plants/ginger) | 2026-10-12 |
| qpk-food-015 | Hard | Fermentation | Yogurt cultures ferment milk sugars into lactic acid. | [Reference](https://books.lib.uoguelph.ca/dairyscienceandtechnologyebook/chapter/cultured-dairy-products-and-cheese/) | 2026-10-12 |
| qpk-food-016 | Expert | Pseudocereal | Quinoa is not a grass, but its seeds are used like cereal grains. | [Reference](https://www.kew.org/plants/quinoa) | 2026-10-12 |
| qpk-food-017 | Expert | Capsaicin | Capsaicin activates heat-sensitive receptors. | [Reference](https://www.acs.org/molecule-of-the-week/archive/c/capsaicin.html) | 2026-10-12 |
| qpk-food-018 | Expert | Mace | Mace is the dried aril; nutmeg is the seed. | [Reference](https://powo.science.kew.org/taxon/urn:lsid:ipni.org:names:77126699-1/general-information) | 2026-10-12 |
| qpk-food-019 | Expert | Rennet | Rennet enzymes coagulate milk proteins into curds. | [Reference](https://books.lib.uoguelph.ca/cheesemakingtechnologyebook/chapter/1-introduction-to-cheese-making/) | 2026-10-12 |
| qpk-food-020 | Expert | Maillard reaction | The Maillard reaction creates many browned colors and aromas. | [Reference](https://pubs.acs.org/doi/10.1021/acs.jafc.7b00882) | 2026-10-12 |
| qpk-space-001 | Easy | 8 | The solar system has eight recognized planets. | [Reference](https://science.nasa.gov/solar-system/planets/) | 2026-10-12 |
| qpk-space-002 | Easy | Mercury | Mercury is the innermost planet. | [Reference](https://science.nasa.gov/mercury/facts/) | 2026-10-12 |
| qpk-space-003 | Easy | Mars | Iron minerals give Mars its reddish appearance. | [Reference](https://science.nasa.gov/mars/facts/) | 2026-10-12 |
| qpk-space-004 | Easy | Jupiter | Jupiter is the solar system's largest planet. | [Reference](https://science.nasa.gov/jupiter/facts/) | 2026-10-12 |
| qpk-space-005 | Easy | The Moon | Earth has one natural moon. | [Reference](https://science.nasa.gov/moon/facts/) | 2026-10-12 |
| qpk-space-006 | Normal | Venus | Venus's dense atmosphere traps heat efficiently. | [Reference](https://science.nasa.gov/venus/venus-facts/) | 2026-10-12 |
| qpk-space-007 | Normal | Mercury and Venus | Mercury and Venus have no natural moons. | [Reference](https://science.nasa.gov/solar-system/solar-system-facts/) | 2026-10-12 |
| qpk-space-008 | Normal | Mars and Jupiter | The main asteroid belt lies between Mars and Jupiter. | [Reference](https://science.nasa.gov/solar-system/solar-system-facts/) | 2026-10-12 |
| qpk-space-009 | Normal | Neptune | Neptune is the outermost of the eight planets. | [Reference](https://science.nasa.gov/neptune/facts/) | 2026-10-12 |
| qpk-space-010 | Normal | Dwarf planet | Pluto is a dwarf planet in the Kuiper Belt. | [Reference](https://science.nasa.gov/dwarf-planets/pluto/facts/) | 2026-10-12 |
| qpk-space-011 | Hard | Uranus | Uranus's extreme axial tilt makes its rotation unusual. | [Reference](https://science.nasa.gov/uranus/facts/) | 2026-10-12 |
| qpk-space-012 | Hard | Ganymede | Ganymede is also the largest moon in the solar system. | [Reference](https://science.nasa.gov/jupiter/jupiter-moons/ganymede/facts/) | 2026-10-12 |
| qpk-space-013 | Hard | Cassini | Cassini orbited Saturn from 2004 until 2017. | [Reference](https://science.nasa.gov/mission/cassini/) | 2026-10-12 |
| qpk-space-014 | Hard | Apollo 11 | Apollo 11 landed Neil Armstrong and Buzz Aldrin in 1969. | [Reference](https://www.nasa.gov/mission/apollo-11/) | 2026-10-12 |
| qpk-space-015 | Hard | Phobos and Deimos | Mars has two small moons, Phobos and Deimos. | [Reference](https://science.nasa.gov/mars/facts/) | 2026-10-12 |
| qpk-space-016 | Expert | Io | Io's intense volcanic activity is driven by tidal heating. | [Reference](https://science.nasa.gov/jupiter/jupiter-moons/io/facts/) | 2026-10-12 |
| qpk-space-017 | Expert | Ganymede | Ganymede is the only known moon with its own intrinsic magnetic field. | [Reference](https://science.nasa.gov/jupiter/jupiter-moons/ganymede/facts/) | 2026-10-12 |
| qpk-space-018 | Expert | Europa | Europa has strong evidence for a subsurface saltwater ocean. | [Reference](https://science.nasa.gov/jupiter/jupiter-moons/europa/europa-facts/) | 2026-10-12 |
| qpk-space-019 | Expert | 165 years | Neptune takes about 165 Earth years to orbit the Sun. | [Reference](https://science.nasa.gov/neptune/facts/) | 2026-10-12 |
| qpk-space-020 | Expert | Charon | Charon is unusually large relative to Pluto. | [Reference](https://science.nasa.gov/dwarf-planets/pluto/facts/) | 2026-10-12 |
