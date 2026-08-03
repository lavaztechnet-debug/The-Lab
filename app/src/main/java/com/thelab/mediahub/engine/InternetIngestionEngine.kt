package com.thelab.mediahub.engine

import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object InternetIngestionEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // 1. Fetch Full Length Horror Movies -> Category: VIDEO
    suspend fun fetchHorrorMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<MediaItem>()
        try {
            val url = "https://archive.org/advancedsearch.php?q=subject%3A%22horror%22+AND+mediatype%3A%22movies%22&fl[]=identifier,title,year&sort[]=downloads+desc&rows=15&page=1&output=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val docs = JSONObject(body).getJSONObject("response").getJSONArray("docs")
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier", "")
                        val title = doc.optString("title", "Horror Stream")
                        val year = doc.optString("year", "2026")

                        movies.add(
                            MediaItem(
                                path = "https://archive.org/download/$id/$id.mp4",
                                fileName = "$title ($year).mp4",
                                size = 1048576000L,
                                mimeType = "video/mp4",
                                category = FileCategory.VIDEO, // Correct Category
                                formattedLabel = "Video - Horror Movie - Year: $year",
                                parentFolder = "Web / Horror Archive",
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

    // 2. Fetch AI Prompts -> Category: DOCUMENT
    suspend fun fetchLatestPrompts(): List<MediaItem> = withContext(Dispatchers.IO) {
        val prompts = mutableListOf<MediaItem>()
        try {
            val url = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    body.lines().take(20).forEachIndexed { index, line ->
                        if (index > 0 && line.isNotBlank()) {
                            val act = line.split(",").firstOrNull()?.replace("\"", "") ?: "Prompt $index"
                            prompts.add(
                                MediaItem(
                                    path = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv#$index",
                                    fileName = "Prompt_$act.txt",
                                    size = line.length.toLong(),
                                    mimeType = "text/plain",
                                    category = FileCategory.DOCUMENT, // Correct Category
                                    formattedLabel = "Document - System Prompt: $act",
                                    parentFolder = "Web / AI Prompts",
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

    // 3. Scan Subnet Hosts -> Category: PACKAGE
    suspend fun scanLocalWifiNetwork(): List<MediaItem> = withContext(Dispatchers.IO) {
        val networkDevices = mutableListOf<MediaItem>()
        try {
            val subnet = "192.168.1"
            for (i in 1..20) {
                val host = "$subnet.$i"
                val address = InetAddress.getByName(host)
                if (address.isReachable(80)) {
                    networkDevices.add(
                        MediaItem(
                            path = "network://$host",
                            fileName = "Network_Host_$host.bin",
                            size = 0L,
                            mimeType = "network/device",
                            category = FileCategory.PACKAGE, // Correct Category
                            formattedLabel = "Package - Host: ${address.hostName} [$host]",
                            parentFolder = "Wi-Fi Subnet",
                            lastModified = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext networkDevices
    }
}
