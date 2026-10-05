package com.laul.lauwell.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Single Room database for the app, shared across feature modules.
 *
 * Each feature module owns its own entities/DAOs (see e.g. feature.health.data) and registers
 * them here. Kept as one physical database (not one-per-feature) so cross-module queries and a
 * single migration story stay possible, while entities/DAOs themselves remain module-scoped.
 *
 * TODO: register real entities/DAOs as each feature module defines its data model
 * (see Claude-LauWell/PLAN.md next steps — data models still need to be designed to avoid
 * TrackAid's ModuleData god-object mistake).
 */

/**
@Database(
    entities = [],
    version = 1,
    exportSchema = true,
)
abstract class LauWellDatabase : RoomDatabase()
 */