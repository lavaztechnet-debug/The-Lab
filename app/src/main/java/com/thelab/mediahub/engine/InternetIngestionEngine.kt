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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Query Internet Archive API for public domain horror & found footage movies
    suspend fun fetchHorrorMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<MediaItem>()
        try {
            val url = "https://archive.org/advancedsearch.php?q=subject%3A%22horror%22+AND+mediatype%3A%22movies%22&fl[]=identifier,title,year&sort[]=downloads+desc&rows=20&page=1&output=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonObj = JSONObject(body)
                    val docs = jsonObj.getJSONObject("response").getJSONArray("docs")
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier", "")
                        val title = doc.optString("title", "Horror Movie")
                        val year = doc.optString("year", "2026")
                        val streamUrl = "https://archive.org/download/$id/$id.mp4"

                        movies.add(
                            MediaItem(
                                path = streamUrl,
                                fileName = "Horror: $title ($year)",
                                size = 1048576000L, // ~1GB estimate
                                mimeType = "video/mp4",
                                category = FileCategory.VIDEO,
                                formattedLabel = "Full Horror Movie → $title ($year) [Archive.org Stream]",
                                parentFolder = "Internet / Horror Archive",
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

    // Fetch AI Prompts
    suspend fun fetchLatestPrompts(): List<MediaItem> = withContext(Dispatchers.IO) {
        val prompts = mutableListOf<MediaItem>()
        try {
            val url = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val lines = body.lines().take(20)
                    lines.forEachIndexed { index, line ->
                        if (index > 0 && line.isNotBlank()) {
                            val act = line.split(",").firstOrNull()?.replace("\"", "") ?: "Prompt $index"
                            prompts.add(
                                MediaItem(
                                    path = "https://raw.githubusercontent.com/f/awesome-chatgpt-prompts/main/prompts.csv",
                                    fileName = "AI Prompt: $act",
                                    size = line.length.toLong(),
                                    mimeType = "text/plain",
                                    category = FileCategory.DOCUMENT,
                                    formattedLabel = "AI System Prompt → $act",
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

    // Scan Subnet
    suspend fun scanLocalWifiNetwork(): List<MediaItem> = withContext(Dispatchers.IO) {
        val activeDevices = mutableListOf<MediaItem>()
        try {
            val subnet = "192.168.1"
            for (i in 1..25) {
                val host = "$subnet.$i"
                val address = InetAddress.getByName(host)
                if (address.isReachable(80)) {
                    activeDevices.add(
                        MediaItem(
                            path = "network://$host",
                            fileName = "LAN Device: $host",
                            size = 0L,
                            mimeType = "network/device",
                            category = FileCategory.PACKAGE,
                            formattedLabel = "Active Network Host → ${address.hostName} [$host]",
                            parentFolder = "Wi-Fi Subnet Discovery",
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
