package com.thelab.mediahub.engine

import android.content.Context
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object InternetIngestionEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // 1. Fetch Latest 2026 Movies metadata from open APIs
    suspend fun fetch2026Movies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<MediaItem>()
        try {
            val url = "https://raw.githubusercontent.com/prust/wikipedia-movie-data/master/movies.json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonArray = JSONArray(body)
                    for (i in (jsonArray.length() - 1) downTo (jsonArray.length() - 20)) {
                        if (i < 0) break
                        val obj = jsonArray.getJSONObject(i)
                        val title = obj.optString("title", "Unknown Title")
                        val year = obj.optInt("year", 2026)
                        
                        movies.add(
                            MediaItem(
                                path = "web://movie/$title",
                                fileName = "$title ($year)",
                                size = 0L,
                                mimeType = "video/movie-entry",
                                category = FileCategory.VIDEO,
                                formattedLabel = "2026 Movie → $title - Year: $year",
                                parentFolder = "Internet / 2026 Movies",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext movies
    }

    // 2. Fetch Latest Prompts & Web Insights
    suspend fun fetchLatestPrompts(): List<MediaItem> = withContext(Dispatchers.IO) {
        val prompts = mutableListOf<MediaItem>()
        try {
            val url = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val lines = body.lines().take(15)
                    lines.forEachIndexed { index, line ->
                        if (index > 0 && line.isNotBlank()) {
                            val parts = line.split(",")
                            val act = parts.firstOrNull()?.replace("\"", "") ?: "Prompt $index"
                            prompts.add(
                                MediaItem(
                                    path = "web://prompt/$act",
                                    fileName = "Prompt: $act",
                                    size = line.length.toLong(),
                                    mimeType = "text/plain",
                                    category = FileCategory.DOCUMENT,
                                    formattedLabel = "AI Prompt → $act",
                                    parentFolder = "Internet / AI Prompts",
                                    lastModified = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext prompts
    }

    // 3. Scan Local Subnet for Connected Wi-Fi Devices
    suspend fun scanLocalWifiNetwork(): List<MediaItem> = withContext(Dispatchers.IO) {
        val activeDevices = mutableListOf<MediaItem>()
        try {
            val subnet = "192.168.1"
            for (i in 1..20) { // Scans first 20 IPs on subnet
                val host = "$subnet.$i"
                val address = InetAddress.getByName(host)
                if (address.isReachable(100)) {
                    activeDevices.add(
                        MediaItem(
                            path = "network://$host",
                            fileName = "Active Host: $host",
                            size = 0L,
                            mimeType = "network/device",
                            category = FileCategory.PACKAGE,
                            formattedLabel = "LAN Device → Hostname: ${address.hostName} [$host]",
                            parentFolder = "Wi-Fi Network / Subnet",
                            lastModified = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext activeDevices
    }
}
