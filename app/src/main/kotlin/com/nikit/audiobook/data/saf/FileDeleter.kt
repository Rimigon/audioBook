package com.nikit.audiobook.data.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.nikit.audiobook.data.diag.CrashLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

/** Удаление документов через SAF (tree-grant). Чистое для тестов. */
fun interface FileDeleter {
    /** @return false, если хотя бы один URI не удалось удалить (и он не отсутствовал изначально). */
    fun delete(uris: List<String>): Boolean
}

/**
 * Удаление через [DocumentsContract] напрямую — `DocumentFile.fromSingleUri` возвращает
 * `SingleDocumentFile`, который не умеет перечислять детей папки, а папка книги
 * удаляется рекурсивно. Файлы, которых уже нет на диске (карточка пережила удаление),
 * считаются удалёнными — это штатная фича, а не ошибка.
 */
@Singleton
class AndroidFileDeleter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val crashLogger: CrashLogger,
    ) : FileDeleter {
        override fun delete(uris: List<String>): Boolean {
            var ok = true
            for (u in uris) {
                ok = deleteDoc(u) && ok
            }
            return ok
        }

        private fun deleteDoc(uriStr: String): Boolean {
            if (uriStr.isBlank()) return true
            return try {
                deleteRecursive(Uri.parse(uriStr))
                true
            } catch (t: Throwable) {
                // файла/папки уже нет на диске (в т.ч. обёрнутое provider'ом в
                // IllegalArgumentException "Failed to determine if ... is child of ...") —
                // штатная ситуация, когда карточка пережила удаление файлов
                if (isMissingFile(t)) {
                    true
                } else {
                    crashLogger.write(RuntimeException("Удаление файла не удалось: $uriStr", t))
                    false
                }
            }
        }

        private fun deleteRecursive(uri: Uri) {
            if (uri.scheme != "content") return
            if (isDirectory(uri)) {
                // папка книги: сперва всё внутри, потом сама папка
                deleteChildren(uri)
            }
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        }

        private fun isDirectory(uri: Uri): Boolean =
            try {
                context.contentResolver
                    .query(uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                    ?.use { c ->
                        if (c.moveToFirst()) {
                            return c.getString(0) == DocumentsContract.Document.MIME_TYPE_DIR
                        }
                    }
                false
            } catch (_: Exception) {
                // файл отсутствует или provider не отвечает — это не каталог;
                // сам deleteDocument скажет правду
                false
            }

        private fun deleteChildren(dirUri: Uri) {
            val docId = DocumentsContract.getDocumentId(dirUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, docId)
            context.contentResolver
                .query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null,
                    null,
                    null,
                )?.use { c ->
                    val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (c.moveToNext()) {
                        val child = DocumentsContract.buildDocumentUriUsingTree(dirUri, c.getString(idIdx))
                        try {
                            if (c.getString(mimeIdx) == DocumentsContract.Document.MIME_TYPE_DIR) {
                                deleteChildren(child)
                            }
                            DocumentsContract.deleteDocument(context.contentResolver, child)
                        } catch (t: Throwable) {
                            // пропускаем уже отсутствующих детей, остальные — пробрасываем
                            if (!isMissingFile(t)) throw t
                        }
                    }
                }
        }

        /** true, если исключение (или его причина) означает «файла больше нет». */
        private fun isMissingFile(t: Throwable): Boolean {
            var cur: Throwable? = t
            while (cur != null) {
                if (cur is FileNotFoundException) return true
                when {
                    cur.message?.contains("Missing file", ignoreCase = true) == true -> return true
                    cur.message?.contains("not found", ignoreCase = true) == true -> return true
                    cur.message?.contains("no such file", ignoreCase = true) == true -> return true
                }
                cur = cur.cause
            }
            return false
        }
    }
