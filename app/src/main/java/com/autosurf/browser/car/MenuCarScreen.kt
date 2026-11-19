package com.autosurf.browser.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import com.autosurf.browser.BrowserState

class MenuCarScreen(
    carContext: CarContext,
    private val controller: BrowserSurfaceController
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val tabsListBuilder = ItemList.Builder()
        tabsListBuilder.addItem(
            Row.Builder()
                .setTitle("+ New Tab")
                .setOnClickListener {
                    controller.createNewTab("about:start")
                    screenManager.pop()
                }
                .build()
        )
        tabsListBuilder.addItem(
            Row.Builder()
                .setTitle("✕ Close Current Tab")
                .setOnClickListener {
                    controller.closeCurrentTab()
                    screenManager.pop()
                }
                .build()
        )
        controller.getTabTitles().forEachIndexed { index, title ->
            tabsListBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setOnClickListener {
                        controller.switchToTab(index)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        val bookmarksBuilder = ItemList.Builder()
        BrowserState.bookmarks.forEach { bm ->
            bookmarksBuilder.addItem(
                Row.Builder()
                    .setTitle(bm.title)
                    .addText(bm.url)
                    .setOnClickListener {
                        controller.loadUrl(bm.url)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Tabs & Bookmarks")
            .setHeaderAction(Action.BACK)
            .addSectionedList(SectionedItemList.create(tabsListBuilder.build(), "Tabs"))
            .addSectionedList(SectionedItemList.create(bookmarksBuilder.build(), "Bookmarks"))
            .build()
    }
}
