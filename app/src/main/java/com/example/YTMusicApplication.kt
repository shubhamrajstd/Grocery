package com.example

import android.app.Application

class YTMusicApplication : Application() {
    override fun getAttributionTag(): String? {
        return "media"
    }
}
