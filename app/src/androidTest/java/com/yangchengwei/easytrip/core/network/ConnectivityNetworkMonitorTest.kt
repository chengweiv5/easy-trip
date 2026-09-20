package com.yangchengwei.easytrip.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ConnectivityNetworkMonitorTest {
    @Test fun availableDefaultNetworkIsReportedOnline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        assumeTrue(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        assumeTrue(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val monitor = ConnectivityNetworkMonitor(context, scope)
            assertTrue(withTimeout(5_000) { monitor.isOnline.first { it } })
            println("NETWORK_MONITOR cellular=${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)} online=true")
        } finally {
            scope.cancel()
        }
    }
}
