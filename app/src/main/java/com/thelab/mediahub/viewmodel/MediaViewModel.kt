package com.thelab.mediahub.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thelab.mediahub.data.AppDatabase
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import com.thelab.mediahub.engine.FileClassifier
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

    init {
        loadAllMedia()
    }

    fun loadAllMedia() {
        viewModelScope.launch {
            mediaDao.getAllMediaItems().collect { items ->
                _mediaItems.value = items
            }
        }
    }

    fun triggerDirectorySweep() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val targetDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            )

            val discoveredFiles = mutableListOf<MediaItem>()
            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        discoveredFiles.add(FileClassifier.classifyFile(getApplication(), file))
                    }
                }
            }

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
                    _mediaItems.value = items
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
                    _mediaItems.value = items
                }
            }
        }
    }
}
