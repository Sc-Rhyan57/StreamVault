package com.streamvault.data.local

import androidx.room.*
import com.streamvault.data.models.WatchProgress
import com.streamvault.data.models.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE contentId = :id AND profileId = :profileId LIMIT 1")
    suspend fun get(id: String, profileId: String): WatchProgress?

    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun getAll(profileId: String): Flow<List<WatchProgress>>

    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT 20")
    fun getRecent(profileId: String): Flow<List<WatchProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgress)

    @Query("DELETE FROM watch_progress WHERE contentId = :id AND profileId = :profileId")
    suspend fun delete(id: String, profileId: String)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun getAll(profileId: String): Flow<List<WatchlistItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE contentId = :id AND profileId = :profileId)")
    fun contains(id: String, profileId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE contentId = :id AND profileId = :profileId")
    suspend fun remove(id: String, profileId: String)
}

@Database(
    entities = [WatchProgress::class, WatchlistItem::class],
    version = 1,
    exportSchema = false
)
abstract class StreamVaultDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun watchlistDao(): WatchlistDao
}
