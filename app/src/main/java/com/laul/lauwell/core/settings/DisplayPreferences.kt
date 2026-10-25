package com.laul.lauwell.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * How the Tracker hub lays out its metrics (`docs/design/UI-spec.md` → Tracker). A display
 * preference only: it never changes which metrics are visible (that is the switched-off set).
 *
 * [key] is what is stored: never change it once shipped.
 */
enum class TrackerView(val key: String) {
    /** 15a, the default: category panels, one row per metric with a 7-day sparkline. */
    List("list"),

    /** 15b: the same categories as 2-up widgets (value, mini chart, takeaway). */
    Widgets("widgets"),
    ;

    companion object {
        val Default = List

        /** Unknown or missing values fall back to [Default], e.g. after a downgrade. */
        fun fromKey(key: String?): TrackerView = entries.firstOrNull { it.key == key } ?: Default
    }
}

/**
 * Light or dark, chosen in Settings › Appearance (design 20b). [System] follows the phone.
 *
 * [key] is what is stored: never change it once shipped.
 */
enum class ThemeMode(val key: String) {
    Light("light"),
    Dark("dark"),
    System("system"),
    ;

    companion object {
        val Default = System

        /** Unknown or missing values fall back to [Default]. */
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: Default
    }
}

/** Display preferences that follow the user across sessions. Not health data. */
interface DisplayPreferences {
    val trackerView: Flow<TrackerView>
    suspend fun setTrackerView(view: TrackerView)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

/** [DisplayPreferences] in the app's Preferences DataStore. */
class DataStoreDisplayPreferences(private val dataStore: DataStore<Preferences>) : DisplayPreferences {

    override val trackerView: Flow<TrackerView> =
        dataStore.data.map { TrackerView.fromKey(it[TrackerViewKey]) }

    override suspend fun setTrackerView(view: TrackerView) {
        dataStore.edit { it[TrackerViewKey] = view.key }
    }

    override val themeMode: Flow<ThemeMode> =
        dataStore.data.map { ThemeMode.fromKey(it[ThemeModeKey]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[ThemeModeKey] = mode.key }
    }

    private companion object {
        val TrackerViewKey = stringPreferencesKey("tracker_view")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
    }
}
