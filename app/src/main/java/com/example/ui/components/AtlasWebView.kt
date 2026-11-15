package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Injects CSS and a lightweight MutationObserver on Google Apps Script wrapper pages
 * (`script.google.com`) to permanently hide the "This application was created by a Google Apps Script user"
 * warning bar (`#warning`, `.warning-bar`, and its wrapper table row) without affecting `#sandboxFrame`
 * or `google.script.run` RPC calls.
 */
object GasWarningBarRemover {
    private const val HIDE_WARNING_SCRIPT = """
        (function() {
            try {
                var host = (window.location && window.location.hostname) || '';
                if (host.indexOf('script.google.com') === -1) return;

                function applyHide() {
                    var doc = document;
                    if (!doc) return;
                    var head = doc.head || doc.documentElement;
                    if (head && !doc.getElementById('atlas-hide-gas-warning-style')) {
                        var style = doc.createElement('style');
                        style.id = 'atlas-hide-gas-warning-style';
                        style.type = 'text/css';
                        style.textContent =
                            '#warning, .warning-bar, #warning-bar-table > tbody > tr:first-child:not(:has(#sandboxFrame)), ' +
                            '#warning-bar-table tr:has(#warning), #warning-bar-table tr:has(.warning-bar) { ' +
                            '  display: none !important; height: 0 !important; min-height: 0 !important; ' +
                            '  max-height: 0 !important; margin: 0 !important; padding: 0 !important; ' +
                            '  overflow: hidden !important; visibility: hidden !important; opacity: 0 !important; ' +
                            '} ' +
                            'html, body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow: hidden !important; } ' +
                            '#warning-bar-table { width: 100% !important; height: 100% !important; border-collapse: collapse !important; } ' +
                            '#sandboxFrame { width: 100% !important; height: 100% !important; border: 0 !important; display: block !important; }';
                        head.appendChild(style);
                    }

                    var warningSelectors = ['#warning', '.warning-bar'];
                    for (var i = 0; i < warningSelectors.length; i++) {
                        var nodes = doc.querySelectorAll(warningSelectors[i]);
                        for (var j = 0; j < nodes.length; j++) {
                            var el = nodes[j];
                            el.style.setProperty('display', 'none', 'important');
                            el.style.setProperty('height', '0px', 'important');
                            el.style.setProperty('visibility', 'hidden', 'important');
                            var row = el.closest ? el.closest('tr') : null;
                            if (row && !row.querySelector('#sandboxFrame')) {
                                row.style.setProperty('display', 'none', 'important');
                                row.style.setProperty('height', '0px', 'important');
                            }
                        }
                    }

                    var table = doc.getElementById('warning-bar-table');
                    if (table && table.rows) {
                        for (var r = 0; r < table.rows.length; r++) {
                            var tr = table.rows[r];
                            if (!tr.querySelector('#sandboxFrame')) {
                                tr.style.setProperty('display', 'none', 'important');
                                tr.style.setProperty('height', '0px', 'important');
                            } else {
                                tr.style.setProperty('height', '100%', 'important');
                                var td = tr.cells && tr.cells[0];
                                if (td) {
                                    td.style.setProperty('height', '100%', 'important');
                                    td.style.setProperty('padding', '0', 'important');
                                }
                            }
                        }
                    }
                }

                applyHide();

                if (!window.__atlasGasObserver && (doc = document) && (doc.body || doc.documentElement)) {
                    var target = doc.body || doc.documentElement;
                    window.__atlasGasObserver = new MutationObserver(function() {
                        applyHide();
                    });
                    window.__atlasGasObserver.observe(target, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        attributeFilter: ['style', 'class']
                    });
                }
            } catch (e) {}
        })();
    """

    fun inject(webView: WebView?) {
        if (webView == null) return
        val url = webView.url
        if (url != null && url.contains("script.google.com", ignoreCase = true)) {
            webView.evaluateJavascript(HIDE_WARNING_SCRIPT, null)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun configureAtlasWebViewSettings(webView: WebView) {
    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        allowFileAccess = false
        allowContentAccess = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
        mediaPlaybackRequiresUserGesture = false
        // Strip "; wv" token so Google Apps Script / Drive / Sheets / Looker Studio treat it as standard Android Chrome
        userAgentString = userAgentString.replace("; wv", "")
    }
    webView.isVerticalScrollBarEnabled = true
    webView.isHorizontalScrollBarEnabled = false
}

fun createPopupWebView(
    context: Context,
    onProgress: (progress: Int, url: String?, title: String?, canGoBack: Boolean) -> Unit,
    onCloseRequested: () -> Unit,
    onOpenExternal: (Uri) -> Boolean,
    onFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onDownload: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit
): WebView {
    return WebView(context).apply {
        configureAtlasWebViewSettings(this)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme != "http" && scheme != "https" && scheme != "about") {
                    return onOpenExternal(uri)
                }
                if (scheme == "https" && isExternalNativeAppUri(uri)) {
                    if (onOpenExternal(uri)) return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                GasWarningBarRemover.inject(view)
                onProgress(15, url, view?.title, view?.canGoBack() == true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                GasWarningBarRemover.inject(view)
                CookieManager.getInstance().flush()
                onProgress(100, url, view?.title, view?.canGoBack() == true)
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 20) {
                    GasWarningBarRemover.inject(view)
                }
                onProgress(newProgress, view?.url, view?.title, view?.canGoBack() == true)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                onProgress(view?.progress ?: 100, view?.url, title, view?.canGoBack() == true)
            }

            override fun onCloseWindow(window: WebView?) {
                onCloseRequested()
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                return onFileChooser(filePathCallback, fileChooserParams)
            }
        }
        setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            onDownload(url, userAgent, contentDisposition, mimeType)
        })
    }
}

private fun isExternalNativeAppUri(uri: Uri): Boolean {
    val host = uri.host?.lowercase().orEmpty()
    return host.contains("wa.me") ||
        host.contains("api.whatsapp.com") ||
        host.contains("play.google.com") ||
        host.contains("maps.google.com")
}

fun handleExternalUri(context: Context, uri: Uri): Boolean {
    return try {
        val scheme = uri.scheme?.lowercase().orEmpty()
        val intent = if (scheme == "intent") {
            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
fun AtlasWebView(
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (url: String?, title: String?, canGoBack: Boolean) -> Unit,
    onProgressChanged: (progress: Int, canGoBack: Boolean) -> Unit,
    onPageError: (String?) -> Unit,
    onCreatePopupWindow: (Message?) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onOpenExternal: (Uri) -> Boolean,
    onDownload: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                configureAtlasWebViewSettings(this)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val scheme = uri.scheme?.lowercase().orEmpty()
                        if (scheme != "http" && scheme != "https" && scheme != "about") {
                            return onOpenExternal(uri)
                        }
                        if (scheme == "https" && isExternalNativeAppUri(uri)) {
                            if (onOpenExternal(uri)) return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        GasWarningBarRemover.inject(view)
                        onPageStarted(url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        GasWarningBarRemover.inject(view)
                        CookieManager.getInstance().flush()
                        onPageFinished(url, view?.title, view?.canGoBack() == true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onPageError(error?.description?.toString())
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress >= 15) {
                            GasWarningBarRemover.inject(view)
                        }
                        onProgressChanged(newProgress, view?.canGoBack() == true)
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        onCreatePopupWindow(resultMsg)
                        return true
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view != null && callback != null) {
                            onShowCustomView(view, callback)
                        }
                    }

                    override fun onHideCustomView() {
                        onHideCustomView()
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        return onFileChooser(filePathCallback, fileChooserParams)
                    }
                }

                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    onDownload(url, userAgent, contentDisposition, mimeType)
                })

                onWebViewCreated(this)
                loadUrl(initialUrl)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun CustomFullscreenOverlay(
    customView: View,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                (customView.parent as? ViewGroup)?.removeView(customView)
                addView(
                    customView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
