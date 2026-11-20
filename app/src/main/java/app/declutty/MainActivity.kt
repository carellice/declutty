package app.declutty

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.declutty.ui.DeclutApp
import app.declutty.ui.theme.DeclutTheme
import app.declutty.ui.theme.Ui
import app.declutty.ui.theme.UiStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        // "skeu" è la chiave della 1.2.0: chi aveva scelto la pelle la ritrova.
        Ui.style = prefs.getString("style", null)?.let { s -> UiStyle.entries.firstOrNull { it.name == s } }
            ?: if (prefs.getBoolean("skeu", false)) UiStyle.SKEU else UiStyle.MODERN
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        setContent { DeclutTheme { DeclutApp(viewModel()) } }
    }
}
