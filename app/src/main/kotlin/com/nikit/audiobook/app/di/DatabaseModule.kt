package com.nikit.audiobook.app.di

import android.content.Context
import androidx.room.Room
import com.nikit.audiobook.data.db.AudioBookDatabase
import com.nikit.audiobook.data.db.dao.BookDao
import com.nikit.audiobook.data.db.dao.ChapterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext ctx: Context,
    ): AudioBookDatabase =
        Room
            .databaseBuilder(ctx, AudioBookDatabase::class.java, "audiobook.db")
            .addMigrations(*AudioBookDatabase.MIGRATIONS)
            .build()

    @Provides fun provideBookDao(db: AudioBookDatabase): BookDao = db.bookDao()

    @Provides fun provideChapterDao(db: AudioBookDatabase): ChapterDao = db.chapterDao()
}
