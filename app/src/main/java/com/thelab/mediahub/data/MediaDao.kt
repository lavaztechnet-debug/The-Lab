package com.thelab.mediahub.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("""
        SELECT * FROM media_items 
        WHERE (:is2026Only = 0 OR is2026Only = 1)
        AND (:category IS NULL OR category = :category)
        AND (:minEpoch IS NULL OR sourceFreshEpoch >= :minEpoch)
        ORDER BY sourceFreshEpoch DESC
    """)
    fun getFilteredMedia(
        category: FileCategory?,
        is2026Only: Boolean,
        minEpoch: Long?
    ): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE contentHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(item: MediaEntity)

    @Transaction
    suspend fun upsertSmart(item: MediaEntity) {
        if (item.sourceFreshEpoch < MIN_CONTENT_EPOCH_MS) return
        val existing = findByHash(item.contentHash)
        if (existing == null) {
            insertRaw(item)
        } else if (item.sourceFreshEpoch >= existing.sourceFreshEpoch) {
            insertRaw(item.copy(uriString = existing.uriString))
        }
    }

    @Transaction
    suspend fun upsertAll(items: List<MediaEntity>) {
        items.forEach { upsertSmart(it) }
    }

    @Query("DELETE FROM media_items WHERE sourceFreshEpoch < :minEpoch")
    suspend fun purgePre2026Items(minEpoch: Long = MIN_CONTENT_EPOCH_MS)
}

class Converters {
    @TypeConverter
    fun fromCategory(category: FileCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): FileCategory = runCatching { FileCategory.valueOf(value) }.getOrDefault(FileCategory.UNKNOWN)
}

@Database(entities = [MediaEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "the_lab_media_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN sourceFreshEpoch INTEGER NOT NULL DEFAULT 1767225600000")
                db.execSQL("ALTER TABLE media_items ADD COLUMN is2026Only INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE media_items ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_items_sourceFreshEpoch` ON media_items (`sourceFreshEpoch` DESC)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_items_contentHash` ON media_items (`contentHash`)")
            }
        }
    }
}
