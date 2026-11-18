package app.declutty.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/** Calcola da una miniatura pHash, dHash, firma colore, nitidezza ed esposizione. */
@RequiresApi(Build.VERSION_CODES.R)
class ImageAnalyzer(private val resolver: ContentResolver) {

    fun analyze(photo: Photo): PhotoFeatures? {
        val thumb = runCatching { resolver.loadThumbnail(photo.uri, Size(THUMB, THUMB), null) }.getOrNull() ?: return null
        return try {
            val src = if (thumb.config == Bitmap.Config.ARGB_8888) thumb else thumb.copy(Bitmap.Config.ARGB_8888, false)
            val (sharpness, exposure) = quality(src)
            PhotoFeatures(pHash(scaled(src, 32, 32)), dHash(scaled(src, 9, 8)), colorGrid(scaled(src, 4, 4)), sharpness, exposure)
                .also { if (src !== thumb) src.recycle() }
        } finally {
            thumb.recycle()
        }
    }

    private fun scaled(bmp: Bitmap, w: Int, h: Int): IntArray {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(bmp, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)
        out.recycle()
        return px
    }

    private fun luma(c: Int): Double = 0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF)

    /** Hash percettivo da DCT: resiste a piccole differenze di luce, compressione e dimensione. */
    private fun pHash(px: IntArray): Long {
        val n = 32
        val g = DoubleArray(n * n) { luma(px[it]) }
        // DCT-II 2D separabile, tenendo solo il blocco 8x8 delle basse frequenze.
        val rows = DoubleArray(n * 8)
        for (y in 0 until n) for (u in 0 until 8) {
            var s = 0.0
            for (x in 0 until n) s += g[y * n + x] * COS[u * n + x]
            rows[y * 8 + u] = s
        }
        val dct = DoubleArray(64)
        for (v in 0 until 8) for (u in 0 until 8) {
            var s = 0.0
            for (y in 0 until n) s += rows[y * 8 + u] * COS[v * n + y]
            dct[v * 8 + u] = s
        }
        val median = dct.copyOfRange(1, 64).sorted().let { (it[30] + it[31]) / 2 }
        var hash = 0L
        for (i in 0 until 64) if (dct[i] > median) hash = hash or (1L shl i)
        return hash
    }

    /** Hash dei gradienti: descrive struttura e composizione. */
    private fun dHash(px: IntArray): Long {
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) for (x in 0 until 8) {
            if (luma(px[y * 9 + x]) > luma(px[y * 9 + x + 1])) hash = hash or (1L shl bit)
            bit++
        }
        return hash
    }

    private fun colorGrid(px: IntArray) = ByteArray(48).also { out ->
        for (i in 0 until 16) {
            out[i * 3] = ((px[i] shr 16) and 0xFF).toByte()
            out[i * 3 + 1] = ((px[i] shr 8) and 0xFF).toByte()
            out[i * 3 + 2] = (px[i] and 0xFF).toByte()
        }
    }

    /** (varianza del Laplaciano, esposizione 0..1) su una dimensione fissa, così le foto sono confrontabili. */
    private fun quality(bmp: Bitmap): Pair<Float, Float> {
        val scale = WORK.toFloat() / maxOf(bmp.width, bmp.height).coerceAtLeast(1)
        val w = (bmp.width * scale).toInt().coerceAtLeast(3)
        val h = (bmp.height * scale).toInt().coerceAtLeast(3)
        val px = scaled(bmp, w, h)
        val g = DoubleArray(w * h) { luma(px[it]) }
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val lap = 4 * g[i] - g[i - 1] - g[i + 1] - g[i - w] - g[i + w]
            sum += lap; sumSq += lap * lap; count++
        }
        val mean = sum / count
        val variance = (sumSq / count - mean * mean).toFloat()
        var bright = 0.0
        var clipped = 0
        for (v in g) { bright += v; if (v < 12 || v > 245) clipped++ }
        val balance = 1.0 - abs(bright / g.size - 120.0) / 120.0
        val exposure = (0.6 * balance + 0.4 * (1.0 - (clipped * 3.0 / g.size).coerceAtMost(1.0))).coerceIn(0.0, 1.0).toFloat()
        return variance to exposure
    }

    private companion object {
        const val THUMB = 512
        const val WORK = 384
        val COS = DoubleArray(8 * 32).also { t ->
            for (u in 0 until 8) for (x in 0 until 32) t[u * 32 + x] = cos((2 * x + 1) * u * Math.PI / 64)
        }
    }
}

/** Somiglianza visiva tra due foto, 0..1. */
fun similarity(a: PhotoFeatures, b: PhotoFeatures): Float {
    val p = 1f - java.lang.Long.bitCount(a.pHash xor b.pHash) / 64f
    val d = 1f - java.lang.Long.bitCount(a.dHash xor b.dHash) / 64f
    var sq = 0.0
    for (i in 0 until 48) {
        val delta = (a.colorGrid[i].toInt() and 0xFF) - (b.colorGrid[i].toInt() and 0xFF)
        sq += delta * delta
    }
    // Differenza colore RMS: 0 = identica, oltre 60 = tavolozza molto diversa.
    val c = (1.0 - sqrt(sq / 48) / 64.0).coerceIn(0.0, 1.0).toFloat()
    return 0.5f * p + 0.3f * d + 0.2f * c
}

/** Raggruppa le foto simili scattate vicine nel tempo e sceglie la migliore di ogni gruppo. */
fun groupSimilar(photos: List<AnalyzedPhoto>, settings: PhotoSettings): List<SimilarGroup> {
    val items = photos.filter { settings.includeScreenshots || !it.photo.isScreenshot }.sortedBy { it.photo.dateTaken }
    if (items.size < 2) return emptyList()
    val uf = UnionFind(items.size)
    val threshold = settings.threshold
    // Sotto questa distanza di pHash due foto non possono raggiungere la soglia: salta il confronto completo.
    val maxHamming = ((1f - (2f * threshold - 1f).coerceAtLeast(0f)) * 64).toInt()
    val window = settings.timeWindow.millis

    // 1. Foto simili nello stesso intervallo di tempo.
    for (i in items.indices) {
        val a = items[i]
        for (j in i + 1 until items.size) {
            val b = items[j]
            if (b.photo.dateTaken - a.photo.dateTaken > window) break
            if (java.lang.Long.bitCount(a.features.pHash xor b.features.pHash) <= maxHamming && similarity(a.features, b.features) >= threshold) uf.union(i, j)
        }
    }
    // 2. Copie identiche ovunque nel rullino (es. immagini salvate due volte dalle chat).
    if (settings.exactEverywhere) {
        val byHash = HashMap<Pair<Long, Long>, Int>()
        items.forEachIndexed { i, it ->
            val first = byHash.putIfAbsent(it.features.pHash to it.features.dHash, i)
            if (first != null && similarity(items[first].features, it.features) >= 0.985f) uf.union(first, i)
        }
    }
    return items.indices.groupBy { uf.find(it) }.values.filter { it.size > 1 }
        .mapNotNull { idx -> buildGroup(idx.map { items[it] }, settings) }
        .sortedByDescending { it.best.photo.dateTaken }
}

private fun buildGroup(members: List<AnalyzedPhoto>, settings: PhotoSettings): SimilarGroup? {
    val logSharp = members.map { ln(1.0 + it.features.sharpness) }
    val maxSharp = logSharp.max().coerceAtLeast(1e-6)
    val maxMp = members.maxOf { it.photo.megapixels }.coerceAtLeast(1e-6)
    val maxExposure = members.maxOf { it.features.exposure }
    data class Scored(val m: AnalyzedPhoto, val raw: Float, val sharp: Float, val res: Float)
    val scored = members.mapIndexed { i, m ->
        val sharp = (logSharp[i] / maxSharp).toFloat()
        val res = (m.photo.megapixels / maxMp).toFloat()
        Scored(m, 0.55f * sharp + 0.25f * m.features.exposure + 0.20f * res, sharp, res)
    }
    val exact = members.all { it.features.pHash == members[0].features.pHash && it.features.dHash == members[0].features.dHash }
    val favorites = scored.filter { it.m.photo.isFavorite }
    val pool = if (settings.protectFavorites && favorites.isNotEmpty()) favorites else scored
    val best = if (exact) {
        // Copie identiche: tieni la più grande, a parità la più vecchia (l'originale).
        pool.maxWith(compareBy<Scored>({ it.m.photo.megapixels }, { it.m.photo.sizeBytes }).thenByDescending { it.m.photo.dateTaken })
    } else {
        pool.maxWith(compareBy({ it.raw }, { it.m.photo.megapixels }, { it.m.photo.sizeBytes }))
    }.m

    // Scarta le foto entrate solo per una catena di scatti intermedi ma diverse da quella da tenere.
    val result = scored.mapNotNull { s ->
        val sim = if (s.m === best) 1f else similarity(s.m.features, best.features)
        if (sim < settings.threshold - 0.05f) return@mapNotNull null
        val reasons = buildList {
            if (s.m.photo.isFavorite) add("Preferita")
            if (s.sharp >= 0.999f) add("La più nitida")
            if (s.m.features.exposure >= maxExposure - 1e-4f) add("Luce migliore")
            if (s.res >= 0.999f) add("Risoluzione massima")
        }
        GroupMember(s.m.photo, (s.raw * 100).toInt().coerceIn(0, 100), sim, reasons)
    }
    if (result.size < 2) return null
    return SimilarGroup(
        id = best.photo.id,
        members = result.sortedWith(compareByDescending<GroupMember> { it.photo.id == best.photo.id }.thenByDescending { it.score }),
        bestId = best.photo.id,
        isExactDuplicate = exact,
    )
}

private class UnionFind(n: Int) {
    private val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) r = parent[r]
        var c = x
        while (parent[c] != r) { val next = parent[c]; parent[c] = r; c = next }
        return r
    }
    fun union(a: Int, b: Int) {
        val ra = find(a); val rb = find(b)
        if (ra != rb) parent[maxOf(ra, rb)] = minOf(ra, rb)
    }
}
