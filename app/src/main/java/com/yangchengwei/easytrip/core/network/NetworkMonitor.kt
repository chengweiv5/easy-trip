package com.yangchengwei.easytrip.core.network
import android.content.Context
import android.net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
fun hasValidatedInternet(capabilities:NetworkCapabilities?)=capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true&&capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
interface NetworkMonitor { val isOnline:StateFlow<Boolean> }
class ConnectivityNetworkMonitor(context:Context,scope:CoroutineScope):NetworkMonitor { private val manager=context.applicationContext.getSystemService(ConnectivityManager::class.java);private fun connected()=hasValidatedInternet(manager.activeNetwork?.let(manager::getNetworkCapabilities));private val state=MutableStateFlow(connected());override val isOnline=state.asStateFlow();init{val callback=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(network:Network){state.value=connected()};override fun onLost(network:Network){state.value=connected()};override fun onCapabilitiesChanged(network:Network,capabilities:NetworkCapabilities){state.value=connected()}};manager.registerDefaultNetworkCallback(callback);scope.coroutineContext[kotlinx.coroutines.Job]?.invokeOnCompletion{manager.unregisterNetworkCallback(callback)}} }
