package com.thelab.mediahub.engine

import android.content.Context
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MIN_CONTENT_EPOCH_MS
import com.thelab.mediahub.data.MediaEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.concurrent.TimeUnit

data class HostScanResult(val host: String, val openPorts: List<Int>, val discoveredMedia: List<MediaEntity>)

object LanDiscoveryEngine2026 {

    private val PORTS_TO_PROBE = listOf(80, 443, 8000, 8080, 5000)
    private val client = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .build()

    suspend fun scanSubnets2026(context: Context): List<MediaEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<MediaEntity>()
        val semaphore = Semaphore(20)
        val targetSubnets = listOf("192.168.1", "192.168.0", "10.0.0")

        val jobs = mutableListOf<Deferred<HostScanResult?>>()

        coroutineScope {
            for (subnet in targetSubnets) {
                for (hostLastByte in 1..30) {
                    val host = "$subnet.$hostLastByte"
                    jobs.add(async {
                        semaphore.withPermit { probeHost2026(host, PORTS_TO_PROBE) }
                    })
                }
            }
            jobs.awaitAll().filterNotNull().forEach { discovered.addAll(it.discoveredMedia) }
        }
        return@withContext discovered
    }

    suspend fun probeHost2026(host: String, ports: List<Int>): HostScanResult? = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<Int>()
        val mediaList = mutableListOf<MediaEntity>()

        for (port in ports) {
            val isPortOpen = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 250)
                    true
                }
            }.getOrDefault(false)

            if (isPortOpen) {
                openPorts.add(port)
                val scheme = if (port == 443) "https" else "http"
                val targetUrl = "$scheme://$host:$port/"

                runCatching {
                    val request = Request.Builder().url(targetUrl).build()
                    client.newCall(request).execute().use { response ->
                        val lastModHeader = response.header("Last-Modified")
                        val pubDateEpoch = parseHeaderEpoch(lastModHeader)

                        if (pubDateEpoch >= MIN_CONTENT_EPOCH_MS) {
                            val mime = response.body?.contentType()?.toString() ?: "text/html"
                            val category = when {
                                mime.contains("video") -> FileCategory.VIDEO
                                mime.contains("pdf") || mime.contains("text") -> FileCategory.DOCUMENT
                                mime.contains("android.package-archive") -> FileCategory.PACKAGE
                                else -> FileCategory.PACKAGE
                            }

                            mediaList.add(
                                MediaEntity(
                                    uriString = targetUrl,
                                    fileName = "LAN_Host_${host}_Port_$port",
                                    size = response.body?.contentLength() ?: 0L,
                                    mimeType = mime,
                                    category = category,
                                    formattedLabel = "LAN Node → $host:$port [$mime]",
                                    parentFolder = "LAN Discovery",
                                    dateAddedEpoch = pubDateEpoch,
                                    dateModifiedEpoch = pubDateEpoch,
                                    sourceFreshEpoch = pubDateEpoch,
                                    is2026Only = true,
                                    contentHash = "lan_${host}_${port}_$pubDateEpoch"
                                )
                            )
                        }
                    }
                }
            }
        }

        if (openPorts.isEmpty()) null else HostScanResult(host, openPorts, mediaList)
    }

    private fun parseHeaderEpoch(header: String?): Long {
        if (header.isNullOrEmpty()) return System.currentTimeMillis()
        return runCatching {
            Instant.from(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(header)).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }
}
