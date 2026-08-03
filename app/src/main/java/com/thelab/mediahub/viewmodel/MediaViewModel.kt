package com.thelab.mediahub.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.thelab.mediahub.data.*
import com.thelab.mediahub.engine.FullSweepWorker
import com.thelab.mediahub.ui.screens.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val _selectedRange = MutableStateFlow(TimeRangeFilter.ALL)
    val selectedRange: StateFlow<TimeRangeFilter> = _selectedRange.asStateFlow()

    private val _is2026Only = MutableStateFlow(true)
    val is2026Only: StateFlow<Boolean> = _is2026Only.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val mediaItems: StateFlow<List<MediaEntity>> = combine(_selectedRange, _is2026Only) { range, is2026 ->
        val minEpoch = range.days?.let { System.currentTimeMillis() - (it * 24 * 3600 * 1000L) }
        db.mediaDao().getFilteredMedia(
            category = null,
            is2026Only = is2026,
            minEpoch = minEpoch ?: MIN_CONTENT_EPOCH_MS
        )
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setTimeRange(range: TimeRangeFilter) { _selectedRange.value = range }
    fun set2026Only(only2026: Boolean) { _is2026Only.value = only2026 }

    fun triggerManualSweep(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            val request = OneTimeWorkRequestBuilder<FullSweepWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
            _isScanning.value = false
        }
    }

    fun downloadMediaItem(item: MediaEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (item.uriString.startsWith("http://") || item.uriString.startsWith("https://")) {
                    val request = DownloadManager.Request(Uri.parse(item.uriString))
                        .setTitle("The-Lab: ${item.fileName}")
                        .setDescription("Downloading 2026 content...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "The-Lab/${item.fileName}")
                        .setAllowedOverMetered(true)

                    val manager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    manager.enqueue(request)
                } else {
                    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "The-Lab")
                    if (!downloadDir.exists()) downloadDir.mkdirs()
                    val targetFile = File(downloadDir, item.fileName + ".txt")
                    targetFile.writeText("URI: ${item.uriString}\nLabel: ${item.formattedLabel}")
                }
            }
        }
    }

    fun search(query: String) {
        // Handled reactively via DB query flows
    }
}
