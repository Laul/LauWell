// Standard-terminology codings for the data points in rows.js.
// One line per coding; a data point may have several (panel + components, variants).
//
// [ name, role, status, system, code, display, ucum, verified, note ]
//
//   name      must match a name in rows.js exactly (build.js checks)
//   role      primary | component | variant
//   status    exact | approximate | candidate   (candidate = pick one at verification time)
//   system    LOINC | SNOMED CT
//   ucum      UCUM unit string, or "" when the code has no numeric value
//   verified  "yes"  = code and display name confirmed against the LOINC search (NLM Clinical Tables)
//             "no"   = written from memory; confirm in the LOINC search before it ships
//
// Data points with no entry here get a default status from CODING_DEFAULTS below.
// Codings for rows dropped from the simplified model (HRV, weight, height, blood type…) are kept in
// `Patient Data Model-v03.csv`.
// Codes live on the metric definition, never on individual records.
const L = 'LOINC';
const CODINGS = [
  ["Heart Rate","primary","exact",L,"8867-4","Heart rate","/min","yes","Also the FHIR vital-signs code."],
  ["Heart Rate","variant","exact",L,"40443-4","Heart rate --resting","/min","yes","Resting heart rate: same metric, different code. Keep as a variant, not a separate data point."],
  ["Blood Pressure","primary","exact",L,"85354-9","Blood pressure panel with all children optional","","yes","Panel. Matches a two-component Measurement."],
  ["Blood Pressure","component","exact",L,"8480-6","Systolic blood pressure","mm[Hg]","yes",""],
  ["Blood Pressure","component","exact",L,"8462-4","Diastolic blood pressure","mm[Hg]","no",""],
  ["Blood Oxygen","primary","exact",L,"59408-5","Oxygen saturation in Arterial blood by Pulse oximetry","%","no","Pulse oximetry is the right method for a wearable or finger clip."],
  ["Blood Oxygen","variant","approximate",L,"2708-6","Oxygen saturation in Arterial blood","%","no","Generic form; use only if the source gives no method."],
  ["Respiratory Rate","primary","exact",L,"9279-1","Respiratory rate","/min","yes",""],
  ["Blood glucose","primary","candidate",L,"15074-8","Glucose [Moles/volume] in Blood (to confirm)","mmol/L","no","Code depends on specimen, method and unit. Candidates: 15074-8 blood mmol/L, 41653-7 capillary by glucometer mg/dL. Pick by what the source device reports."],
  ["Sleep duration","primary","exact",L,"93832-4","Sleep duration","h","yes",""],
  ["Steps","primary","exact",L,"55423-8","Number of steps in unspecified time Pedometer","{steps}","yes",""],
  ["Pain","primary","approximate",L,"72514-3","Pain severity - 0-10 verbal numeric rating [Score] - Reported","{score}","no","Severity only. Location is a SNOMED body-structure concept, handled when symptoms are coded."],
];

// Status for data points with no entry above, by rule. First match wins.
//   own-catalogue  modelled as our own catalogue entity; a standard would add nothing
//   per-analyte    each lab analyte gets its own LOINC code when Labs is coded (one row per analyte)
//   snomed-later   code with a curated SNOMED CT subset when that feature is built
//   din-atc-later  code with Health Canada DIN (product) + WHO ATC (class) when medication is built
//   via-catalogue  the event inherits its coding from the catalogue entry it points at (dose -> medication,
//                  appliance change -> ostomy product); nothing to code on the event itself
//   loinc-doc-later  carries a LOINC document-type code when attachments are built
//   none           no sensible standard code; local metric ID only
const CODING_DEFAULTS = [
  [r => r[1] === 'Labs',                                         'per-analyte'],
  [r => ['Ostomy Appliance Change (+ reason)','Ostomy Accessories used'].includes(r[0]), 'via-catalogue'],
  [r => ['Clinical Records','Prescription','Pictures','Note (Encounter)'].includes(r[0]), 'loinc-doc-later'],
  [r => r[0] === 'Immunisation',                                 'snomed-later'],
  [r => r[3] === 'Catalogue',                                    'own-catalogue'],
  [r => r[1] === 'Symptoms',                                     'snomed-later'],
  [r => ['Allergy','Diagnosis','Surgery','Hospitalization'].includes(r[0]), 'snomed-later'],
  [r => true,                                                    'none'],
];
module.exports = { CODINGS, CODING_DEFAULTS };
