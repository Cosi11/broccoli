package com.roulette.tracker.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Mat

class CameraPreviewManager(private val context: Context) {
    private var previewBitmap: Bitmap? = null
    private val rotationMatrix = Matrix().apply {
        postRotate(90f)  // Kamera ist standardmäßig um 90° gedreht
    }

    fun showPreview(frame: Mat, previewView: ImageView) {
        try {
            // Bitmap recyclen wenn nötig
            previewBitmap?.recycle()
            
            // Neue Bitmap erstellen
            previewBitmap = Bitmap.createBitmap(
                frame.cols(),
                frame.rows(),
                Bitmap.Config.ARGB_8888
            )
            
            // OpenCV Mat in Bitmap konvertieren
            Utils.matToBitmap(frame, previewBitmap)
            
            // Bitmap rotieren wenn nötig
            val rotatedBitmap = Bitmap.createBitmap(
                previewBitmap!!,
                0,
                0,
                previewBitmap!!.width,
                previewBitmap!!.height,
                rotationMatrix,
                true
            )
            
            // In UI Thread anzeigen
            previewView.post {
                previewView.setImageBitmap(rotatedBitmap)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing preview: ${e.message}")
        }
    }

    fun release() {
        previewBitmap?.recycle()
        previewBitmap = null
    }

    companion object {
        private const val TAG = "CameraPreviewManager"
    }
} 