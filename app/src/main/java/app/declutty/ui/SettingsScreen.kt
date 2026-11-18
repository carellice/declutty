package app.declutty.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.declutty.BuildConfig
import app.declutty.MainViewModel
import app.declutty.R
import app.declutty.UpdateState
import app.declutty.ui.theme.DF

@Composable
fun SettingsScreen(vm: MainViewModel, onHidden: () -> Unit, onPhotos: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(intent: Intent) = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            Column(Modifier.padding(top = 12.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", Modifier.padding(start = 20.dp, bottom = 12.dp), onClick = onBack)
                ScreenHeader("Impostazioni", "Declutty ${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(20.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).reveal(0), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(DF.SurfaceHigh))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Declutty", style = MaterialTheme.typography.headlineMedium, color = DF.Text)
                    Text("Decluttering digitale per Android", style = MaterialTheme.typography.bodyMedium, color = DF.Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        item { UpdateCard(vm, Modifier.reveal(1)) }
        item {
            SettingRow(
                Icons.Rounded.Insights, "Accesso ai dati di utilizzo",
                if (vm.usageAccess) "Attivo: ultimo utilizzo e spazio reale delle app" else "Spento: tocca per attivarlo", 2,
            ) { open(vm.apps.usageAccessIntent()) }
        }
        if (vm.photos != null) item { SettingRow(Icons.Rounded.PhotoLibrary, "Foto simili", "Somiglianza, periodo ed eliminazione", 3, onPhotos) }
        item { SettingRow(Icons.Rounded.VisibilityOff, "App nascoste", if (vm.hidden.isEmpty()) "Nessuna" else plural(vm.hidden.size, "app nascosta", "app nascoste"), 4, onHidden) }
        item {
            SettingRow(Icons.Rounded.Code, "Codice sorgente", "github.com/${BuildConfig.GITHUB_REPO}", 5) {
                open(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${BuildConfig.GITHUB_REPO}")))
            }
        }
    }
}

@Composable
private fun UpdateCard(vm: MainViewModel, modifier: Modifier) {
    val state = vm.update
    CardSurface(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = DF.Primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Aggiornamenti", style = MaterialTheme.typography.titleMedium, color = DF.Text)
                    AnimatedContent(state, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, contentKey = { it::class }, label = "upd") { s ->
                        Text(
                            when (s) {
                                UpdateState.Idle -> "Versione ${BuildConfig.VERSION_NAME}"
                                UpdateState.Checking -> "Controllo in corso…"
                                UpdateState.UpToDate -> "Hai già l'ultima versione"
                                is UpdateState.Available -> "Nuova versione ${s.info.version} · ${formatSize(s.info.size)}"
                                is UpdateState.Downloading -> "Scarico ${s.info.version}…"
                                is UpdateState.Ready -> "Pronta da installare"
                                is UpdateState.Failed -> "Errore: ${s.message}"
                            },
                            style = MaterialTheme.typography.bodySmall, color = if (s is UpdateState.Failed) DF.Error else DF.Muted,
                        )
                    }
                }
            }
            (state as? UpdateState.Available)?.info?.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = DF.Muted, maxLines = 8, overflow = TextOverflow.Ellipsis)
            }
            if (state is UpdateState.Downloading) {
                val p by animateFloatAsState(state.progress, label = "dl")
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(DF.SurfaceHigher)) {
                    Box(Modifier.fillMaxWidth(p).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DF.Primary))
                }
            }
            Spacer(Modifier.height(14.dp))
            when (state) {
                is UpdateState.Available -> MintButton("Scarica e installa", Icons.Rounded.SystemUpdate, Modifier.fillMaxWidth()) { vm.downloadUpdate(state.info) }
                is UpdateState.Ready -> MintButton("Installa", Icons.Rounded.SystemUpdate, Modifier.fillMaxWidth()) { vm.installUpdate() }
                is UpdateState.Downloading, UpdateState.Checking -> Unit
                else -> MintButton("Controlla aggiornamenti", null, Modifier.fillMaxWidth(), filled = false) { vm.checkUpdate(manual = true) }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, index: Int, onClick: () -> Unit) {
    Row(
        Modifier.reveal(index).fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).pressScale(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(DF.Surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = DF.Primary, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = DF.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DF.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = DF.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun HiddenScreen(vm: MainViewModel, onBack: () -> Unit) {
    val hidden = vm.allApps.filter { it.pkg in vm.hidden }.sortedBy { it.label.lowercase() }
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
        item {
            Column(Modifier.padding(top = 12.dp)) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", Modifier.padding(start = 20.dp, bottom = 12.dp), onClick = onBack)
                ScreenHeader("App nascoste", "Non compaiono in Declutty, ma sono installate")
                Spacer(Modifier.height(12.dp))
            }
        }
        if (hidden.isEmpty()) item { EmptyState("Nessuna app nascosta", "Tieni premuta un'app e scegli \"Nascondi da Declutty\".") }
        itemsIndexed(hidden, key = { _, a -> a.pkg }) { i, app ->
            Row(
                Modifier.animateItem().reveal(i).fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncBitmap(app.pkg, vm.apps.cachedIcon(app.pkg), 44.dp, fallback = app.label) { vm.apps.icon(app.pkg) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium, color = DF.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.category.title, style = MaterialTheme.typography.bodySmall, color = DF.Muted)
                }
                MintButton("Mostra", Icons.Rounded.Visibility, filled = false) { vm.unhide(app.pkg) }
            }
        }
    }
}
