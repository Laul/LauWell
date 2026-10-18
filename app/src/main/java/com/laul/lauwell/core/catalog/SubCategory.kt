package com.laul.lauwell.core.catalog

import com.laul.lauwell.core.catalog.Category.Labs
import com.laul.lauwell.core.catalog.Category.Lifestyle
import com.laul.lauwell.core.catalog.Category.MedicalRecords
import com.laul.lauwell.core.catalog.Category.Ostomy
import com.laul.lauwell.core.catalog.Category.Symptoms
import com.laul.lauwell.core.catalog.Category.TreatmentsAndAppliances
import com.laul.lauwell.core.catalog.Category.Vitals

/**
 * The 35 sub-categories: the second level of the data hierarchy. Mirrors
 * `docs/data-structure/health-data-model/Patient Data Categories.csv` (labels as written in the Notion table);
 * `CatalogMatchesDocsTest` fails if they drift.
 *
 * A label is only unique within its category ("Cardiovascular" exists under Vitals, Labs and
 * Symptoms), so identity is the [category] plus the [key]. [storageKey] combines both and is
 * stable: it ends up in stored settings and must never change once shipped.
 */
enum class SubCategory(val category: Category, val key: String, val label: String) {
    // Vitals
    VitalsBiochemistryMetabolic(Vitals, "biochemistry_metabolic", "Biochemistry - Metabolic"),
    VitalsCardiovascular(Vitals, "cardiovascular", "Cardiovascular"),
    VitalsRespiratory(Vitals, "respiratory", "Respiratory"),
    VitalsWomanHealth(Vitals, "woman_health", "Woman Health"),

    // Labs
    LabsBiochemistryElectrolytes(Labs, "biochemistry_electrolytes", "Biochemistry - Electrolytes"),
    LabsBiochemistryEnzymes(Labs, "biochemistry_enzymes", "Biochemistry - Enzymes"),
    LabsBiochemistryLiver(Labs, "biochemistry_liver", "Biochemistry - Liver"),
    LabsBiochemistryMetabolic(Labs, "biochemistry_metabolic", "Biochemistry - Metabolic"),
    LabsBiochemistryProtein(Labs, "biochemistry_protein", "Biochemistry - Protein"),
    LabsBiochemistryRenal(Labs, "biochemistry_renal", "Biochemistry - Renal"),
    LabsCardiovascular(Labs, "cardiovascular", "Cardiovascular"),
    LabsCoagulation(Labs, "coagulation", "Coagulation"),
    LabsEndocrinology(Labs, "endocrinology", "Endocrinology"),
    LabsHematology(Labs, "hematology", "Hematology"),
    LabsInflammation(Labs, "inflammation", "Inflammation"),
    LabsIronStudies(Labs, "iron_studies", "Iron studies"),
    LabsLipidProfile(Labs, "lipid_profile", "Lipid profile"),
    LabsUrinalysis(Labs, "urinalysis", "Urinalysis"),
    LabsVitamins(Labs, "vitamins", "Vitamins"),

    // Medical Records
    RecordsAppointment(MedicalRecords, "appointment", "Appointment"),
    RecordsDocuments(MedicalRecords, "documents", "Documents"),
    RecordsMedicalHistory(MedicalRecords, "medical_history", "Medical History"),
    RecordsPictures(MedicalRecords, "pictures", "Pictures"),

    // Lifestyle
    LifestyleActivity(Lifestyle, "activity", "Activity"),
    LifestyleMindAndMood(Lifestyle, "mind_and_mood", "Mind and mood"),
    LifestyleSleep(Lifestyle, "sleep", "Sleep"),

    // Symptoms
    SymptomsCardiovascular(Symptoms, "cardiovascular", "Cardiovascular"),
    SymptomsDigestive(Symptoms, "digestive", "Digestive"),
    SymptomsGeneral(Symptoms, "general", "General"),
    SymptomsNeurological(Symptoms, "neurological", "Neurological"),
    SymptomsPain(Symptoms, "pain", "Pain"),
    SymptomsSystemic(Symptoms, "systemic", "Systemic"),

    // Treatments & Appliances
    TreatmentsAppointment(TreatmentsAndAppliances, "appointment", "Appointment"),

    // Ostomy
    OstomyDailyRoutines(Ostomy, "daily_routines", "Ostomy • Daily Routines"),
    OstomyReplacement(Ostomy, "replacement", "Ostomy • Replacement"),
    ;

    val storageKey: String get() = "${category.key}/$key"

    companion object {
        fun of(category: Category): List<SubCategory> = entries.filter { it.category == category }
    }
}
