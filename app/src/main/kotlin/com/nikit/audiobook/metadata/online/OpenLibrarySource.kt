package com.nikit.audiobook.metadata.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * OpenLibrary: https://openlibrary.org/search.json?q=...
 * Бесплатно, без ключа. Парсинг вынесен в [parse] для тестирования без сети.
 */
class OpenLibrarySource(
    private val client: OkHttpClient = OkHttpClient(),
) : OnlineMetadataSource {
    override val name: String = "OpenLibrary"

    override suspend fun search(
        title: String,
        author: String?,
    ): MetadataResult? =
        withContext(Dispatchers.IO) {
            val url =
                "https://openlibrary.org/search.json"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter(
                        "q",
                        buildString {
                            append(title)
                            if (!author.isNullOrBlank()) {
                                append(" ")
                                append(author)
                            }
                        },
                    ).build()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            parse(body)
        }

    /** Парсит JSON-ответ OpenLibrary. Тестируется без сети. */
    fun parse(json: String): MetadataResult? {
        val root = JSONObject(json)
        val docs = root.optJSONArray("docs") ?: return null
        if (docs.length() == 0) return null
        val doc = docs.getJSONObject(0)
        val title = doc.optString("title").takeIf { it.isNotBlank() } ?: return null
        val author = doc.optJSONArray("author_name")?.optString(0)?.takeIf { it.isNotBlank() }
        val description = doc.optString("text")?.takeIf { it.isNotBlank() }
        val coverId = doc.optLong("cover_i", -1)
        val coverUrl = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else null
        return MetadataResult(
            title = title,
            author = author,
            description = description,
            coverUrl = coverUrl,
            source = name,
        )
    }
}
