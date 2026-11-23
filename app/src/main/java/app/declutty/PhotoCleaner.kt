package app.declutty

import android.Manifest
import android.app.Application
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import app.declutty.data.AnalyzedPhoto
import app.declutty.data.DeleteMode
import app.declutty.data.FeatureCache
import app.declutty.data.ImageAnalyzer
import app.declutty.data.Photo
import app.declutty.data.PhotoRepository
import app.declutty.data.PhotoSettings
import app.declutty.data.PhotoSettingsStore
import app.declutty.data.SimilarGroup
import app.declutty.data.groupSimilar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

data class ScanProgress(val done: Int = 0, val total: Int = 0, val current: Photo? = null, val grouping: Boolean = false)

data class PhotoDeleteSummary(val count: Int, val bytes: Long)

/** Foto simili: scansione della galleria, scelta della migliore per gruppo ed eliminazione delle altre. */
@RequiresApi(Build.VERSION_CODES.R)
class PhotoCleaner(private val app: Application, private val scope: CoroutineScope, private val toast: (String) -> Unit) {
    private val repo = PhotoRepository(app)
    private val cache = FeatureCache(app)
    private val store = PhotoSettingsStore(app)
    private val analyzer = ImageAnalyzer(app.contentResolver)

    var settings by mutableStateOf(store.load()); private set
    var galleryCount by mutableStateOf<Int?>(null); private set
    var lastScanAt by mutableLongStateOf(store.lastScanAt); private set
    var freedBytes by mutableLongStateOf(store.freedBytes); private set
    var progress by mutableStateOf<ScanProgress?>(null); private set
    /** null finché non è stata fatta una scansione in questa sessione. */
    var groups by mutableStateOf<List<SimilarGroup>?>(null); private set
    /** Id delle foto da eliminare. */
    var toDelete by mutableStateOf(emptySet<Long>()); private set
    var lastDelete by mutableStateOf<PhotoDeleteSummary?>(null)

    /** Scorri e scegli su tutta la galleria; quello che elimina sparisce anche dai gruppi. */
    val swiper = PhotoSwiper(app, scope, { settings.deleteMode }) { ids, bytes -> forget(ids, bytes) }

    private var analyzed: List<AnalyzedPhoto>? = null
    private var scanJob: Job? = null
    private var pendingDelete: List<Long> = emptyList()

    val toDeleteBytes: Long get() = groups.orEmpty().flatMap { it.members }.filter { it.photo.id in toDelete }.sumOf { it.photo.sizeBytes }

    fun hasAccess(): Boolean {
        fun granted(p: String) = ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED
        return granted(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE) ||
            Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }

    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun refreshCount() {
        if (!hasAccess()) return
        scope.launch { galleryCount = withContext(Dispatchers.IO) { runCatching { repo.load().size }.getOrNull() } }
    }

    /** Cambiare sensibilità dopo una scansione riraggruppa subito, senza rianalizzare. */
    fun updateSettings(transform: (PhotoSettings) -> PhotoSettings) {
        settings = transform(settings)
        store.save(settings)
        analyzed?.let { list -> scope.launch { regroup(list) } }
    }

    fun clearCache() {
        scope.launch(Dispatchers.IO) { cache.clear() }
        analyzed = null
        groups = null
        toDelete = emptySet()
        toast("Memoria dell'analisi svuotata")
    }

    fun startScan() {
        scanJob?.cancel()
        progress = ScanProgress()
        scanJob = scope.launch {
            try {
                val result = withContext(Dispatchers.Default) { scan() }
                analyzed = result
                galleryCount = result.size
                lastScanAt = System.currentTimeMillis().also { store.lastScanAt = it }
                regroup(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("Scansione non riuscita")
            } finally {
                progress = null
            }
        }
    }

    fun cancelScan() { scanJob?.cancel(); scanJob = null; progress = null }

    private suspend fun scan(): List<AnalyzedPhoto> = coroutineScope {
        val all = repo.load()
        val cached = cache.loadValid(all)
        val missing = all.filter { it.id !in cached }
        val done = AtomicInteger(cached.size)
        progress = ScanProgress(done.get(), all.size)
        val fresh = Collections.synchronizedList(ArrayList<AnalyzedPhoto>(missing.size))
        val unsaved = Collections.synchronizedList(ArrayList<AnalyzedPhoto>())
        val next = AtomicInteger(0)
        (0 until Runtime.getRuntime().availableProcessors().coerceIn(2, 6)).map {
            async(Dispatchers.IO) {
                while (true) {
                    ensureActive()
                    val photo = missing.getOrNull(next.getAndIncrement()) ?: break
                    analyzer.analyze(photo)?.let { f -> AnalyzedPhoto(photo, f).also { fresh += it; unsaved += it } }
                    val d = done.incrementAndGet()
                    if (d % 8 == 0 || d == all.size) progress = ScanProgress(d, all.size, photo)
                    if (unsaved.size >= 200) flush(unsaved)
                }
            }
        }.awaitAll()
        flush(unsaved)
        progress = ScanProgress(all.size, all.size, grouping = true)
        all.mapNotNull { p -> cached[p.id]?.let { AnalyzedPhoto(p, it) } } + fresh
    }

    private fun flush(list: MutableList<AnalyzedPhoto>) = cache.putAll(synchronized(list) { ArrayList(list).also { list.clear() } })

    private suspend fun regroup(items: List<AnalyzedPhoto>) {
        val s = settings
        val result = withContext(Dispatchers.Default) { groupSimilar(items, s) }
        groups = result
        toDelete = defaultSelection(result, s.protectFavorites)
    }

    private fun defaultSelection(groups: List<SimilarGroup>, protectFavorites: Boolean): Set<Long> = groups.flatMap { g ->
        g.members.filter { it.photo.id != g.bestId && !(protectFavorites && it.photo.isFavorite) }.map { it.photo.id }
    }.toSet()

    fun toggle(photoId: Long) {
        val group = groups.orEmpty().firstOrNull { g -> g.members.any { it.photo.id == photoId } } ?: return
        val set = toDelete.toMutableSet()
        if (!set.remove(photoId)) set += photoId
        // In ogni gruppo deve restare almeno una foto.
        if (group.members.all { it.photo.id in set }) set -= photoId
        toDelete = set
    }

    /** Tiene solo [photoId] nel suo gruppo: tutte le altre vanno eliminate. */
    fun keepOnly(groupId: Long, photoId: Long) {
        val list = groups ?: return
        groups = list.map { if (it.id == groupId) it.copy(bestId = photoId) else it }
        val set = toDelete.toMutableSet()
        list.first { it.id == groupId }.members.forEach { if (it.photo.id == photoId) set -= it.photo.id else set += it.photo.id }
        toDelete = set
    }

    fun selectAll(selected: Boolean) {
        toDelete = if (selected) defaultSelection(groups.orEmpty(), protectFavorites = false) else emptySet()
    }

    /** Richiesta di conferma di sistema per le foto selezionate: cestino o eliminazione definitiva. */
    fun deleteRequest(): IntentSender? {
        val members = groups.orEmpty().flatMap { it.members }.filter { it.photo.id in toDelete }
        if (members.isEmpty()) return null
        pendingDelete = members.map { it.photo.id }
        val uris = members.map { it.photo.uri }
        return when (settings.deleteMode) {
            DeleteMode.TRASH -> MediaStore.createTrashRequest(app.contentResolver, uris, true)
            DeleteMode.PERMANENT -> MediaStore.createDeleteRequest(app.contentResolver, uris)
        }.intentSender
    }

    fun onDeleteResult(confirmed: Boolean) {
        val ids = pendingDelete.toSet()
        pendingDelete = emptyList()
        if (!confirmed || ids.isEmpty()) return
        val bytes = groups.orEmpty().flatMap { it.members }.filter { it.photo.id in ids }.sumOf { it.photo.sizeBytes }
        forget(ids, bytes)
        lastDelete = PhotoDeleteSummary(ids.size, bytes)
    }

    /** Foto eliminate (da qui o dallo scorri e scegli): via da memoria, gruppi e conteggi. */
    private fun forget(ids: Set<Long>, bytes: Long) {
        val list = groups.orEmpty()
        freedBytes = (freedBytes + bytes).also { store.freedBytes = it }
        scope.launch(Dispatchers.IO) { cache.remove(ids) }
        analyzed = analyzed?.filter { it.photo.id !in ids }
        groups = list.mapNotNull { g ->
            val left = g.members.filter { it.photo.id !in ids }
            if (left.size < 2) null
            else g.copy(members = left, bestId = if (left.any { it.photo.id == g.bestId }) g.bestId else left.maxBy { it.score }.photo.id)
        }
        toDelete = toDelete - ids
        galleryCount = galleryCount?.minus(ids.size)
    }
}
