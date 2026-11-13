package app.declutty.data

import android.content.ContentProviderOperation
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.AggregationExceptions
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.DisplayNameSources
import android.provider.ContactsContract.RawContacts
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

data class ContactItem(
    val id: Long,
    val name: String,
    val photo: String?,
    val phones: List<String>,
    val emails: List<String>,
    val starred: Boolean,
    /** Il nome mostrato viene da un numero o da un'email: il contatto non ha un nome vero. */
    val unnamed: Boolean,
)

/** Rubrica di Android: lettura, eliminazione e unione dei contatti. */
class ContactsRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val photos = LruCache<String, ImageBitmap>(120)

    suspend fun load(): List<ContactItem> = withContext(Dispatchers.IO) {
        val phones = HashMap<Long, MutableList<String>>()
        resolver.query(Phone.CONTENT_URI, arrayOf(Phone.CONTACT_ID, Phone.NUMBER), null, null, null)?.use { c ->
            while (c.moveToNext()) c.getString(1)?.let { phones.getOrPut(c.getLong(0)) { mutableListOf() }.add(it) }
        }
        val emails = HashMap<Long, MutableList<String>>()
        resolver.query(Email.CONTENT_URI, arrayOf(Email.CONTACT_ID, Email.ADDRESS), null, null, null)?.use { c ->
            while (c.moveToNext()) c.getString(1)?.let { emails.getOrPut(c.getLong(0)) { mutableListOf() }.add(it) }
        }
        val out = ArrayList<ContactItem>()
        val projection = arrayOf(Contacts._ID, Contacts.DISPLAY_NAME_PRIMARY, Contacts.PHOTO_THUMBNAIL_URI, Contacts.STARRED, Contacts.DISPLAY_NAME_SOURCE)
        resolver.query(Contacts.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val source = c.getInt(4)
                out += ContactItem(
                    id = id,
                    name = c.getString(1).orEmpty().ifBlank { "Senza nome" },
                    photo = c.getString(2),
                    // Lo stesso numero salvato in più account compare più volte: tienine uno.
                    phones = phones[id].orEmpty().distinctBy { phoneKey(it) ?: it },
                    emails = emails[id].orEmpty().distinctBy { it.lowercase() },
                    starred = c.getInt(3) == 1,
                    unnamed = source != DisplayNameSources.STRUCTURED_NAME && source != DisplayNameSources.NICKNAME,
                )
            }
        }
        out
    }

    /** Elimina i contatti da tutti gli account del telefono. Restituisce quanti contatti grezzi sono stati rimossi. */
    suspend fun delete(ids: Collection<Long>): Int = withContext(Dispatchers.IO) {
        ids.chunked(400).sumOf { chunk ->
            resolver.delete(RawContacts.CONTENT_URI, "${RawContacts.CONTACT_ID} IN (${chunk.joinToString(",")})", null)
        }
    }

    /** Unisce i contatti come fa la rubrica di Android: i contatti grezzi restano, ma vengono mostrati come uno solo. */
    suspend fun merge(ids: Collection<Long>) = withContext(Dispatchers.IO) {
        if (ids.size < 2) return@withContext
        val raw = ArrayList<Long>()
        resolver.query(
            RawContacts.CONTENT_URI, arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID} IN (${ids.joinToString(",")}) AND ${RawContacts.DELETED}=0", null, null,
        )?.use { c -> while (c.moveToNext()) raw += c.getLong(0) }
        if (raw.size < 2) return@withContext
        val ops = ArrayList<ContentProviderOperation>()
        for (i in raw.indices) for (j in i + 1 until raw.size) {
            ops += ContentProviderOperation.newUpdate(AggregationExceptions.CONTENT_URI)
                .withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
                .withValue(AggregationExceptions.RAW_CONTACT_ID1, raw[i])
                .withValue(AggregationExceptions.RAW_CONTACT_ID2, raw[j])
                .build()
        }
        ops.chunked(400).forEach { resolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(it)) }
    }

    suspend fun photo(uri: String): ImageBitmap? {
        photos.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { resolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }
                .getOrNull()?.also { photos.put(uri, it) }
        }
    }

    fun contactUri(id: Long): Uri = Uri.withAppendedPath(Contacts.CONTENT_URI, id.toString())
}

// ---------- Duplicati ----------

/** Ultime 9 cifre del numero: lo stesso numero con o senza prefisso internazionale combacia. */
fun phoneKey(number: String): String? = number.filter { it.isDigit() }.takeIf { it.length >= 6 }?.takeLast(9)

fun nameKey(name: String): String? =
    Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        .split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }.sorted().joinToString(" ")
        .takeIf { k -> k.any { it.isLetter() } }

/** Gruppi di contatti che sembrano la stessa persona: stesso nome, stesso numero o stessa email. */
fun duplicateGroups(contacts: List<ContactItem>): List<List<ContactItem>> {
    val parent = IntArray(contacts.size) { it }
    fun find(i: Int): Int { var x = i; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    val seen = HashMap<String, Int>()
    contacts.forEachIndexed { i, c ->
        val keys = buildList {
            if (!c.unnamed) nameKey(c.name)?.let { add("n:$it") }
            c.phones.mapNotNullTo(this) { p -> phoneKey(p)?.let { "p:$it" } }
            c.emails.mapTo(this) { "e:${it.trim().lowercase()}" }
        }
        keys.forEach { k -> seen[k]?.let { j -> parent[find(i)] = find(j) } ?: seen.put(k, i) }
    }
    return contacts.indices.groupBy(::find).values.filter { it.size > 1 }
        .map { g -> g.map { contacts[it] } }
        .sortedBy { it.first().name.lowercase() }
}
