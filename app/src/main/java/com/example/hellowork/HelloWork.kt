package com.example.hellowork

import android.app.Application

class HelloWork : Application() {
    companion object {
        lateinit var prefs: PrefManager
    }
    override fun onCreate() {
        prefs = PrefManager(applicationContext)
        super.onCreate()
    }

}