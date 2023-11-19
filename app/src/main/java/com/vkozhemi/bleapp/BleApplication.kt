package com.vkozhemi.bleapp

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BleApplication : Application() {

    init {
        instance = this
    }

    companion object {
        lateinit var instance: BleApplication
        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }
}