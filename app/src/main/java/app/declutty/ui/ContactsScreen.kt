package app.declutty.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.declutty.MainViewModel
import app.declutty.data.ContactItem
import app.declutty.ui.theme.DF
import app.declutty.ui.theme.Skeu
import app.declutty.ui.theme.Metro

private enum class ContactFilter(val label: String) { ALL("Tutti"), DUPLICATES("Duplicati"), NO_NUMBER("Senza numero"), NO_NAME("Senza nome") }

@Composable
fun ContactsScreen(vm: MainViewModel) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) vm.refreshContacts() else vm.message = "Senza permesso non posso leggere la rubrica"
    }
    val contacts = vm.contacts
    if (contacts == null && !vm.contactsLoading) {
        PermissionIntro { permission.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) }
        return
    }
    ContactsList(vm, contacts.orEmpty())
}

@Composable
private fun PermissionIntro(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = TabBarSpace),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.reveal(0).size(110.dp).clip(CircleShape).background(DF.Primary.copy(alpha = 0.12f)).border(1.dp, DF.LineStrong, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.People, null, tint = DF.Primary, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Fai ordine in rubrica", Modifier.reveal(1), style = MaterialTheme.typography.displaySmall, color = DF.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Trova i doppioni, unisci chi è la stessa persona ed elimina in un colpo i contatti che non ti servono più. I contatti non lasciano mai il telefono.",
            Modifier.reveal(2), style = MaterialTheme.typography.bodyLarge, color = DF.Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        MintButton("Consenti accesso ai contatti", Icons.Rounded.ContactPhone, Modifier.reveal(3), onClick = onGrant)
    }
}

@Composable
private fun ContactsList(vm: MainViewModel, contacts: List<ContactItem>) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ContactFilter.ALL) }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmMerge by remember { mutableStateOf<List<Set<Long>>?>(null) }

    val q = query.trim()
    fun ContactItem.matches() = q.isEmpty() || name.contains(q, true) || phones.any { p -> p.filter(Char::isDigit).contains(q.filter(Char::isDigit).ifEmpty { "\u0000" }) } || emails.any { it.contains(q, true) }
    val shown = contacts.filter { it.matches() }.filter {
        when (filter) {
            ContactFilter.NO_NUMBER -> it.phones.isEmpty()
            ContactFilter.NO_NAME -> it.unnamed
            else -> true
        }
    }.sortedWith(compareBy<ContactItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.let { if (vm.contactsDesc) it.reversed() else it })
    val groups = vm.duplicates.filter { g -> g.any { it.matches() } }
    val live = selected.filter { id -> contacts.any { it.id == id } }.toSet()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = TabBarSpace + 120.dp)) {
            item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    ScreenHeader(
                        "Contatti",
                        if (live.isNotEmpty()) plural(live.size, "selezionato", "selezionati")
                        else "${plural(contacts.size, "contatto", "contatti")} · ${plural(vm.duplicates.size, "gruppo di doppioni", "gruppi di doppioni")}",
                        trailing = {
                            RoundIconButton(if (vm.contactsDesc) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, "Ordine", tint = DF.Primary) { vm.toggleContactsOrder() }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    SearchField(query, { query = it }, "Cerca nome, numero o email")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ContactFilter.entries.toList()) { f ->
                            val count = when (f) {
                                ContactFilter.ALL -> contacts.size
                                ContactFilter.DUPLICATES -> vm.duplicates.size
                                ContactFilter.NO_NUMBER -> contacts.count { it.phones.isEmpty() }
                                ContactFilter.NO_NAME -> contacts.count { it.unnamed }
                            }
                            DfChip("${f.label} · $count", filter == f) { filter = f; selected = emptySet() }
                        }
                        if (filter != ContactFilter.DUPLICATES) item {
                            val all = shown.isNotEmpty() && shown.all { it.id in live }
                            DfChip(if (all) "Deseleziona" else "Seleziona tutti", false, icon = Icons.Rounded.DoneAll) {
                                selected = if (all) live - shown.map { it.id }.toSet() else live + shown.map { it.id }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            when {
                vm.contactsLoading -> items(8) { LoadingRow() }
                filter == ContactFilter.DUPLICATES && groups.isEmpty() -> item { EmptyState("Nessun doppione", "La tua rubrica è già in ordine.") }
                filter == ContactFilter.DUPLICATES -> {
                    item {
                        MintButton(
                            "Unisci tutti (${plural(groups.size, "gruppo", "gruppi")})", Icons.Rounded.CallMerge,
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        ) { confirmMerge = groups.map { g -> g.map { it.id }.toSet() } }
                    }
                    itemsIndexed(groups, key = { _, g -> g.first().id }) { i, g ->
                        DuplicateCard(vm, g, Modifier.animateItem().reveal(i)) { confirmMerge = listOf(g.map { it.id }.toSet()) }
                    }
                }
                shown.isEmpty() -> item { EmptyState("Nessun contatto", "Niente da sistemare qui.") }
                else -> itemsIndexed(shown, key = { _, c -> c.id }) { i, c ->
                    ContactRow(
                        vm, c, c.id in live, i, Modifier.animateItem(),
                        onLongClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, vm.contactsRepo.contactUri(c.id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                    ) { selected = if (c.id in live) live - c.id else live + c.id }
                }
            }
        }

        SelectionBar(
            visible = live.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TabBarSpace),
            label = "${live.size}",
            onClear = { selected = emptySet() },
        ) {
            if (live.size >= 2) BarAction(Icons.Rounded.CallMerge, "Unisci") { confirmMerge = listOf(live) }
            MintButton("Elimina", Icons.Rounded.DeleteSweep, danger = true) { confirmDelete = true }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Eliminare ${plural(live.size, "contatto", "contatti")}?",
        "Vengono rimossi da tutti gli account del telefono, anche da Google se la rubrica è sincronizzata. Non si può annullare.",
        "Elimina", danger = true, onDismiss = { confirmDelete = false },
    ) { vm.deleteContacts(live); selected = emptySet() }

    confirmMerge?.let { merge ->
        val names = merge.flatten().mapNotNull { id -> contacts.firstOrNull { it.id == id }?.name }.distinct()
        ConfirmDialog(
            if (merge.size == 1) "Unire ${plural(merge.first().size, "contatto", "contatti")}?" else "Unire ${merge.size} gruppi?",
            if (merge.size == 1) "${names.take(4).joinToString(", ")}${if (names.size > 4) "…" else ""} diventeranno un solo contatto con tutti i numeri e le email. Puoi separarli di nuovo dalla rubrica di Android."
            else "Ogni gruppo di doppioni diventerà un solo contatto con tutti i numeri e le email. Puoi separarli di nuovo dalla rubrica di Android.",
            "Unisci", onDismiss = { confirmMerge = null },
        ) { vm.mergeContacts(merge); selected = emptySet() }
    }
}

@Composable
private fun ContactAvatar(vm: MainViewModel, c: ContactItem, size: Dp) {
    val photo = c.photo
    if (photo != null) AsyncBitmap(photo, null, size, corner = size / 2, fallback = c.name) { vm.contactsRepo.photo(photo) }
    else Box(Modifier.size(size).clip(CircleShape)) { Monogram(c.name, size) }
}

@Composable
private fun ContactRow(vm: MainViewModel, c: ContactItem, selected: Boolean, index: Int, modifier: Modifier, onLongClick: () -> Unit, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) DF.Primary.copy(alpha = 0.12f) else Color.Transparent, tween(240), label = "bg")
    val border by animateColorAsState(if (selected) DF.Primary.copy(alpha = 0.4f) else Color.Transparent, tween(240), label = "border")
    val shape = rounded(22.dp)
    Row(
        modifier.reveal(index).fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .pressScale(0.97f, onLongClick, onClick)
            .then(if (Skeu.on && selected) Modifier.leatherPanel(shape, DF.Primary.copy(alpha = 0.5f)) else if (Metro.on) Modifier.background(bg) else Modifier.clip(shape).background(bg).border(1.dp, border, shape))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(vm, c, 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, Modifier.weight(1f, false), style = MaterialTheme.typography.titleMedium, color = DF.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c.starred) Icon(Icons.Rounded.Star, "Preferito", tint = DF.Primary, modifier = Modifier.padding(start = 6.dp).size(15.dp))
            }
            Text(contactDetail(c), style = MaterialTheme.typography.bodySmall, color = DF.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        SelectCheck(selected)
    }
}

private fun contactDetail(c: ContactItem): String = when {
    c.phones.isNotEmpty() -> c.phones.first() + if (c.phones.size > 1) " · +${c.phones.size - 1}" else ""
    c.emails.isNotEmpty() -> c.emails.first()
    else -> "Nessun numero né email"
}

@Composable
private fun DuplicateCard(vm: MainViewModel, group: List<ContactItem>, modifier: Modifier, onMerge: () -> Unit) {
    CardSurface(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    group.take(3).forEachIndexed { i, c ->
                        Box(Modifier.offset(x = (22 * i).dp).border(2.dp, DF.SurfaceHigh, CircleShape)) { ContactAvatar(vm, c, 40.dp) }
                    }
                }
                Spacer(Modifier.width((22 * (group.size.coerceAtMost(3) - 1) + 14).dp))
                Column(Modifier.weight(1f)) {
                    Text(group.first().name, style = MaterialTheme.typography.titleMedium, color = DF.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(plural(group.size, "contatto simile", "contatti simili"), style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                }
                MintButton("Unisci", Icons.Rounded.CallMerge, onClick = onMerge)
            }
            Spacer(Modifier.height(12.dp))
            group.forEach { c ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (c.phones.isEmpty()) Icons.Rounded.PhoneDisabled else if (c.unnamed) Icons.Rounded.PersonOff else Icons.Rounded.People, null, tint = DF.Muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${c.name} · ${contactDetail(c)}", style = MaterialTheme.typography.bodySmall, color = DF.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
