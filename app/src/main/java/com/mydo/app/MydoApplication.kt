package com.mydo.app

import android.app.Application
import com.mydo.app.di.AppContainer

class MydoApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}
