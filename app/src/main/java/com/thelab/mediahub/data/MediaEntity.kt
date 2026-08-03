package com.thelab.mediahub.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Enforces strict 2026-01-01T00:00:00Z epoch (1767225600000 ms)
const val MIN_CONTENT_EPOCH_MS: Long = 1767225600000L

enum class FileCategory {
    ALL, PHOTO, VIDEO, DOCUMENT, PACKAGE, UNKNOWN
}

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["sourceFreshEpoch"], orders = [Index.Order.DESC]),
        Index(value = ["contentHash"], unique = true)
    ]
)
data class MediaEntity(
    @PrimaryKey val uriString: String,
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val category: FileCategory,
    val formattedLabel: String,
    val parentFolder: String,
    val dateAddedEpoch: Long,
    val dateModifiedEpoch: Long,
    val sourceFreshEpoch: Long, // Content timestamp enforced >= 2026-01-01
    val is2026Only: Boolean = true,
    val contentHash: String,    // Fingerprint of content
    val extraMetadataJson: String? = null
) {
    init {
        require(sourceFreshEpoch >= MIN_CONTENT_EPOCH_MS) {
            "Validation Error: Items prior to 2026-01-01 are forbidden in this database. Provided: $sourceFreshEpoch"
        }
    }
}
