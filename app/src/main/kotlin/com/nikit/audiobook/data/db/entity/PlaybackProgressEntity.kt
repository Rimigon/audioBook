package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaybackProgressEntity(
    @PrimaryKey val bookId: String,
    val positionMs: Long,
    val chapterIndex: Int,
    val percent: Float,
    val lastPlayedAt: Long,
)
