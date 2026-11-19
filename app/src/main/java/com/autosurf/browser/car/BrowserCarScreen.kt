package com.autosurf.browser.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.autosurf.browser.BrowserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BrowserCarScreen(carContext: CarContext) : Screen(carContext) {

    private val surfaceController = BrowserSurfaceController(carContext) {
        invalidate()
    }
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceController)

        // Listen for URLs pushed from the phone companion app
        scope.launch {
            BrowserState.urlCommands.collect { url ->
                surfaceController.loadUrl(url)
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val primaryStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Back")
                    .setOnClickListener { surfaceController.goBack() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Home")
                    .setOnClickListener { surfaceController.loadStartPage() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Menu (${surfaceController.tabCount})")
                    .setOnClickListener {
                        screenManager.push(MenuCarScreen(carContext, surfaceController))
                    }
                    .build()
            )
            .build()

        // Action.PAN enables scroll/zoom/tap interaction on the head-unit surface
        val mapStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setTitle(if (BrowserState.desktopMode) "PC" else "Mob")
                    .setOnClickListener { surfaceController.toggleDesktopMode() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Reload")
                    .setOnClickListener { surfaceController.reload() }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(primaryStrip)
            .setMapActionStrip(mapStrip)
            .build()
    }
}
