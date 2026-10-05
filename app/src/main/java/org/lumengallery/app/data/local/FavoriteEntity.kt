package org.lumengallery.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaId: Long,
    val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
