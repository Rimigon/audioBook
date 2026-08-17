package com.nikit.audiobook.data.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Удаление документов через SAF (tree-grant). Чистое для тестов. */
fun interface FileDeleter {
    /** @return false, если хотя бы один URI не удалось удалить (и он не отсутствовал изначально). */
    fun delete(uris: List<String>): Boolean
}

@Singleton
class AndroidFileDeleter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FileDeleter {
        override fun delete(uris: List<String>): Boolean = uris.all(::deleteDoc)

        private fun deleteDoc(uriStr: String): Boolean {
            if (uriStr.isBlank()) return true
            return try {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(uriStr))
                // уже отсутствует — считаем удалённым
                if (doc == null || !doc.exists()) true else doc.delete()
            } catch (_: Exception) {
                false
            }
        }
    }
