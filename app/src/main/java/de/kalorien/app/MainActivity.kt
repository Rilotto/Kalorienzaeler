package de.kalorien.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.kalorien.app.core.ui.theme.KalorienTheme
import de.kalorien.app.navigation.KalorienAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KalorienTheme {
                KalorienAppRoot()
            }
        }
    }
}
