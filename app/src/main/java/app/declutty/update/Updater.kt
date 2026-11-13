package app.declutty.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import app.declutty.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val notes: String, val apkUrl: String, val size: Long, val pageUrl: String)

/** Aggiornamenti in-app: legge l'ultima release pubblica della repo e installa l'APK allegato. */
object Updater {
    private const val API = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

    private fun open(url: String, accept: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
        useCaches = false
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Declutty/${BuildConfig.VERSION_NAME} (Android)")
        setRequestProperty("Accept", accept)
    }

    suspend fun latest(): UpdateInfo = withContext(Dispatchers.IO) {
        val c = open(API, "application/vnd.github+json")
        val json = try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally { c.disconnect() }
        val tag = json.getString("tag_name")
        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk", true) }
            ?: throw IOException("La release $tag non contiene un APK")
        UpdateInfo(
            version = tag.removePrefix("v"),
            notes = json.optString("body").takeIf { it != "null" }.orEmpty().trim(),
            apkUrl = apk.getString("browser_download_url"),
            size = apk.optLong("size"),
            pageUrl = json.getString("html_url"),
        )
    }

    fun isNewer(remote: String, local: String = BuildConfig.VERSION_NAME): Boolean {
        val r = remote.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val l = local.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "declutty-${info.version}.apk")
        val c = open(info.apkUrl, "application/octet-stream")
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: info.size
            var read = 0L
            c.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
            if (total > 0 && read != total) throw IOException("Download incompleto")
        } finally { c.disconnect() }
        target
    }

    /** Restituisce false se Android deve prima concedere l'installazione da questa app. */
    fun install(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(settings) } catch (_: ActivityNotFoundException) { }
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
