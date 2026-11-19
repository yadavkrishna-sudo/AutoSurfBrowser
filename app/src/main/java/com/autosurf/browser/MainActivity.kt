package com.autosurf.browser

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlInput = EditText(this).apply {
            hint = "Enter URL to open on Android Auto (e.g. youtube.com)"
        }
        val openButton = Button(this).apply {
            text = "Open on Car Display"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    BrowserState.openUrl(url)
                    Toast.makeText(this@MainActivity, "Sent to Android Auto", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val bookmarkNameInput = EditText(this).apply {
            hint = "Bookmark Title"
        }
        val addBookmarkButton = Button(this).apply {
            text = "Add to Car Bookmarks"
            setOnClickListener {
                val title = bookmarkNameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    val normalized = if (url.startsWith("http")) url else "https://$url"
                    BrowserState.bookmarks.add(Bookmark(title, normalized))
                    Toast.makeText(this@MainActivity, "Bookmark added", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(urlInput)
            addView(openButton)
            addView(bookmarkNameInput)
            addView(addBookmarkButton)
        }
        setContentView(layout)
    }
}
