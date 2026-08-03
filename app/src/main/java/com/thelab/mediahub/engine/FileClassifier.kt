package com.thelab.mediahub.engine

import android.content.Context
import android.content.pm.PackageManager
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileClassifier {

    fun classifyFile(context: Context, file: File): MediaItem {
        val extension = file.extension.lowercase()
        val category = when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "heic" -> FileCategory.PHOTO
            "mp4", "mkv", "webm", "avi", "3gp" -> FileCategory.VIDEO
            "pdf", "docx", "xlsx", "txt", "md", "csv" -> FileCategory.DOCUMENT
            "apk", "zip", "tar", "gz", "7z", "bin" -> FileCategory.PACKAGE
            else -> FileCategory.UNKNOWN
        }

        val formattedSize = formatFileSize(file.length())
        var extraDetails = ""

        if (extension == "apk") {
            extraDetails = extractApkMetadata(context, file)
        }

        val label = "${file.name} → ${category.name} - $formattedSize $extraDetails".trim()

        return MediaItem(
            path = file.absolutePath,
            fileName = file.name,
            size = file.length(),
            mimeType = getMimeType(extension),
            category = category,
            formattedLabel = label,
            parentFolder = file.parentFile?.name ?: "Root",
            lastModified = file.lastModified(),
            extraMetadata = extraDetails
        )
    }

    private fun extractApkMetadata(context: Context, file: File): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
            if (info != null) {
                "[Pkg: ${info.packageName} v${info.versionName}]"
            } else ""
        } catch (e: Exception) {
            "[Corrupted APK]"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun getMimeType(extension: String): String {
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
