package com.example.siliguide

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class SiliGuideApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        check(OpenCVLoader.initLocal()) {
            "OpenCV initialization failed"
        }

        Log.d("OpenCV", "Initialized")
    }
}