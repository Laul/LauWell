package com.laul.lauwell.core.record

import java.time.Instant

/**
 * Where a record comes from and when it entered the system (PLAN.md → M2 → Provenance).
 *
 * Two apps reporting the same metric is settled by Health Connect's app priority, and we keep each
 * record's [origin] so the screen can name it. Manual vs device for one metric is decided with
 * manual entries (PLAN.md → Decisions → *Read first*).
 *
 * @param recordedAt when the record was written to its store (Health Connect's last-modified time
 *   for synced data), not when the measured thing happened.
 * @param editedAt last change after creation; null when never edited.
 */
data class Provenance(
    val source: Source,
    val origin: DataOrigin? = null,
    val recordingMethod: RecordingMethod = RecordingMethod.Unknown,
    val recordedAt: Instant,
    val editedAt: Instant? = null,
) {
    init {
        require(source != Source.Device || origin != null) { "A device record needs its origin" }
        require(editedAt == null || !editedAt.isBefore(recordedAt)) { "editedAt is before recordedAt" }
    }
}

/** [key] is what is stored: never change it once shipped. */
enum class Source(val key: String) {
    /** A sensor, through Health Connect. */
    Device("device"),

    /** A laboratory report. */
    Lab("lab"),

    /** Logged by hand in this app. */
    Manual("manual"),

    /** Imported from a file or another system. */
    Import("import"),
}

/**
 * The app that wrote a record to Health Connect (e.g. `com.garmin.android.apps.connectmobile`,
 * `com.sec.android.app.shealth`) and, when it says, the device that measured it.
 */
data class DataOrigin(val appPackage: String, val device: SourceDevice? = null) {
    init {
        require(appPackage.isNotBlank()) { "appPackage must not be blank" }
    }
}

/** Health Connect's device description; any part may be missing. */
data class SourceDevice(val manufacturer: String?, val model: String?, val type: DeviceType = DeviceType.Unknown)

/** Health Connect's `Device.TYPE_*` values. */
enum class DeviceType { Watch, Phone, Scale, Ring, HeadMounted, FitnessBand, ChestStrap, SmartDisplay, Unknown }

/**
 * How the writing app captured it, as Health Connect reports it. A value typed into Samsung Health
 * arrives as [Manual] with source [Source.Device]: it still came through Health Connect.
 */
enum class RecordingMethod {
    /** Passively, e.g. all-day heart rate. */
    Automatic,

    /** During a session the user started, e.g. a workout. */
    Active,

    /** Typed in by the user in the writing app. */
    Manual,

    Unknown,
}
