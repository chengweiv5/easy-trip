package com.yangchengwei.easytrip.place.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.place.amap.AppLocationSession
import com.yangchengwei.easytrip.place.domain.LocatedPosition

/** Share the existing foreground fix; refresh permissions/cache when this destination resumes. */
@Composable
fun rememberLocatedPosition(session: AppLocationSession?): LocatedPosition? {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(session, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) session?.warmUp()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) session?.warmUp()
        onDispose { lifecycle.removeObserver(observer) }
    }
    return session?.position?.collectAsStateWithLifecycle()?.value
}
