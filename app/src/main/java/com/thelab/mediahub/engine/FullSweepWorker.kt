package com.thelab.mediahub.engine

import android.content.Context
import androidx.work.*
import com.thelab.mediahub.data.AppDatabase
import com.thelab.mediahub.data.MIN_CONTENT_EPOCH_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FullSweepWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            coroutineScope {
                val apksDeferred = async { MediaStoreIndexer.scanApks2026(applicationContext) }
                val imagesDeferred = async { MediaStoreIndexer.scanImages2026(applicationContext) }
                val videosDeferred = async { MediaStoreIndexer.scanVideos2026(applicationContext) }
                val docsDeferred = async { MediaStoreIndexer.scanDocuments2026(applicationContext) }
                val horrorDeferred = async { InternetIngestionEngine2026.fetchHorrorMovies2026(client) }
                val promptsDeferred = async { InternetIngestionEngine2026.fetchAiPrompts2026(client) }
                val oemApksDeferred = async { InternetIngestionEngine2026.fetchApksFromRepos2026(client) }
                val lanDeferred = async { LanDiscoveryEngine2026.scanSubnets2026(applicationContext) }

                val allDiscovered = apksDeferred.await() +
                        imagesDeferred.await() +
                        videosDeferred.await() +
                        docsDeferred.await() +
                        horrorDeferred.await() +
                        promptsDeferred.await() +
                        oemApksDeferred.await() +
                        lanDeferred.await()

                val valid2026Items = allDiscovered.filter { it.sourceFreshEpoch >= MIN_CONTENT_EPOCH_MS }

                db.mediaDao().upsertAll(valid2026Items)
                db.mediaDao().purgePre2026Items()
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<FullSweepWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FullSweepWorker2026",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
