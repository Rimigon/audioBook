package com.nikit.audiobook.data.saf

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scanDataStore by preferencesDataStore(name = "scan_settings")

/** Хранит URI выбранной SAF-папки автоскана. */
class ScanSettings(
    private val context: Context,
) {
    private val key = stringPreferencesKey("tree_uri")

    val treeUri: Flow<String?> = context.scanDataStore.data.map { it[key] }

    suspend fun setTreeUri(uri: String) {
        context.scanDataStore.edit { it[key] = uri }
    }
}
