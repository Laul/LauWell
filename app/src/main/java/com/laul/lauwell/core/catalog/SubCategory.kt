package com.laul.lauwell.core.catalog

import com.laul.lauwell.core.catalog.Category.Lifestyle
import com.laul.lauwell.core.catalog.Category.MedicalRecords
import com.laul.lauwell.core.catalog.Category.Symptoms
import com.laul.lauwell.core.catalog.Category.TreatmentsAndAppliances
import com.laul.lauwell.core.catalog.Category.Vitals

/**
 * The 35 sub-categories: the second level of the data hierarchy. Mirrors
 * `docs/data-structure/Patient Data Categories.csv`; `CatalogMatchesDocsTest` fails if they drift.
 *
 * A label is only unique within its category ("Respiratory" exists under both Vitals and
 * Symptoms), so identity is the [category] plus the [key]. [storageKey] combines both and is
 * stable: it ends up in stored settings and must never change once shipped.
 */
enum class SubCategory(val category: Category, val key: String, val label: String) {
    // Vitals
    VitalsCardiovascular(Vitals, "cardiovascular", "Cardiovascular"),
    VitalsRespiratory(Vitals, "respiratory", "Respiratory"),
    VitalsMetabolic(Vitals, "metabolic", "Metabolic"),
    VitalsBodyMeasures(Vitals, "body_measures", "Body measures"),
    VitalsLaboratory(Vitals, "laboratory", "Laboratory"),
    VitalsDigestiveOutput(Vitals, "digestive_output", "Digestive output"),

    // Medical Records
    RecordsConditions(MedicalRecords, "conditions", "Conditions"),
    RecordsSensitivities(MedicalRecords, "sensitivities", "Sensitivities"),
    RecordsProcedures(MedicalRecords, "procedures", "Procedures"),
    RecordsPreventiveCare(MedicalRecords, "preventive_care", "Preventive care"),
    RecordsCareAndVisits(MedicalRecords, "care_visits", "Care & visits"),
    RecordsDocuments(MedicalRecords, "documents", "Documents"),
    RecordsIdentity(MedicalRecords, "identity", "Identity"),
    RecordsAdministration(MedicalRecords, "administration", "Administration"),

    // Lifestyle
    LifestyleSleep(Lifestyle, "sleep", "Sleep"),
    LifestyleActivity(Lifestyle, "activity", "Activity"),
    LifestyleNutritionAndIntake(Lifestyle, "nutrition_intake", "Nutrition & intake"),
    LifestyleMindAndMood(Lifestyle, "mind_mood", "Mind & mood"),
    LifestyleCycle(Lifestyle, "cycle", "Cycle"),
    LifestyleOstomyRoutine(Lifestyle, "ostomy_routine", "Ostomy routine"),
    LifestyleContext(Lifestyle, "context", "Context"),

    // Symptoms
    SymptomsGeneral(Symptoms, "general", "General"),
    SymptomsSystemic(Symptoms, "systemic", "Systemic"),
    SymptomsCardiac(Symptoms, "cardiac", "Cardiac"),
    SymptomsRespiratory(Symptoms, "respiratory", "Respiratory"),
    SymptomsNeurological(Symptoms, "neurological", "Neurological"),
    SymptomsDigestive(Symptoms, "digestive", "Digestive"),
    SymptomsPain(Symptoms, "pain", "Pain"),
    SymptomsSkinAndStoma(Symptoms, "skin_stoma", "Skin & stoma"),

    // Treatments & appliances
    TreatmentsMedication(TreatmentsAndAppliances, "medication", "Medication"),
    TreatmentsTherapies(TreatmentsAndAppliances, "therapies", "Therapies"),
    TreatmentsOstomy(TreatmentsAndAppliances, "ostomy", "Ostomy"),
    TreatmentsDevices(TreatmentsAndAppliances, "devices", "Devices"),
    TreatmentsSupplies(TreatmentsAndAppliances, "supplies", "Supplies"),
    TreatmentsExperiments(TreatmentsAndAppliances, "experiments", "Experiments"),
    ;

    val storageKey: String get() = "${category.key}/$key"

    companion object {
        fun of(category: Category): List<SubCategory> = entries.filter { it.category == category }
    }
}
