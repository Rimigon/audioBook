package com.nikit.audiobook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.data.db.dao.ShelfDao
import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
import com.nikit.audiobook.data.db.entity.ShelfEntity
import com.nikit.audiobook.data.db.entity.ShelfMembershipEntity
import com.nikit.audiobook.data.db.entity.TagEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        PlaybackProgressEntity::class,
        ShelfEntity::class,
        ShelfMembershipEntity::class,
        TagEntity::class,
        BookTagEntity::class,
        MetadataCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AudioBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun chapterDao(): ChapterDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun playbackProgressDao(): PlaybackProgressDao

    abstract fun shelfDao(): ShelfDao

    abstract fun tagDao(): TagDao

    abstract fun metadataCacheDao(): MetadataCacheDao

    companion object {
        // Явные миграции. НИКОГДА не использовать fallbackToDestructiveMigration —
        // каталог ценен и не должен пересоздаваться при смене версии.
        val MIGRATIONS: Array<androidx.room.migration.Migration> =
            arrayOf(
                // MIGRATION_1_2 = object : Migration(1, 2) { ... }
            )
    }
}
