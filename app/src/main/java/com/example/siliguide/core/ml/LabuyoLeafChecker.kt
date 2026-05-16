package com.example.siliguide.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

data class GatekeeperResult(
    val isLeaf: Boolean,
    val confidence: Float,
    val featureMaps: Array<Array<Array<FloatArray>>>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GatekeeperResult

        if (isLeaf != other.isLeaf) return false
        if (confidence != other.confidence) return false
        if (featureMaps != null) {
            if (other.featureMaps == null) return false
            if (!featureMaps.contentDeepEquals(other.featureMaps)) return false
        } else if (other.featureMaps != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLeaf.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + (featureMaps?.contentDeepHashCode() ?: 0)
        return result
    }
}

class LabuyoLeafChecker(context: Context) {

    private var interpreter: Interpreter? = null
    private var inputWidth: Int = 224
    private var inputHeight: Int = 224

    // FIX 2: Variables to track which output is which (TFLite sometimes swaps them)
    private var predictionOutputIndex: Int = 0
    private var featureMapOutputIndex: Int = 1

    init {
        try {
            val appContext = context.applicationContext
            // Make sure this points to your newly converted DUAL OUTPUT .tflite file
            val fileDescriptor = appContext.assets.openFd("models/leaf_checker_model2_dual_output_v3small.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel

            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            inputStream.close()
            fileDescriptor.close()

            val options = Interpreter.Options()
            val tempInterpreter = Interpreter(buffer, options)

            val inputShape = tempInterpreter.getInputTensor(0).shape()
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]

            // FIX 2 (Continued): Dynamically map the outputs based on their shape
            val shape0 = tempInterpreter.getOutputTensor(0).shape()
            if (shape0.size == 2) {
                // Shape is [1, 2] -> This is the prediction
                predictionOutputIndex = 0
                featureMapOutputIndex = 1
            } else {
                // Shape is [1, 7, 7, 576] -> This is the feature map
                predictionOutputIndex = 1
                featureMapOutputIndex = 0
            }

            interpreter = tempInterpreter
            Log.d("LabuyoLeafChecker", "Dual-Output Gatekeeper model loaded. Input: ${inputWidth}x${inputHeight}")
        } catch (e: Exception) {
            Log.e("LabuyoLeafChecker", "FATAL: Failed to load gatekeeper model", e)
        }
    }

    // FIX 1 (Continued): Change return type to our new Data Class
    fun checkLeaf(bitmap: Bitmap): GatekeeperResult {
        Log.d("LabuyoLeafChecker", "checkLeaf() called - starting verification")

        val currentInterpreter = interpreter ?: run {
            Log.e("LabuyoLeafChecker", "Verification failed: Interpreter is NULL")
            return GatekeeperResult(false, 0f, null)
        }

        return try {
            val input = preprocess(bitmap)

            // FIX 3: Setup Multiple Outputs
            val inputs = arrayOf(input)

            // Buffer for Output 0: [1 batch, 2 classes]
            val predictionsBuffer = Array(1) { FloatArray(2) }

            // Buffer for Output 1: [1 batch, 7 height, 7 width, 576 channels]
            val featureMapsBuffer = Array(1) { Array(7) { Array(7) { FloatArray(576) } } }

            // Map the buffers to the correct TFLite output indices we found in init{}
            val outputs = mutableMapOf<Int, Any>()
            outputs[predictionOutputIndex] = predictionsBuffer
            outputs[featureMapOutputIndex] = featureMapsBuffer

            // RUN DUAL INFERENCE
            currentInterpreter.runForMultipleInputsOutputs(inputs, outputs)

            // Extract Results
            val labuyoScore = predictionsBuffer[0][1]
            val result = labuyoScore > 0.5f

            Log.d(
                "LabuyoLeafChecker",
                "non_labuyo=${predictionsBuffer[0][0]}, siling_labuyo=${predictionsBuffer[0][1]} → isLeaf=$result"
            )

            GatekeeperResult(
                isLeaf = result,
                confidence = labuyoScore,
                featureMaps = featureMapsBuffer // Passing the visual data to the UI!
            )

        } catch (e: Exception) {
            Log.e("LabuyoLeafChecker", "Error during checkLeaf execution", e)
            GatekeeperResult(false, 0f, null)
        }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        // FIX 3: Ensure ARGB_8888 format before reading pixels
        val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        // FIX 3: Use Bitmap.createScaledBitmap for consistent quality (same as LeafClassifier)
        val resized = argbBitmap.scale(inputWidth, inputHeight)

        val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())
        // FIX 3 (KEY FIX): rewind so the model reads from position 0
        buffer.rewind()

        val intValues = IntArray(inputWidth * inputHeight)
        resized.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // Raw 0-255 — MobileNetV3Small expects unscaled pixel values
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        // FIX 3: Recycle intermediate bitmaps to free memory
        if (resized != bitmap && resized != argbBitmap) resized.recycle()
        if (argbBitmap != bitmap) argbBitmap.recycle()

        // FIX 3 (KEY FIX): rewind again before returning so interpreter reads from the start
        buffer.rewind()
        return buffer
    }

    fun close() {
        Log.d("LabuyoLeafChecker", "Closing interpreter")
        interpreter?.close()
        interpreter = null
    }
}