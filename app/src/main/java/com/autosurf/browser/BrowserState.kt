package com.autosurf.browser

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class Bookmark(val title: String, val url: String)

object BrowserState {
    var desktopMode: Boolean = false
    var currentUrl: String = "about:start"

    val bookmarks = mutableListOf(
        Bookmark("Google", "https://www.google.com"),
        Bookmark("YouTube", "https://m.youtube.com"),
        Bookmark("Wikipedia", "https://en.m.wikipedia.org"),
        Bookmark("Reddit", "https://www.reddit.com"),
        Bookmark("News", "https://news.google.com"),
        Bookmark("GitHub", "https://github.com")
    )

    private val _urlCommands = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val urlCommands = _urlCommands.asSharedFlow()

    fun openUrl(url: String) {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://") || url == "about:start") {
            url
        } else {
            "https://$url"
        }
        currentUrl = normalized
        _urlCommands.tryEmit(normalized)
    }
}
