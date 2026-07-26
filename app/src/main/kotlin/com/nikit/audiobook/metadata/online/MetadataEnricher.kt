package com.nikit.audiobook.metadata.online

import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import com.nikit.audiobook.data.repo.toEntity
import com.nikit.audiobook.domain.model.MetadataCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

/**
 * Оркестратор обогащения: если теги полны — ничего не делает;
 * иначе идёт в кэш [MetadataCacheDao], при промахе — опрашивает источники по очереди,
 * первый непустой результат кэширует.
 *
 * Кэш хранит JSON-представление [MetadataResult] в поле payloadJson.
 */
class MetadataEnricher(
    private val cache: MetadataCacheDao,
    private val sources: List<OnlineMetadataSource>,
) {
    private val mutex = Mutex()

    suspend fun enrich(
        title: String,
        author: String?,
    ): MetadataResult? =
        mutex.withLock {
            if (title.isBlank()) return@withLock null
            val queryKey = "${TitleNormalizer.normalize(title)}|${author?.lowercase()?.trim().orEmpty()}"
            cache.findByQueryKey(queryKey)?.let { entry ->
                return@withLock parsePayload(entry.payloadJson)
            }
            for (source in sources) {
                val result = source.search(TitleNormalizer.normalize(title), author) ?: continue
                cache.upsert(
                    MetadataCache(
                        id = UUID.randomUUID().toString(),
                        queryKey = queryKey,
                        payloadJson = encodePayload(result),
                        source = result.source,
                    ).toEntity(),
                )
                return@withLock result
            }
            // кэшируем «нет результата», чтобы не дёргать источники повторно
            cache.upsert(
                MetadataCacheEntity(
                    id = UUID.randomUUID().toString(),
                    queryKey = queryKey,
                    payloadJson = "{}",
                    source = "none",
                    createdAt = System.currentTimeMillis(),
                ),
            )
            null
        }

    private fun encodePayload(r: MetadataResult): String {
        val o = JSONObject()
        o.put("title", r.title)
        o.put("author", r.author ?: "")
        o.put("description", r.description ?: "")
        o.put("coverUrl", r.coverUrl ?: "")
        o.put("source", r.source)
        return o.toString()
    }

    private fun parsePayload(json: String): MetadataResult? {
        if (json == "{}") return null
        val o = JSONObject(json)
        val title = o.optString("title").takeIf { it.isNotBlank() } ?: return null
        return MetadataResult(
            title = title,
            author = o.optString("author").takeIf { it.isNotBlank() },
            description = o.optString("description").takeIf { it.isNotBlank() },
            coverUrl = o.optString("coverUrl").takeIf { it.isNotBlank() },
            source = o.optString("source"),
        )
    }
}
