package com.yangchengwei.easytrip.core.network

internal fun canAttemptInternet(hasInternet: Boolean, captivePortal: Boolean): Boolean =
    hasInternet && !captivePortal

internal class DefaultNetworkState<T>(initialNetwork: T?, initialOnline: Boolean) {
    private var current = initialNetwork
    private var capable = initialOnline
    private var blocked = false
    val isOnline: Boolean get() = capable && !blocked

    fun available(network: T) { current = network; blocked = false }
    fun capabilitiesChanged(network: T, online: Boolean) {
        if (network == current) capable = online
    }
    fun blockedChanged(network: T, value: Boolean) {
        if (network == current) blocked = value
    }
    fun lost(network: T) {
        if (network == current) { current = null; capable = false; blocked = false }
    }
}
