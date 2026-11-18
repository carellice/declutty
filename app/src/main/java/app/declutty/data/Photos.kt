package app.declutty.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/** Una foto della galleria, letta da MediaStore. */
data class Photo(
    val id: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val bucket: String,
    val isFavorite: Boolean,
    val isScreenshot: Boolean,
) {
    val megapixels: Double get() = width.toDouble() * height / 1_000_000.0
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
}

/** Impronta visiva e qualità calcolate da una miniatura. */
class PhotoFeatures(
    val pHash: Long,
    val dHash: Long,
    /** Griglia 4x4 di colori medi RGB (48 byte). */
    val colorGrid: ByteArray,
    /** Varianza del Laplaciano: più è alta, più la foto è nitida. */
    val sharpness: Float,
    /** 0..1, 1 = ben esposta (niente zone bruciate, luminosità equilibrata). */
    val exposure: Float,
)

data class AnalyzedPhoto(val photo: Photo, val features: PhotoFeatures)

data class GroupMember(
    val photo: Photo,
    /** Qualità relativa al gruppo, 0..100. */
    val score: Int,
    /** Somiglianza (0..1) con la foto migliore del gruppo. */
    val similarityToBest: Float,
    val reasons: List<String>,
)

data class SimilarGroup(val id: Long, val members: List<GroupMember>, val bestId: Long, val isExactDuplicate: Boolean) {
    val best: GroupMember get() = members.first { it.photo.id == bestId }
}

enum class TimeWindow(val label: String, val millis: Long) {
    MIN_1("1 min", 60_000L),
    MIN_10("10 min", 10 * 60_000L),
    HOUR_1("1 ora", 60 * 60_000L),
    DAY_1("1 giorno", 24 * 60 * 60_000L),
    UNLIMITED("Sempre", Long.MAX_VALUE),
}

enum class DeleteMode { TRASH, PERMANENT }

data class PhotoSettings(
    /** Somiglianza minima (0.75..0.98) perché due foto finiscano nello stesso gruppo. */
    val threshold: Float = 0.88f,
    val timeWindow: TimeWindow = TimeWindow.MIN_10,
    val exactEverywhere: Boolean = true,
    val includeScreenshots: Boolean = false,
    val protectFavorites: Boolean = true,
    val deleteMode: DeleteMode = DeleteMode.TRASH,
)

class PhotoSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("photos", Context.MODE_PRIVATE)

    var lastScanAt: Long
        get() = prefs.getLong("lastScanAt", 0L)
        set(value) = prefs.edit().putLong("lastScanAt", value).apply()

    var freedBytes: Long
        get() = prefs.getLong("freed", 0L)
        set(value) = prefs.edit().putLong("freed", value).apply()

    fun load(): PhotoSettings {
        val d = PhotoSettings()
        return PhotoSettings(
            threshold = prefs.getFloat("threshold", d.threshold),
            timeWindow = runCatching { TimeWindow.valueOf(prefs.getString("window", null)!!) }.getOrDefault(d.timeWindow),
            exactEverywhere = prefs.getBoolean("exactEverywhere", d.exactEverywhere),
            includeScreenshots = prefs.getBoolean("screenshots", d.includeScreenshots),
            protectFavorites = prefs.getBoolean("favorites", d.protectFavorites),
            deleteMode = runCatching { DeleteMode.valueOf(prefs.getString("deleteMode", null)!!) }.getOrDefault(d.deleteMode),
        )
    }

    fun save(s: PhotoSettings) = prefs.edit()
        .putFloat("threshold", s.threshold)
        .putString("window", s.timeWindow.name)
        .putBoolean("exactEverywhere", s.exactEverywhere)
        .putBoolean("screenshots", s.includeScreenshots)
        .putBoolean("favorites", s.protectFavorites)
        .putString("deleteMode", s.deleteMode.name)
        .apply()
}

@RequiresApi(Build.VERSION_CODES.R)
class PhotoRepository(private val context: Context) {
    fun load(): List<Photo> {
        // L'ordine delle colonne è quello letto sotto con c.getX(indice).
        val projection = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.IS_FAVORITE, MediaStore.Images.Media.ORIENTATION,
        )
        val photos = ArrayList<Photo>()
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val bucket = c.getString(7).orEmpty()
                val path = c.getString(8).orEmpty()
                val rotated = c.getInt(10) % 180 != 0
                val w = c.getInt(4)
                val h = c.getInt(5)
                photos += Photo(
                    id = c.getLong(0),
                    dateTaken = c.getLong(1).takeIf { it > 0 } ?: (c.getLong(2) * 1000),
                    dateModified = c.getLong(3),
                    width = if (rotated) h else w,
                    height = if (rotated) w else h,
                    sizeBytes = c.getLong(6),
                    bucket = bucket,
                    isFavorite = c.getInt(9) == 1,
                    isScreenshot = bucket.contains("screenshot", true) || path.contains("screenshot", true),
                )
            }
        }
        return photos.sortedBy { it.dateTaken }
    }
}

/** Ricorda le impronte già calcolate: le scansioni successive analizzano solo le foto nuove o modificate. */
class FeatureCache(context: Context) : SQLiteOpenHelper(context, "photo_features.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) = db.execSQL(
        "CREATE TABLE features (id INTEGER PRIMARY KEY, date_modified INTEGER NOT NULL, size INTEGER NOT NULL, " +
            "phash INTEGER NOT NULL, dhash INTEGER NOT NULL, color BLOB NOT NULL, sharpness REAL NOT NULL, exposure REAL NOT NULL)",
    )

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS features")
        onCreate(db)
    }

    /** Impronte salvate, solo per le foto che non sono cambiate da allora. */
    fun loadValid(photos: List<Photo>): Map<Long, PhotoFeatures> {
        val byId = photos.associateBy { it.id }
        val result = HashMap<Long, PhotoFeatures>(photos.size)
        readableDatabase.rawQuery("SELECT id, date_modified, size, phash, dhash, color, sharpness, exposure FROM features", null).use { c ->
            while (c.moveToNext()) {
                val p = byId[c.getLong(0)] ?: continue
                if (p.dateModified != c.getLong(1) || p.sizeBytes != c.getLong(2)) continue
                result[p.id] = PhotoFeatures(c.getLong(3), c.getLong(4), c.getBlob(5), c.getFloat(6), c.getFloat(7))
            }
        }
        return result
    }

    fun putAll(entries: List<AnalyzedPhoto>) {
        if (entries.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues()
            for ((p, f) in entries) {
                v.clear()
                v.put("id", p.id); v.put("date_modified", p.dateModified); v.put("size", p.sizeBytes)
                v.put("phash", f.pHash); v.put("dhash", f.dHash); v.put("color", f.colorGrid)
                v.put("sharpness", f.sharpness); v.put("exposure", f.exposure)
                db.insertWithOnConflict("features", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun remove(ids: Collection<Long>) {
        if (ids.isNotEmpty()) writableDatabase.delete("features", "id IN (${ids.joinToString(",")})", null)
    }

    fun clear() { writableDatabase.delete("features", null, null) }
}
