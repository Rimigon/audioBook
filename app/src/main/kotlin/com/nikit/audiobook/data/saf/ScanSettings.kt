package com.nikit.audiobook.data.saf

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nikit.audiobook.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_settings")

/** Общие настройки: папка автоскана + тема. */
class ScanSettings(
    private val context: Context,
) {
    private val treeKey = stringPreferencesKey("tree_uri")
    private val themeKey = stringPreferencesKey("theme_mode")

    val treeUri: Flow<String?> = context.appDataStore.data.map { it[treeKey] }
    val themeMode: Flow<ThemeMode> =
        context.appDataStore.data.map {
            when (it[themeKey]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setTreeUri(uri: String) = context.appDataStore.edit { it[treeKey] = uri }

    suspend fun setThemeMode(mode: ThemeMode) = context.appDataStore.edit { it[themeKey] = mode.name }
}
