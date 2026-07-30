package com.lonnnnnng.biu.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.Closeable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.downloadNetworkDataStore by preferencesDataStore(name = "download_network")

enum class DownloadNetworkPreference {
    ANY_VALIDATED,
    UNMETERED_ONLY,
}

data class DownloadNetworkState(
    val hasInternet: Boolean,
    val validated: Boolean,
    val unmetered: Boolean,
) {
    companion object {
        val DISCONNECTED = DownloadNetworkState(
            hasInternet = false,
            validated = false,
            unmetered = false,
        )

        fun fromCapabilities(capabilities: NetworkCapabilities?): DownloadNetworkState {
            if (capabilities == null) return DISCONNECTED
            return DownloadNetworkState(
                hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            )
        }
    }
}

object DownloadNetworkPolicy {
    fun isAllowed(preference: DownloadNetworkPreference, state: DownloadNetworkState): Boolean {
        if (!state.hasInternet || !state.validated) return false
        return preference != DownloadNetworkPreference.UNMETERED_ONLY || state.unmetered
    }

    fun waitingLabel(preference: DownloadNetworkPreference): String {
        return if (preference == DownloadNetworkPreference.UNMETERED_ONLY) {
            "等待 Wi-Fi 或其他非计费网络"
        } else {
            "等待可用网络"
        }
    }
}

class DownloadNetworkPreferenceRepository(context: Context) {
    private val dataStore = context.applicationContext.downloadNetworkDataStore

    val preference: Flow<DownloadNetworkPreference> = dataStore.data.map { preferences ->
        if (preferences[KEY_UNMETERED_ONLY] == true) {
            DownloadNetworkPreference.UNMETERED_ONLY
        } else {
            DownloadNetworkPreference.ANY_VALIDATED
        }
    }

    suspend fun current(): DownloadNetworkPreference = preference.first()

    suspend fun setUnmeteredOnly(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_UNMETERED_ONLY] = enabled }
    }

    private companion object {
        val KEY_UNMETERED_ONLY = booleanPreferencesKey("unmetered_only")
    }
}

class DownloadNetworkMonitor(
    context: Context,
    private val onChanged: (DownloadNetworkState) -> Unit,
) : Closeable {
    private val connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    @Volatile
    private var currentNetwork: Network? = connectivityManager.activeNetwork

    @Volatile
    private var currentState: DownloadNetworkState = DownloadNetworkState.fromCapabilities(
        currentNetwork?.let(connectivityManager::getNetworkCapabilities),
    )

    @Volatile
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            currentNetwork = network
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            currentNetwork = network
            publish(DownloadNetworkState.fromCapabilities(capabilities))
        }

        override fun onLost(network: Network) {
            // long: 默认网络切换时旧网络的 onLost 可能晚于新网络的 onAvailable；只清理由当前网络触发的事件，避免误判为离线。
            if (currentNetwork == network) {
                currentNetwork = null
                publish(DownloadNetworkState.DISCONNECTED)
            }
        }
    }

    fun start() {
        if (registered) return
        connectivityManager.registerDefaultNetworkCallback(callback)
        registered = true
        onChanged(currentState)
    }

    fun state(): DownloadNetworkState = currentState

    override fun close() {
        if (!registered) return
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        registered = false
    }

    private fun publish(state: DownloadNetworkState) {
        currentState = state
        onChanged(state)
    }
}
