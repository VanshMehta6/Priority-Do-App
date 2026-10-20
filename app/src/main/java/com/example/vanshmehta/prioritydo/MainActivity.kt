package com.example.vanshmehta.prioritydo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vanshmehta.prioritydo.data.prefs.SettingsStore
import com.example.vanshmehta.prioritydo.data.prefs.ThemeMode
import com.example.vanshmehta.prioritydo.ui.navigation.AppNavigation
import com.example.vanshmehta.prioritydo.ui.theme.PriorityDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = remember { SettingsStore(applicationContext) }
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PriorityDoTheme(darkTheme = dark) {
                AppNavigation(settings = settings, themeMode = mode)
            }
        }
    }
}
