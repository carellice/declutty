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
import app.declutty.ui.theme.Skeu

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Skeu.on = getSharedPreferences("ui", MODE_PRIVATE).getBoolean("skeu", false)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        setContent { DeclutTheme { DeclutApp(viewModel()) } }
    }
}
