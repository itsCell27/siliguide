package com.example.siliguide

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class SiliGuideApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Offload heavy native OpenCV initialization to background thread
        // so Application.onCreate() does not block the UI thread during cold start.
        CoroutineScope(Dispatchers.Default).launch {
            if (OpenCVLoader.initLocal()) {
                Log.d("OpenCV", "Initialized successfully on background thread")
            } else {
                Log.w("OpenCV", "OpenCV async initialization failed")
            }
        }
    }
}