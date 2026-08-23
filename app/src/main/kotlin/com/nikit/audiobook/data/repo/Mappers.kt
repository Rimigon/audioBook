package com.nikit.audiobook.data.repo

import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
import com.nikit.audiobook.data.db.entity.TagEntity
import com.nikit.audiobook.domain.model.Book
import com.nikit.audiobook.domain.model.Bookmark
import com.nikit.audiobook.domain.model.Chapter
import com.nikit.audiobook.domain.model.MetadataCache
import com.nikit.audiobook.domain.model.PlaybackProgress
import com.nikit.audiobook.domain.model.Tag

fun BookEntity.toDomain() =
    Book(
        id,
        title,
        author,
        series,
        seriesIndex,
        genre,
        description,
        year,
        coverPath,
        sourceUri,
        filesPresent,
        fileType,
        totalDurationMs,
        addedAt,
        lastPlayedAt,
        completedAt,
        rating,
        review,
        status,
        sourceKind,
        originalPath,
        manuallyEdited,
    )

fun Book.toEntity() =
    BookEntity(
        id,
        title,
        author,
        series,
        seriesIndex,
        genre,
        description,
        year,
        coverPath,
        sourceUri,
        filesPresent,
        fileType,
        totalDurationMs,
        addedAt,
        lastPlayedAt,
        completedAt,
        rating,
        review,
        status,
        sourceKind,
        originalPath,
        manuallyEdited,
    )

fun ChapterEntity.toDomain() = Chapter(id, bookId, index, title, startMs, endMs, filePath)

fun Chapter.toEntity() = ChapterEntity(id, bookId, index, title, startMs, endMs, filePath)

fun BookmarkEntity.toDomain() = Bookmark(id, bookId, positionMs, title, note, kind, chapterIndex, createdAt)

fun Bookmark.toEntity() = BookmarkEntity(id, bookId, positionMs, title, note, kind, chapterIndex, createdAt)

fun PlaybackProgressEntity.toDomain() = PlaybackProgress(bookId, positionMs, chapterIndex, percent, lastPlayedAt)

fun PlaybackProgress.toEntity() = PlaybackProgressEntity(bookId, positionMs, chapterIndex, percent, lastPlayedAt)

fun TagEntity.toDomain() = Tag(id, name)

fun Tag.toEntity() = TagEntity(id, name)

fun MetadataCacheEntity.toDomain() = MetadataCache(id, queryKey, payloadJson, source, createdAt)

fun MetadataCache.toEntity() = MetadataCacheEntity(id, queryKey, payloadJson, source, createdAt)
