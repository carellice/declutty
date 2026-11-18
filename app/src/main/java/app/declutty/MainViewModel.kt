package app.declutty

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.declutty.data.AppItem
import app.declutty.data.AppSort
import app.declutty.data.AppsRepository
import app.declutty.data.Category
import app.declutty.data.ContactItem
import app.declutty.data.ContactsRepository
import app.declutty.data.duplicateGroups
import app.declutty.update.UpdateInfo
import app.declutty.update.Updater
import kotlinx.coroutines.launch
import java.io.File

enum class CategorySort(val label: String) { DEFAULT("Predefinito"), NAME("Nome"), COUNT("Numero di app"), SIZE("Spazio occupato") }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Disinstallazione in serie: Android chiede conferma per ogni app, Declutty passa da una all'altra. */
data class UninstallRun(val queue: List<AppItem>, val index: Int = 0, val removed: List<AppItem> = emptyList()) {
    val current: AppItem? get() = queue.getOrNull(index)
    val finished: Boolean get() = index >= queue.size
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val apps = AppsRepository(app)
    val contactsRepo = ContactsRepository(app)
    private val prefs = app.getSharedPreferences("declutty", Context.MODE_PRIVATE)

    var allApps by mutableStateOf<List<AppItem>>(emptyList()); private set
    var appsLoading by mutableStateOf(true); private set
    var usageAccess by mutableStateOf(apps.hasUsageAccess()); private set

    var hidden by mutableStateOf(prefs.getStringSet("hidden", emptySet())!!.toSet()); private set
    var overrides by mutableStateOf(readOverrides()); private set

    var appSort by mutableStateOf(enumOr(prefs.getString("appSort", null), AppSort.NAME)); private set
    var appDesc by mutableStateOf(prefs.getBoolean("appDesc", false)); private set
    var categorySort by mutableStateOf(enumOr(prefs.getString("catSort", null), CategorySort.DEFAULT)); private set
    var contactsDesc by mutableStateOf(prefs.getBoolean("contactsDesc", false)); private set

    var contacts by mutableStateOf<List<ContactItem>?>(null); private set
    var duplicates by mutableStateOf<List<List<ContactItem>>>(emptyList()); private set
    var contactsLoading by mutableStateOf(false); private set

    var uninstall by mutableStateOf<UninstallRun?>(null); private set
    var update by mutableStateOf<UpdateState>(UpdateState.Idle); private set
    var message by mutableStateOf<String?>(null)

    /** Foto simili: serve Android 11 (cestino e conferma di eliminazione di MediaStore). */
    val photos: PhotoCleaner? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PhotoCleaner(app, viewModelScope) { message = it } else null

    /** App mostrate in Declutty: tutte tranne quelle nascoste. */
    val visibleApps: List<AppItem> get() = allApps.filter { it.pkg !in hidden }

    init {
        refreshApps()
        if (hasContactsPermission()) refreshContacts()
        checkUpdate(manual = false)
    }

    fun onResume() {
        val access = apps.hasUsageAccess()
        // Con l'accesso ai dati di utilizzo arrivano ultimo utilizzo e spazio reale: ricarica.
        if (access != usageAccess) { usageAccess = access; refreshApps() }
        if (contacts == null && hasContactsPermission()) refreshContacts()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) photos?.refreshCount()
    }

    fun refreshApps() = viewModelScope.launch {
        appsLoading = allApps.isEmpty()
        allApps = runCatching { apps.load(overrides) }.getOrElse { message = "Non riesco a leggere le app"; allApps }
        appsLoading = false
    }

    fun hasContactsPermission(): Boolean = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        .all { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED }

    fun refreshContacts() = viewModelScope.launch {
        contactsLoading = contacts == null
        runCatching { contactsRepo.load() }
            .onSuccess { list -> contacts = list; duplicates = duplicateGroups(list) }
            .onFailure { message = "Non riesco a leggere la rubrica" }
        contactsLoading = false
    }

    fun deleteContacts(ids: Set<Long>) = viewModelScope.launch {
        // Toglie subito i contatti dall'elenco: la lista si anima mentre la rubrica lavora.
        contacts = contacts?.filter { it.id !in ids }
        runCatching { contactsRepo.delete(ids) }
            .onSuccess { message = if (ids.size == 1) "Contatto eliminato" else "${ids.size} contatti eliminati" }
            .onFailure { message = "Eliminazione non riuscita" }
        refreshContacts()
    }

    fun mergeContacts(groups: List<Set<Long>>) = viewModelScope.launch {
        runCatching { groups.forEach { contactsRepo.merge(it) } }
            .onSuccess { message = if (groups.size == 1) "Contatti uniti" else "${groups.size} gruppi uniti" }
            .onFailure { message = "Unione non riuscita" }
        refreshContacts()
    }

    // ---------- App nascoste, categorie e ordinamento ----------

    fun hide(pkgs: Set<String>) = storeHidden(hidden + pkgs).also { message = if (pkgs.size == 1) "App nascosta" else "${pkgs.size} app nascoste" }
    fun unhide(pkg: String) = storeHidden(hidden - pkg)
    private fun storeHidden(value: Set<String>) { hidden = value; prefs.edit().putStringSet("hidden", value).apply() }

    fun moveTo(pkgs: Set<String>, category: Category) {
        val next = overrides.toMutableMap()
        allApps.filter { it.pkg in pkgs }.forEach { if (it.autoCategory == category) next.remove(it.pkg) else next[it.pkg] = category }
        overrides = next
        prefs.edit().putStringSet("overrides", next.map { "${it.key}=${it.value.name}" }.toSet()).apply()
        allApps = allApps.map { if (it.pkg in pkgs) it.copy(category = category) else it }
    }

    private fun readOverrides(): Map<String, Category> = prefs.getStringSet("overrides", emptySet())!!.mapNotNull { line ->
        val (pkg, cat) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        runCatching { pkg to Category.valueOf(cat) }.getOrNull()
    }.toMap()

    fun setAppSort(sort: AppSort, descending: Boolean) {
        appSort = sort; appDesc = descending
        prefs.edit().putString("appSort", sort.name).putBoolean("appDesc", descending).apply()
    }

    fun changeCategorySort(sort: CategorySort) { categorySort = sort; prefs.edit().putString("catSort", sort.name).apply() }
    fun toggleContactsOrder() { contactsDesc = !contactsDesc; prefs.edit().putBoolean("contactsDesc", contactsDesc).apply() }

    // ---------- Disinstallazione ----------

    fun startUninstall(items: List<AppItem>) { if (items.isNotEmpty()) uninstall = UninstallRun(items) }

    /** Chiamato quando la finestra di sistema si chiude: controlla se l'app c'è ancora e passa alla successiva. */
    fun onUninstallResult() {
        val run = uninstall ?: return
        val current = run.current ?: return
        val gone = !apps.isInstalled(current.pkg)
        uninstall = run.copy(index = run.index + 1, removed = if (gone) run.removed + current else run.removed)
        if (gone) allApps = allApps.filter { it.pkg != current.pkg }
    }

    fun stopUninstall() { uninstall = uninstall?.let { it.copy(index = it.queue.size) } }
    fun closeUninstall() { uninstall = null }

    // ---------- Aggiornamenti ----------

    fun checkUpdate(manual: Boolean) = viewModelScope.launch {
        if (update is UpdateState.Downloading) return@launch
        if (manual) update = UpdateState.Checking
        update = runCatching { Updater.latest() }.fold(
            onSuccess = { if (Updater.isNewer(it.version)) UpdateState.Available(it) else if (manual) UpdateState.UpToDate else UpdateState.Idle },
            onFailure = { if (manual) UpdateState.Failed(it.message ?: "Errore di rete") else UpdateState.Idle },
        )
    }

    fun downloadUpdate(info: UpdateInfo) = viewModelScope.launch {
        update = UpdateState.Downloading(info, 0f)
        update = runCatching { Updater.download(getApplication(), info) { p -> update = UpdateState.Downloading(info, p) } }
            .fold({ UpdateState.Ready(info, it) }, { UpdateState.Failed(it.message ?: "Download non riuscito") })
        (update as? UpdateState.Ready)?.let { Updater.install(getApplication(), it.file) }
    }

    fun installUpdate() { (update as? UpdateState.Ready)?.let { Updater.install(getApplication(), it.file) } }
}

private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T = runCatching { enumValueOf<T>(name!!) }.getOrDefault(default)
