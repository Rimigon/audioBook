package com.nikit.audiobook.metadata.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Google Books: https://www.googleapis.com/books/v1/volumes?q=...
 * Без ключа есть лимиты. Парсинг вынесен в [parse].
 */
class GoogleBooksSource(
    private val client: OkHttpClient = OkHttpClient(),
    private val apiKey: String? = null,
) : OnlineMetadataSource {
    override val name: String = "GoogleBooks"

    override suspend fun search(
        title: String,
        author: String?,
    ): MetadataResult? =
        withContext(Dispatchers.IO) {
            val builder =
                "https://www.googleapis.com/books/v1/volumes"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter(
                        "q",
                        buildString {
                            append("intitle:$title")
                            if (!author.isNullOrBlank()) append(" inauthor:$author")
                        },
                    )
            if (!apiKey.isNullOrBlank()) builder.addQueryParameter("key", apiKey)
            val response = client.newCall(Request.Builder().url(builder.build()).build()).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            parse(body)
        }

    /** Парсит JSON-ответ Google Books. Тестируется без сети. */
    fun parse(json: String): MetadataResult? {
        val root = JSONObject(json)
        val items = root.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val info = items.getJSONObject(0).optJSONObject("volumeInfo") ?: return null
        val title = info.optString("title").takeIf { it.isNotBlank() } ?: return null
        val authors = info.optJSONArray("authors")?.optString(0)?.takeIf { it.isNotBlank() }
        val description = info.optString("description")?.takeIf { it.isNotBlank() }
        val coverUrl =
            info
                .optJSONObject("imageLinks")
                ?.optString("thumbnail")
                ?.takeIf { it.isNotBlank() }
        return MetadataResult(
            title = title,
            author = authors,
            description = description,
            coverUrl = coverUrl,
            source = name,
        )
    }
}
