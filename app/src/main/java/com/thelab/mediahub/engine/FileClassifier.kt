package com.thelab.mediahub.engine

import com.thelab.mediahub.data.FileCategory

object FileClassifier {

    fun classifyMimeAndPath(mimeType: String?, path: String): FileCategory {
        val mime = mimeType?.lowercase() ?: ""
        val extension = path.substringAfterLast('.', "").lowercase()

        return when {
            mime.contains("vnd.android.package-archive") || extension == "apk" -> FileCategory.PACKAGE
            mime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> FileCategory.PHOTO
            mime.startsWith("video/") || extension in listOf("mp4", "mkv", "webm", "avi", "3gp") -> FileCategory.VIDEO
            mime.startsWith("text/") || mime.contains("pdf") || mime.contains("document") || extension in listOf("pdf", "txt", "docx", "md") -> FileCategory.DOCUMENT
            path.startsWith("network://") -> FileCategory.PACKAGE
            else -> FileCategory.UNKNOWN
        }
    }
}
