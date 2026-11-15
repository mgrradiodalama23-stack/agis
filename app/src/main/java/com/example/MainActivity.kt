package com.example

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AtlasLoadingOverlay
import com.example.ui.components.AtlasOfflineErrorView
import com.example.ui.components.AtlasTopBar
import com.example.ui.components.AtlasWebView
import com.example.ui.components.CustomFullscreenOverlay
import com.example.ui.components.PortalInfoBottomSheet
import com.example.ui.components.ReportViewerDialog
import com.example.ui.components.createPopupWebView
import com.example.ui.components.handleExternalUri
import com.example.ui.state.AtlasConstants
import com.example.ui.theme.AtlasArea7Theme
import com.example.ui.theme.AtlasCrimsonDark
import com.example.ui.viewmodel.AtlasViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AtlasCrimsonDark.toArgb())
        )
        setContent {
            AtlasArea7Theme {
                AtlasPortalApp(
                    onExitApp = { finish() },
                    onSystemFullscreenChanged = { fullscreen ->
                        updateSystemBars(fullscreen)
                    }
                )
            }
        }
    }

    private fun updateSystemBars(fullscreen: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun AtlasPortalApp(
    onExitApp: () -> Unit,
    onSystemFullscreenChanged: (Boolean) -> Unit,
    viewModel: AtlasViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        if (callback == null) return@rememberLauncherForActivityResult
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            ?: if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
                arrayOf(result.data!!.data!!)
            } else {
                null
            }
        callback.onReceiveValue(uris)
    }

    val launchFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean =
        { callback, params ->
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = callback
            try {
                val intent = params.createIntent()
                fileChooserLauncher.launch(intent)
                true
            } catch (_: Exception) {
                pendingFileCallback = null
                callback.onReceiveValue(null)
                false
            }
        }

    val startDownload: (String, String, String, String) -> Unit =
        { url, userAgent, contentDisposition, mimeType ->
            try {
                val uri = Uri.parse(url)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(uri).apply {
                        setMimeType(mimeType)
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (!cookies.isNullOrEmpty()) {
                            addRequestHeader("Cookie", cookies)
                        }
                        addRequestHeader("User-Agent", userAgent)
                        setTitle(fileName)
                        setDescription(context.getString(R.string.app_name))
                        setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            fileName
                        )
                    }
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    dm?.enqueue(request)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_download_started))
                    }
                } else {
                    handleExternalUri(context, uri)
                }
            } catch (_: Exception) {
                if (!handleExternalUri(context, Uri.parse(url))) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_no_browser))
                    }
                }
            }
        }

    val openInExternalBrowser: (String) -> Unit = { targetUrl ->
        val safeUrl = targetUrl.takeIf { it.startsWith("http") } ?: AtlasConstants.PORTAL_URL
        if (!handleExternalUri(context, Uri.parse(safeUrl))) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_no_browser))
            }
        }
    }

    val closePopupViewer: () -> Unit = {
        popupWebView?.apply {
            stopLoading()
            destroy()
        }
        popupWebView = null
        viewModel.closePopup()
    }

    LaunchedEffect(uiState.isFullscreen, customFullscreenView) {
        onSystemFullscreenChanged(uiState.isFullscreen || customFullscreenView != null)
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            popupWebView?.destroy()
            mainWebView?.destroy()
        }
    }

    BackHandler {
        when {
            customFullscreenView != null -> {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            }
            uiState.isPopupOpen -> {
                val popup = popupWebView
                if (popup != null && popup.canGoBack()) {
                    popup.goBack()
                } else {
                    closePopupViewer()
                }
            }
            uiState.showPortalInfoSheet -> {
                viewModel.setPortalInfoVisible(false)
            }
            uiState.isFullscreen -> {
                viewModel.setFullscreen(false)
            }
            mainWebView?.canGoBack() == true -> {
                mainWebView?.goBack()
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    onExitApp()
                } else {
                    lastBackPressTime = now
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.msg_press_back_again)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AtlasTopBar(
                    isVisible = !uiState.isFullscreen && customFullscreenView == null,
                    isLoading = uiState.isLoading,
                    loadProgress = uiState.loadProgress,
                    isOnline = uiState.isOnline,
                    canGoBack = uiState.canGoBack,
                    onBackClick = {
                        if (mainWebView?.canGoBack() == true) {
                            mainWebView?.goBack()
                        }
                    },
                    onReloadClick = {
                        viewModel.refreshConnectivity()
                        viewModel.clearError()
                        mainWebView?.reload()
                    },
                    onHomeClick = {
                        viewModel.refreshConnectivity()
                        viewModel.resetToHomeState()
                        mainWebView?.loadUrl(AtlasConstants.PORTAL_URL)
                    },
                    onFullscreenClick = { viewModel.toggleFullscreen() },
                    onOpenInBrowserClick = { openInExternalBrowser(uiState.currentUrl) },
                    onClearCacheClick = {
                        mainWebView?.clearCache(true)
                        viewModel.clearError()
                        mainWebView?.loadUrl(AtlasConstants.PORTAL_URL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.msg_cache_cleared)
                            )
                        }
                    },
                    onPortalInfoClick = { viewModel.setPortalInfoVisible(true) }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            if (uiState.isFullscreen) {
                                WindowInsets(0, 0, 0, 0)
                            } else {
                                WindowInsets.navigationBars.union(WindowInsets.ime)
                            }
                        )
                ) {
                    AtlasWebView(
                        initialUrl = AtlasConstants.PORTAL_URL,
                        onWebViewCreated = { webView ->
                            mainWebView = webView
                        },
                        onPageStarted = { url -> viewModel.onPageStarted(url) },
                        onPageFinished = { url, title, canGoBack ->
                            viewModel.onPageFinished(url, title, canGoBack)
                        },
                        onProgressChanged = { progress, canGoBack ->
                            viewModel.onProgressChanged(progress, canGoBack)
                        },
                        onPageError = { message -> viewModel.onPageError(message) },
                        onCreatePopupWindow = { resultMsg: Message? ->
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            if (transport != null) {
                                popupWebView?.destroy()
                                val childWebView = createPopupWebView(
                                    context = context,
                                    onProgress = { progress, url, title, canGoBack ->
                                        viewModel.updatePopupProgress(progress, url, title, canGoBack)
                                    },
                                    onCloseRequested = { closePopupViewer() },
                                    onOpenExternal = { uri ->
                                        val opened = handleExternalUri(context, uri)
                                        if (!opened) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.msg_no_browser)
                                                )
                                            }
                                        }
                                        opened
                                    },
                                    onFileChooser = launchFileChooser,
                                    onDownload = startDownload
                                )
                                popupWebView = childWebView
                                viewModel.openPopup()
                                transport.webView = childWebView
                                resultMsg.sendToTarget()
                            }
                        },
                        onShowCustomView = { view, callback ->
                            customViewCallback?.onCustomViewHidden()
                            customFullscreenView = view
                            customViewCallback = callback
                        },
                        onHideCustomView = {
                            customFullscreenView = null
                            customViewCallback = null
                        },
                        onFileChooser = launchFileChooser,
                        onOpenExternal = { uri ->
                            val opened = handleExternalUri(context, uri)
                            if (!opened) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.msg_no_browser)
                                    )
                                }
                            }
                            opened
                        },
                        onDownload = startDownload
                    )

                    val showOfflineOrError =
                        uiState.errorMessage != null || (!uiState.isOnline && !uiState.hasFinishedInitialLoad)
                    if (showOfflineOrError) {
                        AtlasOfflineErrorView(
                            isOnline = uiState.isOnline,
                            errorMessage = uiState.errorMessage,
                            onRetryClick = {
                                viewModel.refreshConnectivity()
                                viewModel.clearError()
                                val target = mainWebView?.url
                                    ?.takeIf { it.startsWith("http") }
                                    ?: AtlasConstants.PORTAL_URL
                                mainWebView?.loadUrl(target)
                            },
                            onOpenInBrowserClick = { openInExternalBrowser(uiState.currentUrl) }
                        )
                    }

                    AtlasLoadingOverlay(
                        visible = !uiState.hasFinishedInitialLoad && !showOfflineOrError,
                        progress = uiState.loadProgress
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isFullscreen && customFullscreenView == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
            ) {
                Surface(
                    onClick = { viewModel.setFullscreen(false) },
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.testTag("exit_fullscreen_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = stringResource(id = R.string.action_exit_fullscreen),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.action_exit_fullscreen),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            customFullscreenView?.let { fsView ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    CustomFullscreenOverlay(customView = fsView)
                }
            }
        }
    }

    if (uiState.isPopupOpen && popupWebView != null) {
        ReportViewerDialog(
            popupWebView = popupWebView,
            title = uiState.popupTitle,
            url = uiState.popupUrl,
            isLoading = uiState.isPopupLoading,
            progress = uiState.popupProgress,
            canGoBack = uiState.popupCanGoBack,
            onBackClick = { popupWebView?.goBack() },
            onReloadClick = { popupWebView?.reload() },
            onOpenExternalClick = { openInExternalBrowser(uiState.popupUrl) },
            onCloseClick = closePopupViewer
        )
    }

    if (uiState.showPortalInfoSheet) {
        PortalInfoBottomSheet(
            currentUrl = uiState.currentUrl,
            onDismiss = { viewModel.setPortalInfoVisible(false) },
            onCopyUrl = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(
                    ClipData.newPlainText(
                        context.getString(R.string.app_name),
                        AtlasConstants.PORTAL_URL
                    )
                )
                viewModel.setPortalInfoVisible(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_url_copied))
                }
            },
            onShareUrl = {
                viewModel.setPortalInfoVisible(false)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.portal_info_title))
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${context.getString(R.string.portal_info_title)}\n${AtlasConstants.PORTAL_URL}"
                    )
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, context.getString(R.string.action_share_url))
                )
            },
            onOpenInBrowser = { openInExternalBrowser(uiState.currentUrl) },
            onClearCache = {
                mainWebView?.clearCache(true)
                viewModel.clearError()
                mainWebView?.loadUrl(AtlasConstants.PORTAL_URL)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_cache_cleared))
                }
            }
        )
    }
}
