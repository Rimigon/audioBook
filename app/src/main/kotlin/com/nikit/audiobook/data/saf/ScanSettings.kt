package com.nikit.audiobook.data.saf

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nikit.audiobook.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_settings")

/** Общие настройки: папка автоскана + тема + флаги инициализации. */
class ScanSettings(
    private val context: Context,
) {
    private val treeKey = stringPreferencesKey("tree_uri")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val shelvesSeededKey = booleanPreferencesKey("shelves_seeded")
    private val seekStepKey = stringPreferencesKey("seek_step")
    private val autoResumeKey = booleanPreferencesKey("auto_resume")
    private val onlineKey = booleanPreferencesKey("online_enrich")
    private val gbKey = stringPreferencesKey("google_books_key")
    private val rescanIntervalKey = stringPreferencesKey("rescan_interval_min")
    private val volumeBoostKey = stringPreferencesKey("volume_boost")
    private val playbackSpeedKey = stringPreferencesKey("playback_speed")
    private val equalizerPresetKey = stringPreferencesKey("equalizer_preset")
    private val dismissedContinueKey = stringSetPreferencesKey("dismissed_continue")
    private val lastCategoryKey = stringPreferencesKey("last_category")

    val treeUri: Flow<String?> = context.appDataStore.data.map { it[treeKey] }
    val themeMode: Flow<ThemeMode> =
        context.appDataStore.data.map {
            when (it[themeKey]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
    val shelvesSeeded: Flow<Boolean> = context.appDataStore.data.map { it[shelvesSeededKey] ?: false }
    val seekStepMs: Flow<Long> = context.appDataStore.data.map { (it[seekStepKey]?.toLongOrNull() ?: 30_000L) }
    val autoResume: Flow<Boolean> = context.appDataStore.data.map { it[autoResumeKey] ?: true }
    val onlineEnrich: Flow<Boolean> = context.appDataStore.data.map { it[onlineKey] ?: true }
    val googleBooksKey: Flow<String?> = context.appDataStore.data.map { it[gbKey] }
    val rescanIntervalMin: Flow<Int> = context.appDataStore.data.map { (it[rescanIntervalKey]?.toIntOrNull() ?: 60) }
    val volumeBoost: Flow<Float> = context.appDataStore.data.map { (it[volumeBoostKey]?.toFloatOrNull() ?: 1f) }
    val playbackSpeed: Flow<Float> = context.appDataStore.data.map { (it[playbackSpeedKey]?.toFloatOrNull() ?: 1f) }
    val equalizerPreset: Flow<String?> = context.appDataStore.data.map { it[equalizerPresetKey] }
    val dismissedContinue: Flow<Set<String>> = context.appDataStore.data.map { it[dismissedContinueKey] ?: emptySet() }
    val lastCategory: Flow<String?> = context.appDataStore.data.map { it[lastCategoryKey] }

    suspend fun setTreeUri(uri: String) = context.appDataStore.edit { it[treeKey] = uri }

    suspend fun markShelvesSeeded() = context.appDataStore.edit { it[shelvesSeededKey] = true }

    suspend fun setThemeMode(mode: ThemeMode) = context.appDataStore.edit { it[themeKey] = mode.name }

    suspend fun setSeekStep(ms: Long) = context.appDataStore.edit { it[seekStepKey] = ms.toString() }

    suspend fun setAutoResume(v: Boolean) = context.appDataStore.edit { it[autoResumeKey] = v }

    suspend fun setOnlineEnrich(v: Boolean) = context.appDataStore.edit { it[onlineKey] = v }

    suspend fun setGoogleBooksKey(v: String) = context.appDataStore.edit { it[gbKey] = v }

    suspend fun setRescanInterval(min: Int) = context.appDataStore.edit { it[rescanIntervalKey] = min.toString() }

    suspend fun setVolumeBoost(v: Float) = context.appDataStore.edit { it[volumeBoostKey] = v.toString() }

    suspend fun setPlaybackSpeed(v: Float) = context.appDataStore.edit { it[playbackSpeedKey] = v.toString() }

    suspend fun setEqualizerPreset(preset: String?) =
        context.appDataStore.edit {
            if (preset == null) it.remove(equalizerPresetKey) else it[equalizerPresetKey] = preset
        }

    /** Книга скрыта из ряда «Продолжить» в Библиотеке (прогресс не трогаем). */
    suspend fun dismissFromContinue(bookId: String) =
        context.appDataStore.edit { prefs ->
            prefs[dismissedContinueKey] = (prefs[dismissedContinueKey] ?: emptySet()) + bookId
        }

    /** Запомнить последнюю выбранную категорию Библиотеки (null = «Все»). */
    suspend fun setLastCategory(statusName: String?) =
        context.appDataStore.edit { prefs ->
            if (statusName == null) prefs.remove(lastCategoryKey) else prefs[lastCategoryKey] = statusName
        }
}
