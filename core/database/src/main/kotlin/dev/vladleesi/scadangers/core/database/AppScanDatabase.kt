package dev.vladleesi.scadangers.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.vladleesi.scadangers.core.security.apps.AppScanStore
import dev.vladleesi.scadangers.core.security.apps.AssessedApp
import dev.vladleesi.scadangers.core.security.apps.ScanSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** One encoded row per app avoids a single large inventory blob or APK payload. */
@Entity(tableName = "app_results")
data class AppResultEntity(@PrimaryKey val packageName: String, val resultJson: String)

@Entity(tableName = "scan_metadata")
data class ScanMetadataEntity(@PrimaryKey val id: Int = 1, val snapshotJson: String)

@Dao
interface AppScanDao {
    @Query("SELECT * FROM scan_metadata WHERE id = 1")
    suspend fun metadata(): ScanMetadataEntity?

    @Query("SELECT * FROM app_results ORDER BY packageName")
    suspend fun results(): List<AppResultEntity>

    @Query("DELETE FROM app_results")
    suspend fun clearResults()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<AppResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: ScanMetadataEntity)
}

@Database(
    entities = [AppResultEntity::class, ScanMetadataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppScanDatabase : RoomDatabase() {
    abstract fun scans(): AppScanDao
}

/** Lazily supplied database, transactional replacement and decoding off Main; no destructive fallback. */
class RoomAppScanStore(private val database: () -> AppScanDatabase) : AppScanStore {
    override suspend fun load(): ScanSnapshot? = withContext(Dispatchers.IO) {
        val db = database()
        db.withTransaction {
            val dao = db.scans()
            val metadata = dao.metadata() ?: return@withTransaction null
            Json.decodeFromString<ScanSnapshot>(metadata.snapshotJson).copy(
                apps = dao.results().map { Json.decodeFromString<AssessedApp>(it.resultJson) }
            )
        }
    }

    override suspend fun replace(snapshot: ScanSnapshot) = withContext(Dispatchers.IO) {
        val db = database()
        db.withTransaction {
            val dao = db.scans()
            dao.clearResults()
            for (batch in snapshot.apps.chunked(WRITE_BATCH_SIZE)) {
                dao.insertResults(
                    batch.map {
                        AppResultEntity(it.inventory.packageName, Json.encodeToString(it))
                    }
                )
            }
            dao.insertMetadata(
                ScanMetadataEntity(
                    snapshotJson = Json.encodeToString(snapshot.copy(apps = emptyList()))
                )
            )
        }
    }

    private companion object {
        const val WRITE_BATCH_SIZE = 24
    }
}
