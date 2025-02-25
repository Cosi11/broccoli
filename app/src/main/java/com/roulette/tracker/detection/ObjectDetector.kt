import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectDetector private constructor(private val interpreter: Interpreter) {
    private val inputSize = 300 // TensorFlow Modell Eingabegröße
    private var lastDetection: Point? = null
    
    @Suppress("UNCHECKED_CAST")
    private val outputMap: MutableMap<Int, Any> = HashMap<Int, Any>().apply {
        this[0] = Array(1) { FloatArray(4) } // Bounding Box
        this[1] = Array(1) { FloatArray(1) } // Konfidenz
    }
    
    companion object {
        private const val TAG = "ObjectDetector"
        fun create(context: Context, modelPath: String): ObjectDetector {
            val modelFile = File(context.getExternalFilesDir(null), modelPath)
            val interpreter = Interpreter(modelFile)
            return ObjectDetector(interpreter)
        }
    }
    
    fun detectBall(frame: Mat): Point? {
        val processedFrame = preprocessFrame(frame)
        val inputBuffer = convertBitmapToByteBuffer(processedFrame)
        
        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
        
        val boundingBox = (outputMap[0] as? Array<*>)?.firstOrNull() as? FloatArray
        val confidence = (outputMap[1] as? Array<*>)?.firstOrNull() as? FloatArray

        if (boundingBox == null || confidence == null) {
            Log.e(TAG, "Fehler beim Casting der Detector-Ausgabe")
            return lastDetection
        }
        
        return if (confidence[0] > 0.7f) {
            Point(
                boundingBox[0].toDouble() * frame.cols(),
                boundingBox[1].toDouble() * frame.rows()
            ).also { lastDetection = it }
        } else {
            lastDetection
        }
    }
    
    private fun preprocessFrame(frame: Mat): Bitmap {
        val resized = Mat()
        Imgproc.resize(frame, resized, Size(inputSize.toDouble(), inputSize.toDouble()))
        
        val bitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resized, bitmap)
        return bitmap
    }
    
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            4 * inputSize * inputSize * 3
        ).apply {
            order(ByteOrder.nativeOrder())
        }
        
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixelValue in intValues) {
            byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255f)
            byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255f)
        }
        
        return byteBuffer
    }
} 