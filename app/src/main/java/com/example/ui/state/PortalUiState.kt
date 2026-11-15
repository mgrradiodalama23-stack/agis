package com.example.ui.state

object AtlasConstants {
    const val PORTAL_URL =
        "https://script.google.com/macros/s/AKfycbwZL5EgYgjEd5trmFIC4NETu6cCMWfUwNFDLuyy0IvbsAsjA28OvXNQZjZ6mOcuyZiCiQ/exec"
    const val PORTAL_HOST = "script.google.com"
    const val SANDBOX_HOST_SUFFIX = "script.googleusercontent.com"
}

data class PortalUiState(
    val currentUrl: String = AtlasConstants.PORTAL_URL,
    val pageTitle: String = "ATLAS | AZKO Area 7",
    val isLoading: Boolean = true,
    val hasFinishedInitialLoad: Boolean = false,
    val loadProgress: Int = 0,
    val canGoBack: Boolean = false,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val isFullscreen: Boolean = false,
    val showPortalInfoSheet: Boolean = false,
    val isPopupOpen: Boolean = false,
    val popupUrl: String = "",
    val popupTitle: String = "Report Viewer",
    val popupProgress: Int = 0,
    val isPopupLoading: Boolean = false,
    val popupCanGoBack: Boolean = false
)
