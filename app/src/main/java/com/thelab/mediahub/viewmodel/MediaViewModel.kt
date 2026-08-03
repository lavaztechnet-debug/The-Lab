package com.thelab.mediahub.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
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

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _excludeLocal = MutableStateFlow(false)
    val excludeLocal: StateFlow<Boolean> = _excludeLocal.asStateFlow()

    init {
        loadAllMedia()
    }

    fun loadAllMedia() {
        viewModelScope.launch {
            val flow = if (_selectedCategory.value == null || _selectedCategory.value == FileCategory.ALL) {
                mediaDao.getAllMediaItems()
            } else {
                mediaDao.getMediaByCategory(_selectedCategory.value!!)
            }

            flow.collect { items ->
                _mediaItems.value = if (_excludeLocal.value) {
                    items.filter { it.path.startsWith("http") || it.path.startsWith("network://") }
                } else {
                    items
                }
            }
        }
    }

    fun filterByCategory(category: FileCategory?) {
        _selectedCategory.value = category
        loadAllMedia()
    }

    fun toggleExcludeLocal(exclude: Boolean) {
        _excludeLocal.value = exclude
        loadAllMedia()
    }

    fun downloadMediaItem(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item.path.startsWith("http://") || item.path.startsWith("https://")) {
                    val request = DownloadManager.Request(Uri.parse(item.path))
                        .setTitle("The-Lab: " + item.fileName)
                        .setDescription("Downloading discovered media...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "The-Lab/" + item.fileName)
                        .setAllowedOverMetered(true)

                    val downloadManager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                } else {
                    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "The-Lab")
                    if (!downloadDir.exists()) downloadDir.mkdirs()
                    val targetFile = File(downloadDir, item.fileName + ".txt")
                    targetFile.writeText("Label: ${item.formattedLabel}\nPath: ${item.path}\nCategory: ${item.category.name}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerFullSweep() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            
            val discoveredFiles = mutableListOf<MediaItem>()

            // 1. Scan Storage
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

            // 2. Scan Web & Network
            val horrorMovies = InternetIngestionEngine.fetchHorrorMovies()
            val prompts = InternetIngestionEngine.fetchLatestPrompts()
            val networkDevices = InternetIngestionEngine.scanLocalWifiNetwork()

            discoveredFiles.addAll(horrorMovies)
            discoveredFiles.addAll(prompts)
            discoveredFiles.addAll(networkDevices)

            mediaDao.insertAll(discoveredFiles)
            _isScanning.value = false
            loadAllMedia()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAllMedia()
            } else {
                mediaDao.searchMedia(query).collect { items ->
                    _mediaItems.value = if (_excludeLocal.value) {
                        items.filter { it.path.startsWith("http") || it.path.startsWith("network://") }
                    } else {
                        items
                    }
                }
            }
        }
    }
}
