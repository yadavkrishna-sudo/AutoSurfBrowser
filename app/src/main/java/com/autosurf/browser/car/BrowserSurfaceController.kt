package com.autosurf.browser.car

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.autosurf.browser.BrowserState

class BrowserSurfaceController(
    private val carContext: CarContext,
    private val onStateChanged: () -> Unit
) : SurfaceCallback {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var rootContainer: FrameLayout? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    // Multi-tab support
    private val tabs = mutableListOf<WebView>()
    private var activeTabIndex = 0
    private var currentZoom = 100

    private val desktopUserAgent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    val activeWebView: WebView?
        get() = tabs.getOrNull(activeTabIndex)

    val tabCount: Int
        get() = tabs.size

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        if (surfaceContainer.width <= 0 || surfaceContainer.height <= 0 || surfaceContainer.dpi <= 0) return

        mainHandler.post {
            releaseResources()

            val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            virtualDisplay = displayManager.createVirtualDisplay(
                "AutoSurfDisplay",
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            )

            val display = virtualDisplay?.display ?: return@post
            presentation = Presentation(carContext, display).apply {
                rootContainer = FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                setContentView(rootContainer!!)
                show()
            }

            if (tabs.isEmpty()) {
                createNewTab(BrowserState.currentUrl)
            } else {
                attachActiveTab()
            }
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        mainHandler.post { releaseResources() }
    }

    // Forward head-unit taps directly to the WebView (or fullscreen video player)
    override fun onClick(x: Float, y: Float) {
        mainHandler.post {
            val targetView: View = fullscreenView ?: activeWebView ?: return@post
            val downTime = SystemClock.uptimeMillis()

            val downEvent = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0
            )
            val upEvent = MotionEvent.obtain(
                downTime, downTime + 40, MotionEvent.ACTION_UP, x, y, 0
            )
            targetView.dispatchTouchEvent(downEvent)
            targetView.dispatchTouchEvent(upEvent)
            downEvent.recycle()
            upEvent.recycle()
        }
    }

    // Smooth scrolling on head-unit drag gestures
    override fun onScroll(distanceX: Float, distanceY: Float) {
        mainHandler.post {
            activeWebView?.scrollBy(distanceX.toInt(), distanceY.toInt())
        }
    }

    // Pinch-to-zoom text/UI scaling
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        mainHandler.post {
            if (scaleFactor > 1.02f) {
                currentZoom = (currentZoom + 10).coerceAtMost(200)
            } else if (scaleFactor < 0.98f) {
                currentZoom = (currentZoom - 10).coerceAtLeast(60)
            }
            activeWebView?.settings?.textZoom = currentZoom
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createNewTab(url: String = "about:start") {
        val ctx = presentation?.context ?: return
        val webView = WebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                textZoom = currentZoom
                if (BrowserState.desktopMode) {
                    userAgentString = desktopUserAgent
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                    if (loadedUrl != null && !loadedUrl.startsWith("data:")) {
                        BrowserState.currentUrl = loadedUrl
                    }
                    onStateChanged()
                }
            }
            webChromeClient = object : WebChromeClient() {
                // Handles fullscreen HTML5 video (e.g., YouTube) cleanly on the head unit
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    fullscreenView?.let { rootContainer?.removeView(it) }
                    fullscreenView = view
                    fullscreenCallback = callback
                    view?.let { rootContainer?.addView(it) }
                }

                override fun onHideCustomView() {
                    fullscreenView?.let { rootContainer?.removeView(it) }
                    fullscreenView = null
                    fullscreenCallback?.onCustomViewHidden()
                    fullscreenCallback = null
                }
            }
        }
        tabs.add(webView)
        activeTabIndex = tabs.lastIndex
        attachActiveTab()
        loadUrl(url)
    }

    fun switchToTab(index: Int) {
        if (index in tabs.indices) {
            activeTabIndex = index
            attachActiveTab()
            onStateChanged()
        }
    }

    fun closeCurrentTab() {
        if (tabs.size <= 1) {
            loadStartPage()
            return
        }
        val removed = tabs.removeAt(activeTabIndex)
        rootContainer?.removeView(removed)
        removed.destroy()
        activeTabIndex = (activeTabIndex - 1).coerceAtLeast(0)
        attachActiveTab()
        onStateChanged()
    }

    fun loadUrl(url: String) {
        mainHandler.post {
            if (url == "about:start") {
                loadStartPage()
            } else {
                activeWebView?.loadUrl(url)
            }
        }
    }

    fun loadStartPage() {
        mainHandler.post {
            val tilesHtml = BrowserState.bookmarks.take(6).joinToString("") { bm ->
                """<a class="tile" href="${bm.url}">${bm.title}</a>"""
            }
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    body { margin:0; padding:24px; background:#121318; color:#fff; font-family:sans-serif; }
                    h2 { margin:0 0 18px 0; font-size:22px; color:#a8c7fa; }
                    .grid { display:grid; grid-template-columns: repeat(3, 1fr); gap:16px; }
                    .tile {
                      display:flex; align-items:center; justify-content:center;
                      height:110px; background:#1f222b; border-radius:18px;
                      color:#e8eaed; text-decoration:none; font-size:22px; font-weight:600;
                      border:2px solid #2e3340;
                    }
                  </style>
                </head>
                <body>
                  <h2>AutoSurf Quick Access</h2>
                  <div class="grid">$tilesHtml</div>
                </body>
                </html>
            """.trimIndent()
            activeWebView?.loadDataWithBaseURL("https://autosurf.local/", html, "text/html", "UTF-8", null)
        }
    }

    fun goBack(): Boolean {
        if (fullscreenView != null) {
            fullscreenCallback?.onCustomViewHidden()
            return true
        }
        val wv = activeWebView ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else false
    }

    fun reload() {
        mainHandler.post { activeWebView?.reload() }
    }

    fun toggleDesktopMode() {
        mainHandler.post {
            BrowserState.desktopMode = !BrowserState.desktopMode
            activeWebView?.apply {
                settings.userAgentString = if (BrowserState.desktopMode) desktopUserAgent else null
                reload()
            }
            onStateChanged()
        }
    }

    fun getTabTitles(): List<String> = tabs.mapIndexed { i, wv ->
        val title = wv.title?.takeIf { it.isNotBlank() } ?: "Tab ${i + 1}"
        if (i == activeTabIndex) "● $title" else title
    }

    private fun attachActiveTab() {
        val container = rootContainer ?: return
        container.removeAllViews()
        activeWebView?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            container.addView(wv)
        }
    }

    private fun releaseResources() {
        rootContainer?.removeAllViews()
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
