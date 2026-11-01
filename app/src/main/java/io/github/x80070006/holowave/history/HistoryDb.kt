package io.github.x80070006.holowave.history

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One finished listening session. Title, artist and album are copied in so the
 * history stays meaningful if the file is later moved, retagged or deleted;
 * [trackKey] is the stable identity shared with the lyrics cache.
 */
@Entity(
    tableName = "listens",
    indices = [Index("trackKey"), Index("startedAt")],
)
data class ListenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val startedAt: Long,
    val listenedMs: Long,
    val durationMs: Long,
    val outcome: Int,
    val leftAtMs: Long,
    val repeats: Int,
    val seeks: Int,
    /** Local hour of day (0-23) and day of week (1 = Monday … 7 = Sunday) when it started. */
    val hour: Int,
    val dayOfWeek: Int,
    /** [OutputKind] code: speaker, wired headphones, Bluetooth… */
    val output: Int,
    /** [io.github.x80070006.holowave.data.TrackSource] code. */
    val source: Int,
)

@Dao
interface ListenDao {
    @Insert suspend fun insert(listen: ListenEntity)

    @Query("SELECT COUNT(*) FROM listens")
    fun count(): Flow<Int>

    @Query("SELECT * FROM listens ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<ListenEntity>

    @Query("DELETE FROM listens")
    suspend fun deleteAll()
}

@Database(entities = [ListenEntity::class], version = 1, exportSchema = true)
abstract class HolowaveDb : RoomDatabase() {
    abstract fun listens(): ListenDao

    companion object {
        fun open(context: Context): HolowaveDb =
            Room.databaseBuilder(context.applicationContext, HolowaveDb::class.java, "holowave.db")
                // No destructive fallback: a missing migration must fail loudly in tests,
                // never silently wipe a user's listening history.
                .build()
    }
}
