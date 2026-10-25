package io.github.rim95dev.island

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.rim95dev.island.ui.CameraCalibrationScreen
import io.github.rim95dev.island.ui.IslandSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val colorScheme =
                if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            var calibrating by rememberSaveable { mutableStateOf(false) }
            MaterialTheme(colorScheme = colorScheme) {
                if (calibrating) {
                    CameraCalibrationScreen(onDone = { calibrating = false })
                } else {
                    Scaffold { padding ->
                        IslandSettingsScreen(
                            onAdjustCamera = { calibrating = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
