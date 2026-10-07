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
