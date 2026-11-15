package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import com.example.ui.state.AtlasConstants
import com.example.ui.state.PortalUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AtlasViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        PortalUiState(isOnline = checkCurrentConnectivity(application))
    )
    val uiState: StateFlow<PortalUiState> = _uiState.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _uiState.update { it.copy(isOnline = true) }
        }

        override fun onLost(network: Network) {
            _uiState.update {
                it.copy(isOnline = checkCurrentConnectivity(getApplication()))
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val validated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _uiState.update { it.copy(isOnline = validated) }
        }
    }

    init {
        registerNetworkMonitor()
    }

    private fun registerNetworkMonitor() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Ignore on restricted devices
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Already unregistered
        }
    }

    fun onPageStarted(url: String?) {
        _uiState.update {
            it.copy(
                currentUrl = url ?: it.currentUrl,
                isLoading = true,
                errorMessage = null
            )
        }
    }

    fun onPageFinished(url: String?, title: String?, canGoBack: Boolean) {
        _uiState.update {
            it.copy(
                currentUrl = url ?: it.currentUrl,
                pageTitle = title?.takeIf { t -> t.isNotBlank() && t != "about:blank" } ?: it.pageTitle,
                isLoading = false,
                hasFinishedInitialLoad = true,
                loadProgress = 100,
                canGoBack = canGoBack
            )
        }
    }

    fun onProgressChanged(progress: Int, canGoBack: Boolean) {
        _uiState.update {
            val finishedInitial = it.hasFinishedInitialLoad || progress >= 95
            it.copy(
                loadProgress = progress.coerceIn(0, 100),
                isLoading = progress < 100,
                hasFinishedInitialLoad = finishedInitial,
                canGoBack = canGoBack
            )
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack) }
    }

    fun onPageError(description: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                hasFinishedInitialLoad = true,
                errorMessage = description ?: "Tidak dapat terhubung ke server ATLAS."
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, isLoading = true) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun setFullscreen(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreen = enabled) }
    }

    fun setPortalInfoVisible(visible: Boolean) {
        _uiState.update { it.copy(showPortalInfoSheet = visible) }
    }

    fun openPopup(initialUrl: String = "") {
        _uiState.update {
            it.copy(
                isPopupOpen = true,
                popupUrl = initialUrl,
                popupTitle = "Report Viewer",
                popupProgress = 0,
                isPopupLoading = true,
                popupCanGoBack = false
            )
        }
    }

    fun updatePopupProgress(progress: Int, url: String?, title: String?, canGoBack: Boolean) {
        _uiState.update {
            it.copy(
                popupProgress = progress.coerceIn(0, 100),
                isPopupLoading = progress < 100,
                popupUrl = url?.takeIf { u -> u.isNotBlank() && u != "about:blank" } ?: it.popupUrl,
                popupTitle = title?.takeIf { t -> t.isNotBlank() && t != "about:blank" } ?: it.popupTitle,
                popupCanGoBack = canGoBack
            )
        }
    }

    fun closePopup() {
        _uiState.update {
            it.copy(
                isPopupOpen = false,
                popupUrl = "",
                popupTitle = "Report Viewer",
                popupProgress = 0,
                isPopupLoading = false,
                popupCanGoBack = false
            )
        }
    }

    fun refreshConnectivity() {
        val online = checkCurrentConnectivity(getApplication())
        _uiState.update { it.copy(isOnline = online) }
    }

    fun resetToHomeState() {
        _uiState.update {
            it.copy(
                currentUrl = AtlasConstants.PORTAL_URL,
                errorMessage = null,
                isLoading = true
            )
        }
    }

    private fun checkCurrentConnectivity(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
