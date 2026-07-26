package com.nikit.audiobook.metadata.online

import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeCacheDao : MetadataCacheDao {
    val map = mutableMapOf<String, MetadataCacheEntity>()
    var upserts = 0

    override suspend fun upsert(entry: MetadataCacheEntity) {
        map[entry.queryKey] = entry
        upserts++
    }

    override suspend fun findByQueryKey(queryKey: String): MetadataCacheEntity? = map[queryKey]

    override suspend fun deleteById(id: String) = Unit
}

private class FakeSource(
    val result: MetadataResult?,
) : OnlineMetadataSource {
    override val name = "fake"
    var calls = 0

    override suspend fun search(
        title: String,
        author: String?,
    ): MetadataResult? {
        calls++
        return result
    }
}

@RunWith(RobolectricTestRunner::class)
class MetadataEnricherTest {
    @Test fun cacheMiss_callsSource_andCaches() =
        runTest {
            val cache = FakeCacheDao()
            val source = FakeSource(MetadataResult("Dune", "Herbert", "desc", "cover", "fake"))
            val enricher = MetadataEnricher(cache, listOf(source))
            val r1 = enricher.enrich("Дюна", null)
            assertThat(r1?.title).isEqualTo("Dune")
            assertThat(source.calls).isEqualTo(1)
            // второй раз — из кэша, source не дёргается
            val r2 = enricher.enrich("Дюна", null)
            assertThat(r2?.title).isEqualTo("Dune")
            assertThat(source.calls).isEqualTo(1)
        }

    @Test fun cacheHit_doesNotCallSource() =
        runTest {
            val cache = FakeCacheDao()
            val source = FakeSource(MetadataResult("X", null, null, null, "fake"))
            val enricher = MetadataEnricher(cache, listOf(source))
            // первый прогон кэширует
            enricher.enrich("title", null)
            source.calls = 0
            enricher.enrich("title", null)
            assertThat(source.calls).isEqualTo(0)
        }

    @Test fun noResult_isCachedAsEmpty_andNotRequeried() =
        runTest {
            val cache = FakeCacheDao()
            val source = FakeSource(null)
            val enricher = MetadataEnricher(cache, listOf(source))
            val r1 = enricher.enrich("unknown", null)
            assertThat(r1).isNull()
            val r2 = enricher.enrich("unknown", null)
            assertThat(r2).isNull()
            // source позвали только один раз; второй раз — кэш «нет результата»
            assertThat(source.calls).isEqualTo(1)
        }

    @Test fun blankTitle_returnsNull() =
        runTest {
            val cache = FakeCacheDao()
            val source = FakeSource(MetadataResult("X", null, null, null, "fake"))
            val enricher = MetadataEnricher(cache, listOf(source))
            assertThat(enricher.enrich("  ", null)).isNull()
            assertThat(source.calls).isEqualTo(0)
        }
}
