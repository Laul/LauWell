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
// Codes live on the metric definition, never on individual records.
const L = 'LOINC';
const CODINGS = [
  ["Heart rate","primary","exact",L,"8867-4","Heart rate","/min","yes","Also the FHIR vital-signs code."],
  ["Heart rate","variant","exact",L,"40443-4","Heart rate --resting","/min","yes","Resting heart rate: same metric, different code. Keep as a variant, not a separate data point."],
  ["Heart rate variability","primary","approximate",L,"80404-7","R-R interval.standard deviation (Heart rate variability)","ms","no","Wearables report SDNN or RMSSD and the two are not interchangeable: record which one in the metric definition."],
  ["Blood pressure","primary","exact",L,"85354-9","Blood pressure panel with all children optional","","yes","Panel. Matches a two-component Measurement."],
  ["Blood pressure","component","exact",L,"8480-6","Systolic blood pressure","mm[Hg]","yes",""],
  ["Blood pressure","component","exact",L,"8462-4","Diastolic blood pressure","mm[Hg]","no",""],
  ["Blood oxygen","primary","exact",L,"59408-5","Oxygen saturation in Arterial blood by Pulse oximetry","%","no","Pulse oximetry is the right method for a wearable or finger clip."],
  ["Blood oxygen","variant","approximate",L,"2708-6","Oxygen saturation in Arterial blood","%","no","Generic form; use only if the source gives no method."],
  ["Respiratory rate","primary","exact",L,"9279-1","Respiratory rate","/min","yes",""],
  ["Peak flow / spirometry","primary","candidate",L,"33452-4","Peak expiratory flow rate (to confirm)","L/min","no","This row covers two measures. Split into peak flow and FEV1 (candidate 20150-9, L) when coding for real."],
  ["Blood glucose","primary","candidate",L,"15074-8","Glucose [Moles/volume] in Blood (to confirm)","mmol/L","no","Code depends on specimen, method and unit. Candidates: 15074-8 blood mmol/L, 41653-7 capillary by glucometer mg/dL. Pick by what the source device reports."],
  ["Ketones","primary","candidate",L,"2514-8","Ketones in Urine by Test strip (to confirm)","","no","Urine strip only. Blood ketone (beta-hydroxybutyrate) needs its own code, found at verification."],
  ["Weight","primary","exact",L,"29463-7","Body weight","kg","no","Also the FHIR vital-signs code."],
  ["Body composition","primary","approximate",L,"41982-0","Percentage of body fat Measured","%","yes","Only body fat has a clean code. Muscle, water and bone mass should become their own metrics before they are coded."],
  ["Body temperature","primary","exact",L,"8310-5","Body temperature","Cel","no","Skin-temperature deviation from baseline is a different metric and needs its own entry."],
  ["Height","primary","exact",L,"8302-2","Body height","cm","yes",""],
  ["Sleep duration","primary","exact",L,"93832-4","Sleep duration","h","yes",""],
  ["Steps","primary","exact",L,"55423-8","Number of steps in unspecified time Pedometer","{steps}","yes",""],
  ["Pain","primary","approximate",L,"72514-3","Pain severity - 0-10 verbal numeric rating [Score] - Reported","{score}","no","Severity only. Location is a SNOMED body-structure concept, handled when symptoms are coded."],
  ["Blood type","primary","candidate",L,"883-9","ABO group [Type] in Blood (to confirm)","","no","Two metrics: ABO group and Rh (candidate 10331-7)."],
];

// Status for data points with no entry above, by rule. First match wins.
//   own-catalogue  modelled as our own catalogue entity; a standard would add nothing
//   per-analyte    the record itself carries a LOINC code (lab results)
//   snomed-later   code with a curated SNOMED CT subset when that feature is built
//   din-atc-later  code with Health Canada DIN (product) + WHO ATC (class) when medication is built
//   via-catalogue  the event inherits its coding from the catalogue entry it points at (dose -> medication,
//                  appliance change -> ostomy product); nothing to code on the event itself
//   loinc-doc-later  carries a LOINC document-type code when attachments are built
//   none           no sensible standard code; local metric ID only
const CODING_DEFAULTS = [
  [r => r[0] === 'Lab result',                                   'per-analyte'],
  [r => ['Dose scheduled','Dose taken or skipped','As-needed dose','Appliance change','Accessory used'].includes(r[0]), 'via-catalogue'],
  [r => ['Clinical document','Imaging study'].includes(r[0]),    'loinc-doc-later'],
  [r => r[0] === 'Immunisation',                                 'snomed-later'],
  [r => ['Medication','Supplement'].includes(r[0]),              'din-atc-later'],
  [r => r[3] === 'Catalogue',                                    'own-catalogue'],
  [r => r[1] === 'Symptoms',                                     'snomed-later'],
  [r => ['Conditions','Sensitivities','Procedures'].includes(r[2]), 'snomed-later'],
  [r => r[0] === 'Side effect',                                  'snomed-later'],
  [r => true,                                                    'none'],
];
module.exports = { CODINGS, CODING_DEFAULTS };
