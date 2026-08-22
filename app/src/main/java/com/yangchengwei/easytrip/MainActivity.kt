package com.yangchengwei.easytrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyTripTheme {
                AppNavigation()
            }
        }
    }
}
