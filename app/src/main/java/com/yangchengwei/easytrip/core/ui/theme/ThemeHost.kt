package com.yangchengwei.easytrip.core.ui.theme

import android.app.Activity
import android.view.Window
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalThemePicker = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun EasyTripThemeHost(store: ThemePreferenceStore, content: @Composable () -> Unit) {
    val saved by store.theme.collectAsStateWithLifecycle()
    val model: ThemePickerViewModel = viewModel(key = "global-theme-picker", factory = ThemePickerViewModel.Factory(store))
    val picker by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(model) {
        model.applied.collect { Toast.makeText(context, "已切换为${it.displayName}", Toast.LENGTH_SHORT).show() }
    }
    EasyTripTheme(saved) {
        ThemeSystemBars((context as? Activity)?.window, saved)
        CompositionLocalProvider(LocalThemePicker provides model::open) {
            content()
            if (picker.visible) {
                Dialog(
                    onDismissRequest = model::dismiss,
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                    ),
                ) {
                    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
                    ThemeSystemBars(window, picker.preview)
                    ThemePickerScreen(picker, saved, model::select, model::apply, model::dismiss)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ThemeSystemBars(window: Window?, palette: ThemePalette) {
    SideEffect {
        if (window != null) {
            window.statusBarColor = palette.colors.background.toArgb()
            window.navigationBarColor = palette.colors.background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
}
