package com.wenge.sautils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sensorsdata.analytics.android.sdk.BuildConfig
import com.wenge.analytics.SAMarker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SAMarker.initSensors("url", "tag", BuildConfig.DEBUG, this, "uid")
        SAMarker.trackViewScreen(this, "main")
    }
}