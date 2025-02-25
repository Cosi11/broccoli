package com.roulette.tracker.ui.tracking

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.content.Context
import android.content.res.AssetManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.opencv.core.CvType
import java.io.File

class TestFrameProvider @Inject constructor(
    private val context: Context
) : BaseFrameProvider() {
    
    private var frameCount = 0
    private val testFrames = mutableListOf<Mat>()
    
    init {
        loadTestFrames()
    }
    
    private fun loadTestFrames() {
        try {
            context.assets.list("test_frames")?.forEach { fileName ->
                if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                    val inputStream = context.assets.open("test_frames/$fileName")
                    val file = File(context.cacheDir, fileName)
                    file.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    val mat = Imgcodecs.imread(file.absolutePath)
                    if (!mat.empty()) {
                        testFrames.add(mat)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: Erstelle einen leeren Frame
            testFrames.add(Mat(720, 1280, CvType.CV_8UC3))
        }
    }
    
    override suspend fun getNextFrame(): Mat? = withContext(Dispatchers.IO) {
        if (!isActive) return@withContext null
        
        try {
            delay(33) // ~30 FPS
            val frame = testFrames[frameCount % testFrames.size].clone()
            frameCount++
            frame
        } catch (e: Exception) {
            null
        }
    }

    fun getFrameFlow(): Flow<Mat> = flow {
        while (isActive) {
            getNextFrame()?.let { frame ->
                emit(frame)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override fun onCleared() {
        super.onCleared()
        testFrames.forEach { it.release() }
        testFrames.clear()
    }
} 