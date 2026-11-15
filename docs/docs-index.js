// Map of everything under docs/: one entry per document, with the files that belong to it.
// Run:  node docs/docs-index.js        -> checks the map against the folder, rewrites docs/INDEX.md
// Exit code 1 if a listed path is missing or a file under docs/ is not covered by any entry.
// The Notion database "LauWell • Table of Content" has one row per entry (Doc Location = /docs, Repo link = `main`).
const fs = require('fs');
const path = require('path');

const REPO = 'https://github.com/Laul/LauWell/blob/main/docs/';
const DH = 'data-structure/health-data-model/';

// main = the file to open; also = supporting files or folders (a trailing / covers the whole folder);
// history = older versions kept for reference; local = true when the folder is gitignored (no GitHub link).
const ENTRIES = [
  { name: 'Database schema (Postgres)', topic: 'Architecture', status: 'In Progress',
    main: 'architecture/backend/db-schema/lauwell-data-schema.html',
    also: ['architecture/backend/db-schema/schema-er.mmd', 'architecture/backend/db-schema/schema-flow.mmd', 'architecture/backend/db-schema/schema-tables.md'],
    what: 'The Postgres tables on Neon, how they connect, and the sync flow. Identity, entries and files are built (backend/db/migrations); thing, detail and sensor tables are still proposals, built with their features (milestone M9). The .mmd files are the diagram sources, schema-tables.md lists each table\'s columns and the agreed rules.' },
  { name: 'C4 diagrams (levels 1 to 3)', topic: 'Architecture', status: 'In Progress',
    main: 'architecture/c4/lauwell-c4.html',
    also: ['architecture/c4/'],
    what: 'Context, containers and components of the app, Firebase and Postgres. The .mmd files are the sources, the README says how they were drawn.' },
  { name: 'Shared foundation: things, acts and links', topic: 'Architecture', status: 'Done',
    main: 'architecture/backend/shared-foundation.md',
    what: 'The two families of records (things and acts) and how records link to each other. Settled and built (2026-11-09, milestone M4); mood moved to these tables on 2026-11-11.' },
  { name: 'Feature modules and on/off toggles', topic: 'Architecture', status: 'Done',
    main: 'architecture/feature-modules-and-toggles.md',
    what: 'How the app is split into feature modules, how each is switched on or off, and the four-tab navigation.' },
  { name: 'Patient data model (data map)', topic: 'Data', status: 'In Progress',
    main: DH + 'Patient Data Categories.html',
    also: [DH + 'Patient Data Categories.csv', DH + 'Patient Data Model.csv', DH + 'Patient Data Codings.csv', DH + 'README.md', DH + '_src/'],
    history: [DH + 'Patient Data Categories-v03.html', DH + 'Patient Data Categories-v03.csv', DH + 'Patient Data Model-v03.csv'],
    what: 'Current reference: categories, sub-categories, storage shapes, 126 data points with screens, sources and FHIR resource, plus the LOINC/SNOMED coding sheet. Generated from the Notion table Data Model [Simplified] with _src/build.js.' },
  { name: 'Health data inventory (earlier category map)', topic: 'Data', status: 'Done',
    main: DH + 'Health Data • Inventory-v02.html',
    also: [DH + 'Health Data • Inventory-v02.csv'],
    history: [DH + 'Health Data • Inventory-v01.html', DH + 'Health Data • Inventory-v01.csv'],
    what: 'Earlier inventory of the health data to cover, kept as history for the current data map.' },
  { name: 'Common lab tests list', topic: 'Data', status: 'In Progress',
    main: 'data-structure/health_data_labs/common_lab_tests.csv',
    what: 'About 70 common lab tests with category, unit and normal range; input for the Labs work (Records tab).' },
  { name: 'Health data standardization (PDF)', topic: 'Data', status: 'Done',
    main: 'data-structure/health_data_standards/Health Data • Standardization.pdf',
    what: 'Background document on health data standards (LOINC, SNOMED, FHIR and others); the decision itself is on the Notion page Medical Record Standardization and Normalization.' },
  { name: 'Design directions', topic: 'FrontEnd', local: true,
    main: 'design/LauWell Design Directions.html',
    what: 'Visual directions explored for the app (made in Claude Desktop).' },
  { name: 'Final design (for Figma import)', topic: 'FrontEnd', local: true,
    main: 'design/LauWell Final Design (for Figma import).html',
    what: 'The chosen design (October 2026), packaged for import into Figma. Superseded by Full experience mockups v03.' },
  { name: 'Full experience mockups', topic: 'FrontEnd', local: true,
    main: 'design/Design • Full Experience - v03.html',
    history: ['design/Design • Full Experience - v01.html', 'design/Design • Full Experience - v02.html', 'design/Design • Today Screen.png'],
    what: 'The design reference (v03, 2026-10-24): every screen on the Galaxy Z Fold closed (the phone design) and open (Today dashboard, Tracker week board, list + detail), light and dark, with the Settings placement round (20b) merged in.' },
  { name: 'Global direction', topic: 'FrontEnd', local: true,
    main: 'design/Design • Global direction - v18.html',
    history: ['design/Design • Global direction - v13b.html', 'design/Design • Global direction - v13b-02.html', 'design/Design • Global direction - v15.html', 'design/Design • Global direction - v05.01.png', 'design/Design • Global direction - v05.02.png', 'design/Design • Global direction - v05.03.png'],
    what: 'Overall look and feel, latest version v18 (a zip despite its name, holding UI-spec.md and the icon list).' },
  { name: 'Settings placement', topic: 'FrontEnd', local: true,
    main: 'design/Design • Settings placement - v20b.html',
    history: ['design/Design • Settings placement - v20a.html'],
    what: 'Where Settings lives in the navigation (header with search and gear), latest version v20b, merged into Full experience mockups v03.' },
  { name: 'Icons', topic: 'FrontEnd', local: true,
    main: 'design/Icons/lauwell-icons.csv',
    also: ['design/Icons/'],
    what: 'Icon list (which icon is used where, with variant and color) and the custom SVGs.' },
  { name: 'Claude Design exports (2026-11-12)', topic: 'FrontEnd', local: true,
    main: 'design/LauWell • App design [Claude Export 20261112].zip',
    also: ['design/LauWell health hub design.zip'],
    what: 'Backup of the Claude Design projects: every round\'s source canvas (.dc.html), the exports (screens, tokens, icons) and the v18 and v20 handoffs. The second zip is the separate LauWell Health Hub canvas.' },
  { name: 'Review of existing health apps', topic: 'Requirements and Specifications', status: 'Done',
    main: 'research/Review • Existing Health Apps.html',
    what: 'Teardown of existing health apps that fed the requirements.' },
  { name: 'TrackAid review', topic: 'Architecture', status: 'Done',
    main: 'research/TrackAid review.md',
    what: 'Lessons from the earlier TrackAid prototype; summarised in PLAN.md under Decisions.' },
];

const root = __dirname;
const IGNORE = new Set(['.DS_Store', 'INDEX.md', 'docs-index.js']);
const all = [];
(function walk(d) {
  for (const f of fs.readdirSync(d, { withFileTypes: true })) {
    if (IGNORE.has(f.name) || f.name === 'node_modules') continue;
    const p = path.join(d, f.name);
    f.isDirectory() ? walk(p) : all.push(path.relative(root, p).split(path.sep).join('/'));
  }
})(root);

// Some file names contain a non-breaking space after the bullet, so compare names with spaces normalised
// and rewrite each entry's paths to the real file names.
const norm = x => x.replace(/\u00a0/g, ' ').normalize('NFC');
const real = new Map(all.map(f => [norm(f), f]));
const fix = p => (p.endsWith('/') ? p : real.get(norm(p)) || p);
for (const e of ENTRIES) {
  e.main = fix(e.main);
  if (e.also) e.also = e.also.map(fix);
  if (e.history) e.history = e.history.map(fix);
}
const claimed = new Set();
let bad = 0;
for (const e of ENTRIES) {
  for (const p of [e.main, ...(e.also || []), ...(e.history || [])]) {
    if (p.endsWith('/')) {
      const hits = all.filter(f => f.startsWith(p));
      if (!hits.length) { console.error('MISSING folder:', p); bad++; }
      hits.forEach(h => claimed.add(h));
    } else {
      if (!all.includes(p)) { console.error('MISSING file:', p); bad++; }
      claimed.add(p);
    }
  }
}
for (const f of all) if (!claimed.has(f)) { console.error('NOT IN THE MAP:', f); bad++; }

const link = e => (e.local ? '(local only, gitignored)' : REPO + encodeURI(e.main));
let md = '# Docs map\n\n*Generated by `node docs/docs-index.js` from the list inside that file. Do not edit by hand. ' +
  'The Notion database LauWell • Table of Content has one row per entry below.*\n';
let topic = '';
for (const e of [...ENTRIES].sort((a, b) => a.topic.localeCompare(b.topic))) {
  if (e.topic !== topic) { topic = e.topic; md += `\n## ${topic}\n`; }
  md += `\n### ${e.name}\n\n${e.what}\n\n- Open: \`docs/${e.main}\`${e.local ? ' (local only, gitignored)' : ''}\n`;
  if (e.also) md += `- Also: ${e.also.map(a => '`' + a + '`').join(', ')}\n`;
  if (e.history) md += `- History: ${e.history.map(a => '`' + a + '`').join(', ')}\n`;
}
fs.writeFileSync(path.join(root, 'INDEX.md'), md);
console.log(`${ENTRIES.length} entries, ${all.length} files checked, ${bad} problem(s). Wrote docs/INDEX.md`);
if (process.argv.includes('--json')) console.log(JSON.stringify(ENTRIES.map(e => ({ name: e.name, topic: e.topic, status: e.status || null, link: link(e), path: 'docs/' + e.main, also: e.also || [], history: e.history || [], what: e.what })), null, 1));
process.exit(bad ? 1 : 0);
