package com.nikit.audiobook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.BookmarkDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.db.dao.PlaybackProgressDao
import com.nikit.audiobook.data.db.dao.TagDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.BookTagEntity
import com.nikit.audiobook.data.db.entity.BookmarkEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity
import com.nikit.audiobook.data.db.entity.MetadataCacheEntity
import com.nikit.audiobook.data.db.entity.PlaybackProgressEntity
import com.nikit.audiobook.data.db.entity.TagEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        PlaybackProgressEntity::class,
        TagEntity::class,
        BookTagEntity::class,
        MetadataCacheEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AudioBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun chapterDao(): ChapterDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun playbackProgressDao(): PlaybackProgressDao

    abstract fun tagDao(): TagDao

    abstract fun metadataCacheDao(): MetadataCacheDao

    companion object {
        // Явные миграции. НИКОГДА не использовать fallbackToDestructiveMigration —
        // каталог ценен и не должен пересоздаваться при смене версии.
        val MIGRATIONS: Array<androidx.room.migration.Migration> =
            arrayOf(
                // v1 → v2: закладки получают тип (kind) и индекс главы (chapterIndex).
                object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE bookmarks ADD COLUMN kind INTEGER NOT NULL DEFAULT 0")
                        database.execSQL("ALTER TABLE bookmarks ADD COLUMN chapterIndex INTEGER")
                    }
                },
                // v2 → v3: функционал полок полностью убран — таблицы удаляются.
                object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("DROP TABLE IF EXISTS shelf_memberships")
                        database.execSQL("DROP TABLE IF EXISTS shelves")
                    }
                },
            )
    }
}
