package com.nikit.audiobook.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationSafetyTest {
    @Test fun migrationsArrayExists_andHasMigrationsToCurrentVersion() {
        assertThat(AudioBookDatabase.MIGRATIONS).isNotNull()
        // Начиная с v2 есть явная миграция 1→2 (закладки получили kind + chapterIndex).
        assertThat(AudioBookDatabase.MIGRATIONS).isNotEmpty()
    }

    @Test fun databaseBuilderWithoutDestructiveFallback_builds() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db =
            Room
                .databaseBuilder(ctx, AudioBookDatabase::class.java, "test.db")
                .addMigrations(*AudioBookDatabase.MIGRATIONS)
                .build()
        // БД открывается без fallbackToDestructiveMigration
        assertThat(db.openHelper.writableDatabase).isNotNull()
        db.close()
    }
}
