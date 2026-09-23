package com.yangchengwei.easytrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yangchengwei.easytrip.core.ui.theme.EasyTripThemeHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themes = (application as EasyTripApplication).themePreferences
        setContent {
            EasyTripThemeHost(themes) {
                AppNavigation()
            }
        }
    }
}
