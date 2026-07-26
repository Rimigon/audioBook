package com.nikit.audiobook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import com.nikit.audiobook.data.db.entity.BookEntity
import com.nikit.audiobook.data.db.entity.ChapterEntity

@Database(
    entities = [BookEntity::class, ChapterEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AudioBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun chapterDao(): ChapterDao

    companion object {
        // Явные миграции. НИКОГДА не использовать fallbackToDestructiveMigration —
        // каталог ценен и не должен пересоздаваться при смене версии.
        // Пример будущей миграции:
        //   val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { ... } }
        val MIGRATIONS: Array<androidx.room.migration.Migration> =
            arrayOf(
                // MIGRATION_1_2,
            )
    }
}
