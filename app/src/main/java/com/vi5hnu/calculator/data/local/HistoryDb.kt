package com.vi5hnu.calculator.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: Double,
    val pinned: Boolean = false,
    val createdAt: Long,
)

@Dao
interface HistoryDao {

    /** Pinned entries float to the top; within each group, newest first. */
    @Query("SELECT * FROM history ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("UPDATE history SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()

    /**
     * Drops the oldest unpinned rows once the table exceeds [limit].
     *
     * The design caps its localStorage history at 200; this does the same server-side of the
     * Flow so the UI never has to think about it. Pinned rows are exempt — the user asked for
     * those explicitly.
     */
    @Query(
        """
        DELETE FROM history
        WHERE pinned = 0
          AND id NOT IN (
            SELECT id FROM history WHERE pinned = 0 ORDER BY createdAt DESC LIMIT :limit
          )
        """,
    )
    suspend fun trimTo(limit: Int)
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = true)
abstract class MathProDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        /**
         * Deliberately not "calculator" — that name belongs to the old Flutter sqflite file,
         * which is left untouched on disk. History starts fresh, as agreed.
         */
        const val NAME = "mathpro.db"
    }
}
