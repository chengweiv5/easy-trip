package com.yangchengwei.easytrip.place.ui

import android.content.Context
import androidx.activity.compose.BackHandler

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.RealAmapMapHost

@Composable
fun PlaceSearchRoute(
    viewModel: PlaceSearchViewModel,
    detailContent: (@Composable (com.yangchengwei.easytrip.place.amap.PlaceCandidate) -> Unit)? = null,
    consent: AmapConsentToken? = null,
    mapHostFactory: (Context) -> AmapMapHost = ::RealAmapMapHost,
    onBack: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val resultsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnBack = rememberUpdatedState(onBack)

    BackHandler { viewModel.dispatch(PlaceSearchAction.Back) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    PlaceSearchEffect.ExitDestination -> currentOnBack.value()
                }
            }
        }
    }

    PlaceSearchContent(
        state = state,
        onAction = viewModel::dispatch,
        resultsListState = resultsListState,
        detailContent = detailContent,
        consent = consent,
        mapHostFactory = mapHostFactory,
    )
    state.pendingCollectionRemoval?.let { pending ->
        val busy = pending.candidate.poiId in state.collectionBusyPoiIds
        AlertDialog(
            onDismissRequest = {
                if (!busy) viewModel.dispatch(PlaceSearchAction.DismissRemovalConfirmation)
            },
            title = { Text("取消收藏 ${pending.place.name}？") },
            text = { Text("将同时删除 ${pending.impact.itineraryItemCount} 次行程安排和 ${pending.impact.routeLegCount} 段路线。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dispatch(PlaceSearchAction.ConfirmRemoval) },
                    enabled = !busy,
                ) {
                    Text("确认取消收藏")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dispatch(PlaceSearchAction.DismissRemovalConfirmation) },
                    enabled = !busy,
                ) {
                    Text("取消")
                }
            },
        )
    }
}
