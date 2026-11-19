package com.autosurf.browser.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class AutoBrowserService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Allows testing on DHU and sideloaded Android Auto head units
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return BrowserCarScreen(carContext)
            }
        }
    }
}
