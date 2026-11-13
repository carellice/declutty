package app.declutty.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppItem(
    val pkg: String,
    val label: String,
    val category: Category,
    /** Categoria scelta da Declutty, prima di eventuali spostamenti fatti a mano. */
    val autoCategory: Category,
    val size: Long,
    val installed: Long,
    val updated: Long,
    /** null senza accesso ai dati di utilizzo, 0 se non risulta mai usata. */
    val lastUsed: Long?,
    val version: String,
)

enum class AppSort(val label: String) { NAME("Nome"), SIZE("Dimensione"), INSTALLED("Data di installazione"), UPDATED("Ultimo aggiornamento"), LAST_USED("Ultimo utilizzo") }

fun List<AppItem>.sorted(sort: AppSort, descending: Boolean): List<AppItem> {
    val c = when (sort) {
        AppSort.NAME -> compareBy<AppItem, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
        AppSort.SIZE -> compareBy { it.size }
        AppSort.INSTALLED -> compareBy { it.installed }
        AppSort.UPDATED -> compareBy { it.updated }
        AppSort.LAST_USED -> compareBy { it.lastUsed ?: 0 }
    }
    return sortedWith(if (descending) c.reversed() else c)
}

/** Le app installate dall'utente: elenco, dimensioni, ultimo utilizzo e icone. */
class AppsRepository(private val context: Context) {
    private val pm = context.packageManager
    private val icons = LruCache<String, ImageBitmap>(160)

    suspend fun load(overrides: Map<String, Category>): List<AppItem> = withContext(Dispatchers.IO) {
        val usage = lastUsed()
        val stats = if (hasUsageAccess()) context.getSystemService(StorageStatsManager::class.java) else null
        val user: UserHandle = Process.myUserHandle()
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0).mapNotNull { info ->
            val ai = info.applicationInfo ?: return@mapNotNull null
            // Solo le app che l'utente può disinstallare: niente app di sistema né Declutty stessa.
            if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0 || info.packageName == context.packageName) return@mapNotNull null
            val label = ai.loadLabel(pm).toString()
            val size = stats?.let { s ->
                runCatching { s.queryStatsForPackage(StorageManager.UUID_DEFAULT, info.packageName, user) }.getOrNull()
                    ?.let { it.appBytes + it.dataBytes + it.cacheBytes }
            } ?: apkSize(ai)
            val auto = categorize(info.packageName, label, ai.category)
            AppItem(
                pkg = info.packageName, label = label,
                category = overrides[info.packageName] ?: auto, autoCategory = auto,
                size = size, installed = info.firstInstallTime, updated = info.lastUpdateTime,
                lastUsed = usage?.let { it[info.packageName] ?: 0L },
                version = info.versionName.orEmpty(),
            )
        }
    }

    private fun apkSize(ai: ApplicationInfo): Long =
        (listOfNotNull(ai.sourceDir) + ai.splitSourceDirs.orEmpty()).sumOf { File(it).length() }

    private fun lastUsed(): Map<String, Long>? {
        if (!hasUsageAccess()) return null
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        return usm.queryAndAggregateUsageStats(now - 2 * 365L * 24 * 60 * 60 * 1000, now)
            .mapValues { it.value.lastTimeUsed }.filterValues { it > 0 }
    }

    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun isInstalled(pkg: String): Boolean =
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess

    fun uninstallIntent(pkg: String): Intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))

    fun detailsIntent(pkg: String): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))

    fun launchIntent(pkg: String): Intent? = pm.getLaunchIntentForPackage(pkg)

    suspend fun icon(pkg: String): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { pm.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap() }.getOrNull()?.also { icons.put(pkg, it) }
        }
    }

    fun cachedIcon(pkg: String): ImageBitmap? = icons.get(pkg)
}
