package com.nikit.audiobook.metadata.tags

import android.content.Context
import android.media.MediaMetadataRetriever
import com.nikit.audiobook.domain.model.BookMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Читает теги аудиофайла через [MediaMetadataRetriever]. */
interface TagReader {
    fun read(uri: String): BookMetadata
}

@Singleton
class AndroidTagReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TagReader {
        override fun read(uri: String): BookMetadata {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, android.net.Uri.parse(uri))
                val durationMs =
                    retriever
                        .runCatching {
                            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        }.getOrDefault(0L)
                val year =
                    retriever
                        .runCatching {
                            extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
                        }.getOrNull()
                BookMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    author =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                    year = year,
                    description = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR),
                    coverBytes = retriever.embeddedPicture,
                    durationMs = durationMs,
                )
            } catch (e: Exception) {
                BookMetadata()
            } finally {
                retriever.runCatching { release() }
            }
        }
    }
