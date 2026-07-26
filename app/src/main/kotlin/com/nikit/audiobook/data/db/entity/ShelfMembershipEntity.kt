package com.nikit.audiobook.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shelf_memberships",
    primaryKeys = ["shelfId", "bookId"],
    foreignKeys = [
        ForeignKey(entity = ShelfEntity::class, parentColumns = ["id"], childColumns = ["shelfId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bookId")],
)
data class ShelfMembershipEntity(
    val shelfId: String,
    val bookId: String,
)
