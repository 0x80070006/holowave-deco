package io.github.x80070006.holowave.history

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * A song listened to in NouTube. One row per song, whatever the number of plays.
 * [artwork] is the file name of the cover kept in the app's private storage.
 */
@Entity(
    tableName = "streamed",
    indices = [Index(value = ["trackKey"], unique = true), Index("lastSeen")],
)
data class StreamedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val videoId: String?,
    val artwork: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val plays: Int,
)

@Dao
abstract class StreamedDao {
    @Query("SELECT * FROM streamed ORDER BY lastSeen DESC LIMIT 5000")
    abstract fun all(): Flow<List<StreamedEntity>>

    @Query("SELECT COUNT(*) FROM streamed")
    abstract fun count(): Flow<Int>

    @Query("SELECT * FROM streamed WHERE trackKey = :key LIMIT 1")
    abstract suspend fun find(key: String): StreamedEntity?

    @Query("SELECT artwork FROM streamed WHERE artwork IS NOT NULL")
    abstract suspend fun artworkNames(): List<String>

    @Insert abstract suspend fun insert(e: StreamedEntity)
    @Update abstract suspend fun update(e: StreamedEntity)

    @Query("DELETE FROM streamed")
    abstract suspend fun deleteAll()

    /** Inserts the song or updates its counters, keeping whatever was already known about it. */
    @Transaction
    open suspend fun record(e: StreamedEntity, countPlay: Boolean) {
        val add = if (countPlay) 1 else 0
        val old = find(e.trackKey)
        if (old == null) {
            insert(e.copy(plays = add))
        } else {
            update(
                old.copy(
                    lastSeen = maxOf(old.lastSeen, e.lastSeen),
                    plays = old.plays + add,
                    videoId = old.videoId ?: e.videoId,
                    artwork = old.artwork ?: e.artwork,
                    durationMs = if (old.durationMs > 0) old.durationMs else e.durationMs,
                    album = old.album.ifBlank { e.album },
                ),
            )
        }
    }
}

data class TasteRow(val trackKey: String, val startedAt: Long, val outcome: Int, val hour: Int)

@Dao
interface ListenDao {
    @Insert suspend fun insert(listen: ListenEntity)

    @Query("SELECT COUNT(*) FROM listens")
    fun count(): Flow<Int>

    @Query("SELECT * FROM listens ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<ListenEntity>

    /** Most recently played tracks, newest first, one entry per track. */
    @Query("SELECT trackKey FROM listens GROUP BY trackKey ORDER BY MAX(startedAt) DESC LIMIT :limit")
    fun recentKeys(limit: Int): Flow<List<String>>

    /** The light columns the taste engine needs, newest first. */
    @Query("SELECT trackKey, startedAt, outcome, hour FROM listens ORDER BY startedAt DESC LIMIT :limit")
    suspend fun taste(limit: Int): List<TasteRow>

    @Query("DELETE FROM listens")
    suspend fun deleteAll()
}

@Database(entities = [ListenEntity::class, StreamedEntity::class], version = 2, exportSchema = true)
abstract class HolowaveDb : RoomDatabase() {
    abstract fun listens(): ListenDao
    abstract fun streamed(): StreamedDao

    companion object {
        fun open(context: Context): HolowaveDb =
            Room.databaseBuilder(context.applicationContext, HolowaveDb::class.java, "holowave.db")
                .addMigrations(MIGRATION_1_2)
                // No destructive fallback: a missing migration must fail loudly in tests,
                // never silently wipe a user's listening history.
                .build()
    }
}

/** v1 -> v2: adds the table of songs streamed in NouTube; listening history is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `streamed` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackKey` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, " +
                "`videoId` TEXT, `artwork` TEXT, `firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `plays` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_streamed_trackKey` ON `streamed` (`trackKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_streamed_lastSeen` ON `streamed` (`lastSeen`)")
    }
}
