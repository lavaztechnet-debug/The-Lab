package com.thelab.mediahub.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thelab.mediahub.data.AppDatabase
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import com.thelab.mediahub.engine.FileClassifier
import com.thelab.mediahub.engine.InternetIngestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mediaDao = db.mediaDao()

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _excludeLocal = MutableStateFlow(false)
    val excludeLocal: StateFlow<Boolean> = _excludeLocal.asStateFlow()

    init {
        loadAllMedia()
    }

    fun loadAllMedia() {
        viewModelScope.launch {
            mediaDao.getAllMediaItems().collect { items ->
                _mediaItems.value = if (_excludeLocal.value) {
                    items.filter { it.path.startsWith("web://") || it.path.startsWith("network://") }
                } else {
                    items
                }
            }
        }
    }

    fun toggleExcludeLocal(exclude: Boolean) {
        _excludeLocal.value = exclude
        loadAllMedia()
    }

    fun downloadMediaItem(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "The-Lab")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val targetFile = File(downloadDir, item.fileName.replace("/", "_") + ".txt")
            targetFile.writeText("Resource: ${item.fileName}\nSource Path: ${item.path}\nCategory: ${item.category}\n\n${item.formattedLabel}")
        }
    }

    fun triggerFullSweep() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            
            val discoveredFiles = mutableListOf<MediaItem>()

            val targetDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            )

            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        discoveredFiles.add(FileClassifier.classifyFile(getApplication(), file))
                    }
                }
            }

            val movies = InternetIngestionEngine.fetch2026Movies()
            val prompts = InternetIngestionEngine.fetchLatestPrompts()
            val networkDevices = InternetIngestionEngine.scanLocalWifiNetwork()

            discoveredFiles.addAll(movies)
            discoveredFiles.addAll(prompts)
            discoveredFiles.addAll(networkDevices)

            mediaDao.insertAll(discoveredFiles)
            _isScanning.value = false
        }
    }

    fun filterByCategory(category: FileCategory?) {
        viewModelScope.launch {
            if (category == null) {
                loadAllMedia()
            } else {
                mediaDao.getMediaByCategory(category).collect { items ->
                    _mediaItems.value = if (_excludeLocal.value) {
                        items.filter { it.path.startsWith("web://") || it.path.startsWith("network://") }
                    } else {
                        items
                    }
                }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAllMedia()
            } else {
                mediaDao.searchMedia(query).collect { items ->
                    _mediaItems.value = if (_excludeLocal.value) {
                        items.filter { it.path.startsWith("web://") || it.path.startsWith("network://") }
                    } else {
                        items
                    }
                }
            }
        }
    }
}
