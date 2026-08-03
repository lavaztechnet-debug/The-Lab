package com.thelab.mediahub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileCategory {
    ALL, PHOTO, VIDEO, DOCUMENT, PACKAGE, UNKNOWN
}

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey val path: String,
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val category: FileCategory,
    val formattedLabel: String,
    val parentFolder: String,
    val lastModified: Long,
    val extraMetadata: String? = null
)
