package com.nextstepai.inventory

import android.app.Application
import com.nextstepai.inventory.data.db.AppContextHolder

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.appContext = applicationContext
    }
}
