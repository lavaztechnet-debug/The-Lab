package com.thelab.mediahub.engine

import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MIN_CONTENT_EPOCH_MS
import com.thelab.mediahub.data.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant

object InternetIngestionEngine2026 {

    suspend fun fetchHorrorMovies2026(client: OkHttpClient): List<MediaEntity> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<MediaEntity>()
        val url = "https://archive.org/advancedsearch.php?q=mediatype%3Amovies+AND+publicdomain%3Atrue+AND+publicdate%3A%5B2026-01-01T00%3A00%3A00Z+TO+NOW%5D&fl[]=identifier,title,publicdate,item_size&sort[]=publicdate+desc&rows=25&page=1&output=json"

        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val docs = JSONObject(body).getJSONObject("response").getJSONArray("docs")
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier")
                        val title = doc.optString("title", "Horror Movie Entry")
                        val pubDateStr = doc.optString("publicdate", "2026-01-01T00:00:00Z")
                        val size = doc.optLong("item_size", 0L)
                        val epoch = parseIsoEpoch(pubDateStr)

                        if (epoch >= MIN_CONTENT_EPOCH_MS) {
                            val streamUrl = "https://archive.org/download/$id/$id.mp4"
                            movies.add(
                                MediaEntity(
                                    uriString = streamUrl,
                                    fileName = "$title (2026).mp4",
                                    size = size,
                                    mimeType = "video/mp4",
                                    category = FileCategory.VIDEO,
                                    formattedLabel = "2026 Archive Stream → $title",
                                    parentFolder = "Web / Horror Movies",
                                    dateAddedEpoch = epoch,
                                    dateModifiedEpoch = epoch,
                                    sourceFreshEpoch = epoch,
                                    is2026Only = true,
                                    contentHash = "movie_$id"
                                )
                            )
                        }
                    }
                }
            }
        }
        return@withContext movies
    }

    suspend fun fetchAiPrompts2026(client: OkHttpClient): List<MediaEntity> = withContext(Dispatchers.IO) {
        val prompts = mutableListOf<MediaEntity>()
        val url = "https://api.github.com/repos/x1xhlol/system-prompts-and-models-of-ai-tools/commits?since=2026-01-01T00:00:00Z"

        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", "TheLab-MediaHub").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonArray = org.json.JSONArray(body)

                    for (i in 0 until minOf(jsonArray.length(), 20)) {
                        val obj = jsonArray.getJSONObject(i)
                        val sha = obj.getString("sha")
                        val commitObj = obj.getJSONObject("commit")
                        val message = commitObj.getString("message").lines().firstOrNull() ?: "AI Prompt Update"
                        val dateStr = commitObj.getJSONObject("committer").getString("date")
                        val epoch = parseIsoEpoch(dateStr)

                        if (epoch >= MIN_CONTENT_EPOCH_MS) {
                            val ageDays = maxOf(1L, (System.currentTimeMillis() - epoch) / (1000 * 3600 * 24))
                            val freshnessScore = (100.0 / ageDays).coerceIn(1.0, 100.0)

                            prompts.add(
                                MediaEntity(
                                    uriString = "https://github.com/x1xhlol/system-prompts-and-models-of-ai-tools/commit/$sha",
                                    fileName = "Prompt_$sha.txt",
                                    size = message.length.toLong(),
                                    mimeType = "text/plain",
                                    category = FileCategory.DOCUMENT,
                                    formattedLabel = "2026 AI Prompt [Freshness: ${freshnessScore.toInt()}/100] → $message",
                                    parentFolder = "Web / Prompts 2026",
                                    dateAddedEpoch = epoch,
                                    dateModifiedEpoch = epoch,
                                    sourceFreshEpoch = epoch,
                                    is2026Only = true,
                                    contentHash = "prompt_$sha",
                                    extraMetadataJson = "{\"freshnessScore\": $freshnessScore}"
                                )
                            )
                        }
                    }
                }
            }
        }
        return@withContext prompts
    }

    suspend fun fetchApksFromRepos2026(client: OkHttpClient): List<MediaEntity> = withContext(Dispatchers.IO) {
        val apks = mutableListOf<MediaEntity>()
        val targetUrl = "https://api.github.com/repos/google/security-research/releases"

        runCatching {
            val request = Request.Builder().url(targetUrl).header("User-Agent", "TheLab-MediaHub").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonArray = org.json.JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val release = jsonArray.getJSONObject(i)
                        val publishedAt = release.getString("published_at")
                        val epoch = parseIsoEpoch(publishedAt)

                        if (epoch >= MIN_CONTENT_EPOCH_MS) {
                            val assets = release.getJSONArray("assets")
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val downloadUrl = asset.getString("browser_download_url")
                                val name = asset.getString("name")

                                if (name.endsWith(".apk")) {
                                    apks.add(
                                        MediaEntity(
                                            uriString = downloadUrl,
                                            fileName = name,
                                            size = asset.getLong("size"),
                                            mimeType = "application/vnd.android.package-archive",
                                            category = FileCategory.PACKAGE,
                                            formattedLabel = "2026 OEM APK Build → $name",
                                            parentFolder = "Web / OEM Packages",
                                            dateAddedEpoch = epoch,
                                            dateModifiedEpoch = epoch,
                                            sourceFreshEpoch = epoch,
                                            is2026Only = true,
                                            contentHash = "apk_${asset.getString("id")}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return@withContext apks
    }

    private fun parseIsoEpoch(dateStr: String): Long {
        return runCatching {
            Instant.parse(dateStr).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }
}
