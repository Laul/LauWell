#!/usr/bin/env node
/*
 * Generates "Patient Data Categories.html" and ".csv" in the parent folder
 * from rows.js + template.html. One source, two outputs — so the page and the
 * spreadsheet can never disagree.
 *
 * Usage:  node docs/data-structure/_src/build.js
 * No dependencies. Edit rows.js to change the data; never edit the outputs.
 */
const fs = require('fs');
const path = require('path');

const SRC = __dirname;
const OUT = path.join(SRC, '..');
const NAME = 'Patient Data Categories';

const rows = require('./rows.js');
const template = fs.readFileSync(path.join(SRC, 'template.html'), 'utf8');

// --- validate before writing anything ---
const SHAPES = ['Measurement','Event','Interval','Standing fact','Catalogue','Document','Derived'];
const CATS = ['Vitals','Medical Records','Lifestyle','Symptoms','Treatments & appliances'];
const problems = [];
rows.forEach((r, i) => {
  if (r.length !== 5) problems.push(`row ${i} has ${r.length} fields, expected 5`);
  if (!CATS.includes(r[1])) problems.push(`row ${i} ("${r[0]}") unknown category: ${r[1]}`);
  if (!SHAPES.includes(r[3])) problems.push(`row ${i} ("${r[0]}") unknown shape: ${r[3]}`);
  if (!r[2]) problems.push(`row ${i} ("${r[0]}") missing sub-category`);
});
const names = rows.map(r => r[0]);
names.forEach((n, i) => { if (names.indexOf(n) !== i) problems.push(`duplicate data name: ${n}`); });
if (problems.length) {
  console.error('Validation failed:\n  ' + problems.join('\n  '));
  process.exit(1);
}

// --- HTML ---
if (!template.includes('/*__DATA__*/')) {
  console.error('template.html is missing the /*__DATA__*/ placeholder');
  process.exit(1);
}
fs.writeFileSync(path.join(OUT, NAME + '.html'), template.replace('/*__DATA__*/', JSON.stringify(rows)));

// --- CSV (RFC 4180: CRLF, every field quoted, no BOM) ---
const esc = v => '"' + String(v).replace(/"/g, '""') + '"';
const csv = [['Data','Category','Sub-category','Shape','Description'].map(esc).join(',')]
  .concat(rows.map(r => r.map(esc).join(','))).join('\r\n') + '\r\n';
fs.writeFileSync(path.join(OUT, NAME + '.csv'), csv);

// --- report ---
const count = (fn) => rows.reduce((m, r) => (m[fn(r)] = (m[fn(r)] || 0) + 1, m), {});
console.log(`${rows.length} rows -> ${NAME}.html + ${NAME}.csv`);
console.log('by category:', count(r => r[1]));
console.log('by shape:   ', count(r => r[3]));

// --- codings (standard terminology layer) ---
const { CODINGS, CODING_DEFAULTS } = require('./codings.js');
const ROLES = ['primary','component','variant'];
const CSTATUS = ['exact','approximate','candidate'];
const SYSTEMS = ['LOINC','SNOMED CT'];
const cproblems = [];
CODINGS.forEach((c, i) => {
  if (c.length !== 9) cproblems.push(`coding ${i} has ${c.length} fields, expected 9`);
  if (!names.includes(c[0])) cproblems.push(`coding ${i}: no data point named "${c[0]}" in rows.js`);
  if (!ROLES.includes(c[1])) cproblems.push(`coding ${i} ("${c[0]}") unknown role: ${c[1]}`);
  if (!CSTATUS.includes(c[2])) cproblems.push(`coding ${i} ("${c[0]}") unknown status: ${c[2]}`);
  if (!SYSTEMS.includes(c[3])) cproblems.push(`coding ${i} ("${c[0]}") unknown system: ${c[3]}`);
  if (c[3] === 'LOINC' && !/^\d{1,7}-\d$/.test(c[4])) cproblems.push(`coding ${i} ("${c[0]}") malformed LOINC code: ${c[4]}`);
  if (!['yes','no'].includes(c[7])) cproblems.push(`coding ${i} ("${c[0]}") verified must be yes/no`);
});
names.forEach(n => {
  const cs = CODINGS.filter(c => c[0] === n);
  if (cs.length && cs.filter(c => c[1] === 'primary').length !== 1) cproblems.push(`"${n}" needs exactly one primary coding`);
});
if (cproblems.length) {
  console.error('Coding validation failed:\n  ' + cproblems.join('\n  '));
  process.exit(1);
}
const CNAME = 'Patient Data Codings';
const ccsv = [['Data','Category','Sub-category','Shape','Role','Status','System','Code','Display','UCUM unit','Verified','Note'].map(esc).join(',')];
rows.forEach(r => {
  const cs = CODINGS.filter(c => c[0] === r[0]);
  if (cs.length) cs.forEach(c => ccsv.push([r[0], r[1], r[2], r[3], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8]].map(esc).join(',')));
  else ccsv.push([r[0], r[1], r[2], r[3], '', CODING_DEFAULTS.find(d => d[0](r))[1], '', '', '', '', '', ''].map(esc).join(','));
});
fs.writeFileSync(path.join(OUT, CNAME + '.csv'), ccsv.join('\r\n') + '\r\n');
const coded = new Set(CODINGS.map(c => c[0]));
const unverified = CODINGS.filter(c => c[7] === 'no').length;
console.log(`${coded.size} data points coded (${CODINGS.length} codings, ${unverified} still unverified) -> ${CNAME}.csv`);
console.log('uncoded by default status:', count(r => coded.has(r[0]) ? null : CODING_DEFAULTS.find(d => d[0](r))[1]));
