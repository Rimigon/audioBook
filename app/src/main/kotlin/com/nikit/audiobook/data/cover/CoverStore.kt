package com.nikit.audiobook.data.cover

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Сохраняет обложки во внутреннее хранилище: из байтов тега или по онлайн URL. */
@Singleton
class CoverStore
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        private val client: OkHttpClient,
    ) {
        private val dir: File by lazy { File(context.filesDir, "covers").apply { mkdirs() } }

        /** Сохраняет байты обложки из тега. Возвращает абсолютный путь файла или null. */
        fun saveBytes(
            bytes: ByteArray,
            key: String,
        ): String? =
            runCatching {
                val name = "${hash(key)}.jpg"
                val file = File(dir, name)
                if (!file.exists()) file.writeBytes(bytes)
                file.absolutePath
            }.getOrNull()

        /** Скачивает обложку по URL. Возвращает абсолютный путь файла или null. */
        suspend fun download(
            url: String,
            key: String,
        ): String? =
            runCatching {
                val name = "${hash(key)}.jpg"
                val file = File(dir, name)
                if (file.exists()) return@runCatching file.absolutePath
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val resp = client.newCall(Request.Builder().url(url).build()).execute()
                    if (!resp.isSuccessful) return@withContext null
                    resp.body?.byteStream()?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    }
                    file.absolutePath
                }
            }.getOrNull()

        private fun hash(key: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(key.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(24)
    }
