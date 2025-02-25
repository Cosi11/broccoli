package com.roulette.tracker.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.adaptech.tesseract4android.TessBaseAPI
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.ArrayDeque
import org.opencv.core.Scalar

@Singleton
class OCRService @Inject constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var tesseract: TessBaseAPI? = null
    private val TAG = "OCRService"
    private val numberCache = LruCache<Int, List<Int>>(20)
    private val matPool = MatPool(5) // Objekt-Pool für Mat-Objekte
    
    private class MatPool(capacity: Int) {
        private val pool = ArrayDeque<Mat>(capacity)
        
        @Synchronized
        fun acquire(): Mat = pool.removeFirstOrNull() ?: Mat()
        
        @Synchronized
        fun release(mat: Mat) {
            if (pool.size < capacity && !mat.empty()) {
                mat.setTo(Scalar(0.0))
                pool.addLast(mat)
            } else {
                mat.release()
            }
        }
        
        fun clear() {
            pool.forEach { it.release() }
            pool.clear()
        }
    }

    init {
        initTesseract()
    }

    private fun initTesseract() {
        try {
            val dataPath = File(context.getExternalFilesDir(null), "tessdata")
            if (!dataPath.exists()) {
                dataPath.mkdirs()
                copyTrainedData(dataPath)
            }

            tesseract = TessBaseAPI().apply {
                init(dataPath.parent, "eng", TessBaseAPI.OEM_LSTM_ONLY)
                setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789")
                setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]{}|;:,.<>?/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                setVariable("tessedit_pageseg_mode", "7") // Treat image as single text line
                setVariable("tessedit_ocr_engine_mode", "3") // Use LSTM OCR Engine
                setVariable("load_system_dawg", "0") // Disable dictionary
                setVariable("load_freq_dawg", "0")
                setVariable("textord_heavy_nr", "1") // Better handling of numeric recognition
                setVariable("tessedit_minimal_confidence", "60") // Minimum confidence threshold
                setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Tesseract-Initialisierung", e)
        }
    }

    fun recognizeNumbers(mat: Mat): List<Int> {
        var processed = matPool.acquire()
        var kernel = matPool.acquire()
        var bitmap: Bitmap? = null
        
        return try {
            preprocessImage(mat, processed)
            
            // Verbessertes morphologisches Closing
            Imgproc.morphologyEx(processed, processed, Imgproc.MORPH_CLOSE, kernel)
            
            // Kantenglättung
            Imgproc.medianBlur(processed, processed, 3)
            
            bitmap = Bitmap.createBitmap(processed.cols(), processed.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(processed, bitmap)
            
            recognizeNumbers(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der Bildverarbeitung", e)
            emptyList()
        } finally {
            matPool.release(processed)
            matPool.release(kernel)
            bitmap?.recycle()
        }
    }

    fun recognizeNumbers(bitmap: Bitmap): List<Int> {
        val cacheKey = bitmap.generateHash()
        
        numberCache.get(cacheKey)?.let { cached ->
            return cached
        }
        
        return tesseract?.let { tess ->
            try {
                tess.setImage(bitmap)
                val numbers = tess.utF8Text?.split("\n")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.let { validateNumbers(it) }
                    ?: emptyList()
                    
                if (numbers.isNotEmpty()) {
                    numberCache.put(cacheKey, numbers)
                }
                numbers
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei der Zahlenerkennung", e)
                emptyList()
            }
        } ?: emptyList()
    }

    private fun copyTrainedData(dataPath: File) {
        try {
            context.assets.open("tessdata/eng.traineddata").use { input ->
                FileOutputStream(File(dataPath, "eng.traineddata")).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Fehler beim Kopieren der Trainingsdaten", e)
        }
    }

    fun release() {
        tesseract?.end()
        tesseract = null
        matPool.clear()
        numberCache.evictAll()
    }

    private fun Bitmap.generateHash(): Int {
        // Einfacher Hash für Bitmap-Inhalt
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.contentHashCode()
    }

    private fun preprocessImage(mat: Mat, processed: Mat) {
        // Bildgröße optimieren basierend auf DPI
        val dpi = context.resources.displayMetrics.densityDpi
        val scaleFactor = (300.0 / dpi).coerceIn(1.0, 3.0)
        val targetSize = Size(mat.width() * scaleFactor, mat.height() * scaleFactor)
        
        Imgproc.resize(mat, processed, targetSize, 0.0, 0.0, Imgproc.INTER_CUBIC)
        
        // Verbesserte Vorverarbeitung
        Imgproc.cvtColor(processed, processed, Imgproc.COLOR_BGR2GRAY)
        
        // Rauschreduzierung mit bilateralem Filter für bessere Kantenerkennung
        Imgproc.bilateralFilter(processed, processed, 9, 75.0, 75.0)
        
        // Lokale Kontrastverstärkung mit optimierten Parametern
        val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
        clahe.apply(processed, processed)
        
        // Adaptive Schwellenwert-Segmentierung mit optimierten Parametern
        Imgproc.adaptiveThreshold(
            processed, processed,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            15, 5.0
        )
    }

    private fun validateNumbers(numbers: List<Int>): List<Int> {
        if (numbers.isEmpty()) return emptyList()
        
        // Prüfe auf ungültige Zahlenkombinationen
        val validNumbers = numbers.filter { it in 0..36 }
        
        // Prüfe auf Duplikate und unwahrscheinliche Sequenzen
        return validNumbers
            .distinct()
            .filterNot { number ->
                val neighbors = validNumbers.filter { 
                    abs(it - number) <= 1 || abs(it - number) == 35 
                }
                neighbors.size > 2 // Unwahrscheinlich viele benachbarte Zahlen
            }
    }
} 