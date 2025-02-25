@file:OptIn(ExperimentalUnsignedTypes::class)
package com.roulette.tracker

import android.content.Context
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.roulette.tracker.data.BallData
import javax.inject.Inject

class BallTracker @Inject constructor(private val context: Context) {
    private var lastPosition: Point? = null
    private var lastTimestamp: Long = 0

    fun trackBall(frame: Mat): BallData? {
        // Bild in HSV-Farbraum konvertieren
        val hsvFrame = Mat()
        Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV)

        // Weißen Ball erkennen
        val ballMask = Mat()
        Core.inRange(
            hsvFrame,
            Scalar(0.0, 0.0, 200.0),  // Untere Grenze für Weiß
            Scalar(180.0, 30.0, 255.0),  // Obere Grenze für Weiß
            ballMask
        )

        // Rauschen reduzieren
        Imgproc.erode(ballMask, ballMask, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0)))
        Imgproc.dilate(ballMask, ballMask, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0)))

        // Konturen finden
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(ballMask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Größte runde Kontur finden
        val ballPosition = findBallPosition(frame) ?: return null
        
        // Geschwindigkeit berechnen
        val currentTime = System.currentTimeMillis()
        val velocity = calculateVelocity(ballPosition)
        
        // Landezeitpunkt vorhersagen
        val predictedLandingTime = predictLandingTime(ballPosition, velocity)

        lastPosition = ballPosition
        lastTimestamp = currentTime

        return BallData(
            currentPosition = ballPosition,
            velocity = velocity,
            predictedLandingTime = predictedLandingTime
        )
    }

    private fun findBallPosition(frame: Mat): Point? {
        var bestCircle: Point? = null
        var maxCircularity = 0.0

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(frame, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < 100) continue  // Zu kleine Konturen ignorieren

            val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val circularity = 4 * Math.PI * area / (perimeter * perimeter)

            if (circularity > 0.8 && circularity > maxCircularity) {  // Nur sehr runde Objekte
                val moments = Imgproc.moments(contour)
                val center = Point(moments.m10 / moments.m00, moments.m01 / moments.m00)
                bestCircle = center
                maxCircularity = circularity
            }
        }

        return bestCircle
    }

    private fun calculateVelocity(currentPosition: Point): Double {
        if (currentPosition == null || lastPosition == null || lastTimestamp == 0L) return 0.0

        val deltaTime = (System.currentTimeMillis() - lastTimestamp) / 1000.0  // in Sekunden
        if (deltaTime == 0.0) return 0.0

        val distance = Math.sqrt(
            Math.pow(currentPosition.x - lastPosition!!.x, 2.0) +
            Math.pow(currentPosition.y - lastPosition!!.y, 2.0)
        )

        return distance / deltaTime
    }

    private fun predictLandingTime(position: Point, velocity: Double): Long {
        // Einfache lineare Vorhersage - kann später verfeinert werden
        if (position == null || velocity == 0.0) return 0L

        // Annahme: Ball landet am unteren Rand des Roulette-Rads
        val distanceToLanding = 500.0  // Beispielwert, muss angepasst werden
        val timeToLanding = (distanceToLanding / velocity).toLong()

        return System.currentTimeMillis() + timeToLanding
    }
} 