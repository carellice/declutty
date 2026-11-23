package app.declutty

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.declutty.data.DeleteMode
import app.declutty.data.Photo
import app.declutty.data.PhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Scorri e scegli: tutta la galleria, una foto alla volta, dalla più recente.
 * Le foto tenute non si ripropongono; quelle scartate restano in sospeso finché non le elimini o le rivedi.
 */
@RequiresApi(Build.VERSION_CODES.R)
class PhotoSwiper(
    private val app: Application,
    private val scope: CoroutineScope,
    private val deleteMode: () -> DeleteMode,
    private val onDeleted: (ids: Set<Long>, bytes: Long) -> Unit,
) {
    private val repo = PhotoRepository(app)
    private val prefs = app.getSharedPreferences("swipe", Context.MODE_PRIVATE)
    private var kept = prefs.getStringSet("kept", null).orEmpty().mapNotNull { it.toLongOrNull() }.toMutableSet()

    /** null mentre la galleria si carica. */
    var queue by mutableStateOf<List<Photo>?>(null); private set
    var index by mutableIntStateOf(0); private set
    var pending by mutableStateOf(emptyList<Photo>()); private set
    /** Mostra il riepilogo anche se restano foto da scorrere. */
    var summary by mutableStateOf(false)
    private val history = ArrayList<Pair<Photo, Boolean>>()
    private var deleting: List<Photo> = emptyList()

    val current: Photo? get() = queue?.getOrNull(index)
    val next: Photo? get() = queue?.getOrNull(index + 1)
    val left: Int get() = (queue?.size ?: 0) - index
    val pendingBytes: Long get() = pending.sumOf { it.sizeBytes }
    val canUndo: Boolean get() = history.isNotEmpty() && index > 0

    fun load() {
        scope.launch {
            val all = withContext(Dispatchers.IO) { runCatching { repo.load() }.getOrDefault(emptyList()) }
            val ids = all.mapTo(HashSet()) { it.id }
            // Le foto eliminate da fuori non restano nelle liste salvate.
            kept.retainAll(ids)
            val saved = prefs.getStringSet("pending", null).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
            pending = all.filter { it.id in saved }
            queue = all.filter { it.id !in kept && it.id !in saved }.sortedByDescending { it.dateTaken }
            index = 0
            history.clear()
            summary = false
            save()
        }
    }

    fun keep() = decide(false)
    fun discard() = decide(true)

    private fun decide(delete: Boolean) {
        val photo = current ?: return
        if (delete) pending = pending + photo else kept += photo.id
        history += photo to delete
        index++
        save()
    }

    fun undo() {
        if (!canUndo) return
        val (photo, deleted) = history.removeAt(history.lastIndex)
        if (deleted) pending = pending - photo else kept -= photo.id
        index--
        save()
    }

    /** Toglie una foto dalle sospese: la si tiene. */
    fun rescue(photo: Photo) {
        pending = pending - photo
        kept += photo.id
        save()
    }

    /** Le sospese tornano in cima alla coda, da riscorrere una per una. */
    fun review() {
        queue = pending + queue.orEmpty().drop(index)
        pending = emptyList()
        index = 0
        history.clear()
        summary = false
        save()
    }

    /** Ricomincia da capo: anche le foto già tenute si ripropongono. */
    fun restart() {
        kept.clear()
        prefs.edit().remove("kept").apply()
        load()
    }

    fun deleteRequest(): IntentSender? {
        if (pending.isEmpty()) return null
        deleting = pending
        val uris = pending.map { it.uri }
        return when (deleteMode()) {
            DeleteMode.TRASH -> MediaStore.createTrashRequest(app.contentResolver, uris, true)
            DeleteMode.PERMANENT -> MediaStore.createDeleteRequest(app.contentResolver, uris)
        }.intentSender
    }

    fun onDeleteResult(confirmed: Boolean) {
        val gone = deleting
        deleting = emptyList()
        if (!confirmed || gone.isEmpty()) return
        val ids = gone.mapTo(HashSet()) { it.id }
        pending = pending.filter { it.id !in ids }
        save()
        // Restano foto da scorrere: si riprende da lì.
        if (left > 0) summary = false
        onDeleted(ids, gone.sumOf { it.sizeBytes })
    }

    private fun save() = prefs.edit()
        .putStringSet("kept", kept.mapTo(HashSet()) { it.toString() })
        .putStringSet("pending", pending.mapTo(HashSet()) { it.id.toString() })
        .apply()
}
