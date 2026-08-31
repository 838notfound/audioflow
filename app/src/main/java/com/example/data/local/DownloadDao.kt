package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE status = 'STAGING' ORDER BY timestamp DESC")
    fun getStagingItems(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE status IN ('QUEUED', 'DOWNLOADING', 'EXTRACTING_AUDIO') ORDER BY id ASC")
    fun getActiveQueue(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getCompletedItems(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE status = 'FAILED' ORDER BY timestamp DESC")
    fun getFailedItems(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): DownloadItem?

    @Query("SELECT * FROM download_items WHERE id = :id LIMIT 1")
    fun getItemByIdFlow(id: Long): Flow<DownloadItem?>

    @Query("SELECT * FROM download_items WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedItem(): DownloadItem?

    @Query("SELECT COUNT(*) FROM download_items WHERE status = 'QUEUED'")
    suspend fun getQueuedCount(): Int

    @Query("SELECT COUNT(*) FROM download_items WHERE status IN ('DOWNLOADING', 'EXTRACTING_AUDIO')")
    suspend fun getActiveDownloadingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DownloadItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DownloadItem>): List<Long>

    @Update
    suspend fun updateItem(item: DownloadItem)

    @Delete
    suspend fun deleteItem(item: DownloadItem)

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM download_items WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("DELETE FROM download_items WHERE status = 'STAGING'")
    suspend fun clearStaging()

    @Query("UPDATE download_items SET status = 'FAILED', errorMessage = 'Interrupted' WHERE status IN ('DOWNLOADING', 'EXTRACTING_AUDIO')")
    suspend fun resetStuckDownloads()

    @Query("UPDATE download_items SET status = 'QUEUED', progress = 0, errorMessage = null WHERE id = :id")
    suspend fun retryItem(id: Long)

    @Query("UPDATE download_items SET status = 'QUEUED' WHERE status = 'STAGING'")
    suspend fun approveAllStaging()
}
