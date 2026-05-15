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

class LabuyoLeafChecker(context: Context) {

    private var interpreter: Interpreter? = null
    private var inputWidth: Int = 224
    private var inputHeight: Int = 224
    private var outputSize: Int = 1

    init {
        try {
            val appContext = context.applicationContext
            val fileDescriptor = appContext.assets.openFd("models/gatekeeper2_v3small.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel

            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            // FIX 3: Close streams after mapping to avoid file descriptor leaks
            inputStream.close()
            fileDescriptor.close()

            val options = Interpreter.Options()
            val tempInterpreter = Interpreter(buffer, options)

            val inputShape = tempInterpreter.getInputTensor(0).shape()
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]

            val outputShape = tempInterpreter.getOutputTensor(0).shape()
            outputSize = outputShape[1]

            interpreter = tempInterpreter
            Log.d("LabuyoLeafChecker", "Gatekeeper model loaded. Input: ${inputWidth}x${inputHeight}, Output: $outputSize")
        } catch (e: Exception) {
            Log.e("LabuyoLeafChecker", "FATAL: Failed to load gatekeeper model", e)
        }
    }

    fun isLeaf(bitmap: Bitmap): Boolean {
        Log.d("LabuyoLeafChecker", "isLeaf() called - starting verification")

        val currentInterpreter = interpreter ?: run {
            Log.e("LabuyoLeafChecker", "Verification failed: Interpreter is NULL")
            return false
        }

        return try {
            val input = preprocess(bitmap)
            val output = Array(1) { FloatArray(outputSize) }

            currentInterpreter.run(input, output)

            // Model output: [non_siling_labuyo_leaf, siling_labuyo_leaf]
            // Index 0 = non_siling_labuyo_leaf, Index 1 = siling_labuyo_leaf
            val labuyoScore = output[0][1]
            val result = labuyoScore > 0.5f

            Log.d(
                "LabuyoLeafChecker",
                "non_labuyo=${output[0][0]}, siling_labuyo=${output[0][1]} → isLeaf=$result"
            )
            result
        } catch (e: Exception) {
            Log.e("LabuyoLeafChecker", "Error during isLeaf execution", e)
            false
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