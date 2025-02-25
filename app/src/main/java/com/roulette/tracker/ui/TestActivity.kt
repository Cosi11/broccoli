package com.roulette.tracker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OpenCV initialisieren
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
            Toast.makeText(this, "OpenCV konnte nicht initialisiert werden", Toast.LENGTH_LONG).show()
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded")
            Toast.makeText(this, "OpenCV erfolgreich initialisiert", Toast.LENGTH_SHORT).show()
        }
    }
} 