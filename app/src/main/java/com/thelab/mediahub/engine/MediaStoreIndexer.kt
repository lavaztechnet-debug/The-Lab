package com.thelab.mediahub.engine

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MIN_CONTENT_EPOCH_MS
import com.thelab.mediahub.data.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object MediaStoreIndexer {

    private const val BATCH_SIZE = 500

    suspend fun scanApks2026(context: Context): List<MediaEntity> = withContext(Dispatchers.IO) {
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk'"
        val selectionArgs = arrayOf("application/vnd.android.package-archive")

        queryMediaStore(context, collection, projection, selection, selectionArgs, FileCategory.PACKAGE)
    }

    suspend fun scanImages2026(context: Context): List<MediaEntity> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        queryMediaStore(context, collection, projection, null, null, FileCategory.PHOTO)
    }

    suspend fun scanVideos2026(context: Context): List<MediaEntity> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        queryMediaStore(context, collection, projection, null, null, FileCategory.VIDEO)
    }

    suspend fun scanDocuments2026(context: Context): List<MediaEntity> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN ('application/pdf', 'text/plain', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')"
        queryMediaStore(context, collection, projection, selection, null, FileCategory.DOCUMENT)
    }

    private suspend fun queryMediaStore(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        category: FileCategory
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaEntity>()
        val minEpochSeconds = MIN_CONTENT_EPOCH_MS / 1000L

        val combinedSelection = if (selection.isNullOrEmpty()) {
            "(${MediaStore.MediaColumns.DATE_ADDED} >= $minEpochSeconds OR ${MediaStore.MediaColumns.DATE_MODIFIED} >= $minEpochSeconds)"
        } else {
            "($selection) AND (${MediaStore.MediaColumns.DATE_ADDED} >= $minEpochSeconds OR ${MediaStore.MediaColumns.DATE_MODIFIED} >= $minEpochSeconds)"
        }

        context.contentResolver.query(
            uri,
            projection,
            combinedSelection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            var count = 0
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "unnamed_item"
                val size = cursor.getLong(sizeCol)
                val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                val dateAddedMs = cursor.getLong(addedCol) * 1000L
                val dateModMs = cursor.getLong(modCol) * 1000L

                val freshEpoch = maxOf(dateAddedMs, dateModMs)
                if (freshEpoch < MIN_CONTENT_EPOCH_MS) continue

                val itemUri = ContentUris.withAppendedId(uri, id)
                val hash = computePartialHash(context, itemUri)

                results.add(
                    MediaEntity(
                        uriString = itemUri.toString(),
                        fileName = name,
                        size = size,
                        mimeType = mime,
                        category = category,
                        formattedLabel = "$name • ${category.name} • 2026 Verified",
                        parentFolder = "MediaStore / ${category.name}",
                        dateAddedEpoch = dateAddedMs,
                        dateModifiedEpoch = dateModMs,
                        sourceFreshEpoch = freshEpoch,
                        is2026Only = true,
                        contentHash = hash
                    )
                )

                count++
                if (count % BATCH_SIZE == 0) yield()
            }
        }
        return@withContext results
    }

    private fun computePartialHash(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(65536)
                val bytesRead = stream.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) return uri.toString().hashCode().toString()
                val digest = MessageDigest.getInstance("MD5")
                digest.update(buffer, 0, bytesRead)
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }.getOrNull() ?: uri.toString().hashCode().toString()
    }
}
