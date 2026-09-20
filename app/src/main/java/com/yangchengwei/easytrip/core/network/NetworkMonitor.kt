package com.yangchengwei.easytrip.core.network
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job

internal fun hasInternetAccess(capabilities: NetworkCapabilities?): Boolean = canAttemptInternet(
    hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
    captivePortal = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
)

interface NetworkMonitor { val isOnline:StateFlow<Boolean> }

class ConnectivityNetworkMonitor(context: Context, scope: CoroutineScope) : NetworkMonitor {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val initialNetwork = manager.activeNetwork
    private val connection = DefaultNetworkState(initialNetwork, hasInternetAccess(initialNetwork?.let(manager::getNetworkCapabilities)))
    private val state = MutableStateFlow(connection.isOnline)
    override val isOnline = state.asStateFlow()

    init {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update { available(network) }
            override fun onLost(network: Network) = update { lost(network) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                update { capabilitiesChanged(network, hasInternetAccess(capabilities)) }
            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) =
                update { blockedChanged(network, blocked) }
        }
        manager.registerDefaultNetworkCallback(callback)
        scope.coroutineContext[Job]?.invokeOnCompletion { manager.unregisterNetworkCallback(callback) }
    }

    private fun update(action: DefaultNetworkState<Network>.() -> Unit) = synchronized(connection) {
        connection.action()
        state.value = connection.isOnline
    }
}
