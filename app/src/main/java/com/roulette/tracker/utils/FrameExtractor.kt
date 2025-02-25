package com.roulette.tracker.utils

import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceView
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc

class FrameExtractor {
    fun extractFrame(surfaceView: SurfaceView): Mat? {
        try {
            // Screenshot des SurfaceView erstellen
            val bitmap = Bitmap.createBitmap(
                surfaceView.width,
                surfaceView.height,
                Bitmap.Config.ARGB_8888
            )
            
            // Bitmap zu OpenCV Mat konvertieren
            val frame = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
            Utils.bitmapToMat(bitmap, frame)
            
            // Farbraum konvertieren
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2BGR)
            
            return frame
        } catch (e: Exception) {
            Log.e("FrameExtractor", "Fehler bei Frame-Extraktion: ${e.message}")
            return null
        }
    }
} 