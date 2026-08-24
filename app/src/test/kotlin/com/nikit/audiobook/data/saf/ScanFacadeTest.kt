package com.nikit.audiobook.data.saf

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import com.nikit.audiobook.data.repo.BookRepository
import com.nikit.audiobook.domain.model.AudioFileRef
import com.nikit.audiobook.domain.model.BookDescriptor
import com.nikit.audiobook.domain.model.BookMetadata
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind
import com.nikit.audiobook.metadata.online.MetadataEnricher
import com.nikit.audiobook.metadata.online.MetadataResult
import com.nikit.audiobook.metadata.online.OnlineMetadataSource
import com.nikit.audiobook.metadata.tags.TagReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeTagReader(
    private val durationMs: Long = 1000L,
) : TagReader {
    override fun read(uri: String): BookMetadata = BookMetadata(title = "T", author = "Author", durationMs = durationMs)
}

private class NullSource : OnlineMetadataSource {
    override val name = "null"

    override suspend fun search(
        title: String,
        author: String?,
    ): MetadataResult? = null
}

private class FakeCacheDao : MetadataCacheDao {
    val map = mutableMapOf<String, MetadataCacheEntity>()

    override suspend fun upsert(entry: MetadataCacheEntity) {
        map[entry.queryKey] = entry
    }

    override suspend fun findByQueryKey(queryKey: String): MetadataCacheEntity? = map[queryKey]

    override suspend fun deleteById(id: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
class ScanFacadeTest {
    private lateinit var db: AudioBookDatabase
    private lateinit var repo: BookRepository
    private lateinit var facade: ScanFacade

    @Before fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AudioBookDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repo = BookRepository(db.bookDao(), db.chapterDao())
        val enricher = MetadataEnricher(FakeCacheDao(), listOf(NullSource()))
        // FolderScanner не используется в importDescriptors — передаём заглушку через недоступный конструктор?
        // Создаём фасад через реальный FolderScanner (Context) — он не вызывается в этом тесте.
        facade =
            ScanFacade(
                repo,
                FolderScanner(ApplicationProvider.getApplicationContext()),
                FakeTagReader(),
                enricher,
                com.nikit.audiobook.data.cover
                    .CoverStore(ApplicationProvider.getApplicationContext(), okhttp3.OkHttpClient()),
            )
    }

    @After fun teardown() {
        db.close()
    }

    private fun folder(
        name: String,
        vararg files: String,
    ) = BookDescriptor(
        title = name,
        type = FileType.FOLDER,
        files = files.map { AudioFileRef("uri://$it", it) },
        sourceUri = "uri://dir/$name",
        sourceKind = SourceKind.LOCAL_FOLDER,
    )

    @Test fun importFolderCreatesBookWithChapters() =
        runTest {
            val added = facade.importDescriptors(listOf(folder("Дюна", "01.mp3", "02.mp3")))
            assertThat(added.added).isEqualTo(1)
            val books = repo.observeAll().first()
            assertThat(books).hasSize(1)
            assertThat(books.single().title).isEqualTo("Дюна")
            assertThat(books.single().fileType).isEqualTo(FileType.FOLDER)
            assertThat(books.single().totalDurationMs).isEqualTo(2000L)
        }

    @Test fun dedupBySourceUri() =
        runTest {
            val d = folder("Дюна", "01.mp3")
            facade.importDescriptors(listOf(d))
            val added2 = facade.importDescriptors(listOf(d))
            assertThat(added2.added).isEqualTo(0)
            assertThat(added2.skipped).isEqualTo(1)
            assertThat(repo.observeAll().first()).hasSize(1)
        }

    @Test fun singleFileCreatesOneChapter() =
        runTest {
            val d =
                BookDescriptor(
                    title = "lecture",
                    type = FileType.M4B,
                    files = listOf(AudioFileRef("uri://lecture.m4b", "lecture.m4b")),
                    sourceUri = "uri://lecture.m4b",
                    sourceKind = SourceKind.LOCAL_FILE,
                )
            facade.importDescriptors(listOf(d))
            val book =
                repo.observeAll().first().single()
            assertThat(book.fileType).isEqualTo(FileType.M4B)
            assertThat(book.totalDurationMs).isEqualTo(1000L)
        }
}
