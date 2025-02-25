@file:OptIn(ExperimentalUnsignedTypes::class)
package com.roulette.tracker

import android.content.Context
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.roulette.tracker.data.WheelData
import javax.inject.Inject

class WheelTracker @Inject constructor(
    private val context: Context,
    private val ocrService: OCRService
) {
    private var lastAngle: Double = 0.0
    private var lastTimestamp: Long = 0

    fun trackWheel(frame: Mat): WheelData {
        // Bild in Graustufen konvertieren
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
        
        // Rauschen reduzieren
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        
        // Kreise erkennen (Hough Transform)
        val circles = Mat()
        Imgproc.HoughCircles(
            gray,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            gray.rows() / 8.0,
            100.0,
            30.0,
            gray.rows() / 8,
            gray.rows() / 3
        )

        if (circles.cols() > 0) {
            val wheelCircle = circles.get(0, 0)
            val center = Point(wheelCircle[0], wheelCircle[1])
            val radius = wheelCircle[2]

            // ROI (Region of Interest) für das Rad erstellen
            val roi = extractROI(frame, center, radius)
            
            // Winkel des Rads berechnen
            val currentAngle = detectWheelAngle(roi)
            
            // Rotationsgeschwindigkeit berechnen
            val currentTime = System.currentTimeMillis()
            val rotationSpeed = calculateRotationSpeed(currentAngle, currentTime)
            
            // Sichtbare Zahlen erkennen
            val visibleNumbers = detectVisibleNumbers(roi)

            lastAngle = currentAngle
            lastTimestamp = currentTime

            return WheelData(
                currentAngle = currentAngle,
                rotationSpeed = rotationSpeed,
                visibleNumbers = visibleNumbers
            )
        }

        return WheelData(
            currentAngle = 0.0,
            rotationSpeed = 0.0,
            visibleNumbers = emptyList()
        )
    }

    private fun extractROI(frame: Mat, center: Point, radius: Double): Mat {
        val roi = Mat()
        try {
            val rect = Rect(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (2 * radius).toInt(),
                (2 * radius).toInt()
            )
            
            // Sicherstellen, dass ROI innerhalb der Bildgrenzen liegt
            val safeRect = Rect(
                rect.x.coerceIn(0, frame.cols() - 1),
                rect.y.coerceIn(0, frame.rows() - 1),
                rect.width.coerceAtMost(frame.cols() - rect.x),
                rect.height.coerceAtMost(frame.rows() - rect.y)
            )
            
            if (safeRect.width > 0 && safeRect.height > 0) {
                frame.submat(safeRect).copyTo(roi)
            } else {
                Log.w(TAG, "Ungültige ROI-Dimensionen")
                frame.copyTo(roi)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei ROI-Extraktion", e)
            frame.copyTo(roi)
        }
        return roi
    }

    private fun detectWheelAngle(roi: Mat): Double {
        // Kanten erkennen
        val edges = Mat()
        Imgproc.Canny(roi, edges, 50.0, 150.0)
        
        // Linien erkennen
        val lines = Mat()
        Imgproc.HoughLines(edges, lines, 1.0, Math.PI / 180, 100)
        
        // Dominanten Winkel finden
        var dominantAngle = 0.0
        if (lines.rows() > 0) {
            val line = lines.get(0, 0)
            dominantAngle = Math.toDegrees(line[1])
        }
        
        return dominantAngle
    }

    private fun calculateRotationSpeed(currentAngle: Double, currentTime: Long): Double {
        if (lastTimestamp == 0L) return 0.0
        
        val deltaTime = (currentTime - lastTimestamp) / 1000.0  // in Sekunden
        if (deltaTime == 0.0) return 0.0
        
        var deltaAngle = currentAngle - lastAngle
        
        // Korrektur für Winkelübergänge
        if (deltaAngle > 180) deltaAngle -= 360
        if (deltaAngle < -180) deltaAngle += 360
        
        return deltaAngle / deltaTime
    }

    private fun detectVisibleNumbers(roi: Mat): List<Int> {
        // Vorverarbeitung für bessere OCR-Ergebnisse
        val processedRoi = Mat()
        
        // Bild für OCR optimieren
        Imgproc.threshold(roi, processedRoi, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
        
        // Text-Regionen finden und extrahieren
        val textRegions = findTextRegions(processedRoi)
        
        // OCR auf jede Region anwenden und Zahlen extrahieren
        return textRegions.mapNotNull { region ->
            try {
                val number = ocrService.recognizeNumber(region)
                if (number in 0..36) number else null
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei OCR-Erkennung", e)
                null
            }
        }
    }

    private fun findTextRegions(image: Mat): List<Mat> {
        val regions = mutableListOf<Mat>()
        
        // MSER (Maximally Stable Extremal Regions) für Texterkennung
        val mser = MSER.create()
        val msers = MatOfRect()
        
        mser.detectRegions(image, msers)
        
        // Regionen extrahieren und filtern
        msers.toArray().forEach { rect ->
            if (isValidTextRegion(rect)) {
                regions.add(image.submat(rect))
            }
        }
        
        return regions
    }

    private fun isValidTextRegion(rect: Rect): Boolean {
        // Filterkriterien für Textregionen
        val aspectRatio = rect.width.toDouble() / rect.height
        val area = rect.width * rect.height
        
        return aspectRatio in 0.2..2.0 && // Typische Aspektverhältnisse für Zahlen
               area in 100..1000 // Minimale/maximale Größe für lesbare Zahlen
    }

    fun release() {
        try {
            // Aufräumen der OpenCV-Ressourcen
            lastAngle = 0.0
            lastTimestamp = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Aufräumen der Ressourcen", e)
        }
    }

    override fun finalize() {
        release()
    }

    companion object {
        private const val TAG = "WheelTracker"
    }
} 