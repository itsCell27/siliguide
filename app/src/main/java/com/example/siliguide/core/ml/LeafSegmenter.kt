package com.example.siliguide.core.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.graphics.Color

class LeafSegmenter(
    context: Context,
    modelPath: String
) : Closeable {

    companion object {
        private const val TAG = "LeafSegmenter"

        private const val INPUT_SIZE = 640

        // Number of mask coefficients produced by YOLO
        private const val NUM_MASK_COEFFS = 32

        // Detection confidence threshold
        private const val CONF_THRESHOLD = 0.25f

        // Only kept for future use if we need additional NMS
        private const val IOU_THRESHOLD = 0.45f
    }

    /**
     * Single detected leaf
     */
    data class Detection(
        val box: RectF,
        val confidence: Float,
        val classId: Int,
        val coeffs: FloatArray,
        var mask: Bitmap? = null
    )

    private val interpreter: Interpreter

    init {

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(
            loadModelFile(context, modelPath),
            options
        )

        logModelInfo()
    }

    @Throws(IOException::class)
    private fun loadModelFile(
        context: Context,
        modelPath: String
    ): MappedByteBuffer {

        val fileDescriptor: AssetFileDescriptor =
            context.assets.openFd(modelPath)

        val inputStream =
            FileInputStream(fileDescriptor.fileDescriptor)

        val fileChannel = inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun logModelInfo() {

        Log.d(TAG, "========== MODEL INPUTS ==========")

        for (i in 0 until interpreter.inputTensorCount) {

            val tensor = interpreter.getInputTensor(i)

            Log.d(
                TAG,
                "Input $i " +
                        "shape=${tensor.shape().contentToString()} " +
                        "type=${tensor.dataType()}"
            )
        }

        Log.d(TAG, "========== MODEL OUTPUTS ==========")

        for (i in 0 until interpreter.outputTensorCount) {

            val tensor = interpreter.getOutputTensor(i)

            Log.d(
                TAG,
                "Output $i " +
                        "shape=${tensor.shape().contentToString()} " +
                        "type=${tensor.dataType()} " +
                        "name=${tensor.name()}"
            )
        }

        Log.d(
            TAG,
            "Interpreter initialized successfully."
        )
    }

    /**
     * Runs YOLO instance segmentation.
     */
    fun segment(
        bitmap: Bitmap
    ): List<Detection> {

        Log.d(
            TAG,
            "Starting segmentation on ${bitmap.width}x${bitmap.height}"
        )

        val inputBuffer =
            preprocess(bitmap)

        // Output 0:
        // [1,300,38]
        val output0 =
            Array(1) {
                Array(300) {
                    FloatArray(38)
                }
            }

        // Output 1:
        // [1,32,160,160]
        val output1 =
            Array(1) {
                Array(NUM_MASK_COEFFS) {
                    Array(160) {
                        FloatArray(160)
                    }
                }
            }

        val outputs =
            HashMap<Int, Any>()

        outputs[0] = output0
        outputs[1] = output1

        val start =
            System.currentTimeMillis()

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            outputs
        )

        val end =
            System.currentTimeMillis()

        Log.d(
            TAG,
            "Inference completed in ${end - start} ms"
        )

        return postProcess(
            output0[0],
            output1[0],
            bitmap.width,
            bitmap.height
        )
    }

    /**
     * Converts Bitmap to
     * Float32
     * NCHW
     * RGB
     * [1,3,640,640]
     */
    private fun preprocess(
        bitmap: Bitmap
    ): ByteBuffer {

        val resized =
            Bitmap.createScaledBitmap(
                bitmap,
                INPUT_SIZE,
                INPUT_SIZE,
                true
            )

        val buffer =
            ByteBuffer.allocateDirect(
                4 * 3 * INPUT_SIZE * INPUT_SIZE
            )

        buffer.order(ByteOrder.nativeOrder())

        val pixels =
            IntArray(INPUT_SIZE * INPUT_SIZE)

        resized.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        // ---------- R ----------
        for (pixel in pixels) {
            buffer.putFloat(
                ((pixel shr 16) and 0xFF) / 255f
            )
        }

        // ---------- G ----------
        for (pixel in pixels) {
            buffer.putFloat(
                ((pixel shr 8) and 0xFF) / 255f
            )
        }

        // ---------- B ----------
        for (pixel in pixels) {
            buffer.putFloat(
                (pixel and 0xFF) / 255f
            )
        }

        buffer.rewind()

        return buffer
    }

    private fun postProcess(
        results: Array<FloatArray>,
        protos: Array<Array<FloatArray>>,
        imgW: Int,
        imgH: Int
    ): List<Detection> {

//        Log.d(TAG, "===== FIRST 10 DETECTIONS =====")
//
//        repeat(minOf(10, results.size)) { det ->
//
//            val values = results[det]
//                .joinToString(", ")
//
//            Log.d(
//                TAG,
//                "Detection $det: $values"
//            )
//        }

        val detections = mutableListOf<Detection>()

        results.forEachIndexed { index, detection ->

            val x1 = detection[0]
            val y1 = detection[1]
            val x2 = detection[2]
            val y2 = detection[3]

            val confidence = detection[4]
            val classId = detection[5].toInt()

//            Log.d(
//                TAG,
//                "RAW $index class=$classId conf=$confidence"
//            )

            if (confidence < CONF_THRESHOLD) {

//                Log.d(
//                    TAG,
//                    "SKIP $index class=$classId conf=$confidence"
//                )

                return@forEachIndexed
            }

            val box = RectF(
                x1 * imgW / INPUT_SIZE,
                y1 * imgH / INPUT_SIZE,
                x2 * imgW / INPUT_SIZE,
                y2 * imgH / INPUT_SIZE
            )

            val coeffs = detection.copyOfRange(
                6,
                6 + NUM_MASK_COEFFS
            )



            val det = Detection(
                box = box,
                confidence = confidence,
                classId = classId,
                coeffs = coeffs
            )

            detections.add(det)

//            Log.d(
//                TAG,
//                "KEEP $index " +
//                        "class=$classId " +
//                        "conf=$confidence " +
//                        "box=$box"
//            )
        }

        Log.d(
            TAG,
            "Parsed detections before NMS = ${detections.size}"
        )

        val filteredDetections = nonMaximumSuppression(detections)

        for (det in filteredDetections) {

            val rawMask = reconstructMask(det.coeffs, protos)

            val probabilityMask = applySigmoid(rawMask)

            val upsampledMask = upsampleMask(
                probabilityMask,
                imgW,
                imgH
            )

            val croppedMask = cropMask(
                upsampledMask,
                det.box
            )

            val binaryMask = thresholdMask(croppedMask)

            det.mask = binaryMaskToBitmap(binaryMask)

            Log.d(
                TAG,
                "Generated mask: ${det.mask?.width}x${det.mask?.height}"
            )
        }

        Log.d(
            TAG,
            "Parsed detections after NMS = ${filteredDetections.size}"
        )

        return filteredDetections
    }

    /**
     * Reconstructs the raw prototype mask (160x160)
     * from the 32 mask coefficients.
     *
     * Equivalent to:
     *
     * masks = (masks_in @ protos.view(c,-1)).view(mh,mw)
     *
     * Returns raw mask logits.
     */
    private fun reconstructMask(
        coeffs: FloatArray,
        protos: Array<Array<FloatArray>>
    ): Array<FloatArray> {

        val maskHeight = protos[0].size
        val maskWidth = protos[0][0].size

        val mask = Array(maskHeight) {
            FloatArray(maskWidth)
        }

        for (y in 0 until maskHeight) {

            for (x in 0 until maskWidth) {

                var value = 0f

                for (c in 0 until NUM_MASK_COEFFS) {
                    value += coeffs[c] * protos[c][y][x]
                }

                mask[y][x] = value
            }
        }

        return mask
    }

    private fun sigmoid(x: Float): Float {
        return (1f / (1f + kotlin.math.exp(-x)))
    }

    /**
     * Converts raw mask logits into probabilities.
     *
     * Equivalent to applying sigmoid to every pixel.
     */
    private fun applySigmoid(
        mask: Array<FloatArray>
    ): Array<FloatArray> {

        val height = mask.size
        val width = mask[0].size

        val output = Array(height) {
            FloatArray(width)
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                output[y][x] = sigmoid(mask[y][x])
            }
        }

        return output
    }

    /**
     * Zeroes pixels outside the detected bounding box.
     *
     * The mask is already upsampled to the original image size.
     */
    private fun cropMask(
        mask: Array<FloatArray>,
        box: RectF
    ): Array<FloatArray> {

        val height = mask.size
        val width = mask[0].size

        val output = Array(height) {
            FloatArray(width)
        }

        val left = box.left.toInt().coerceIn(0, width - 1)
        val top = box.top.toInt().coerceIn(0, height - 1)
        val right = box.right.toInt().coerceIn(0, width)
        val bottom = box.bottom.toInt().coerceIn(0, height)

        for (y in top until bottom) {
            for (x in left until right) {
                output[y][x] = mask[y][x]
            }
        }

        return output
    }

    /**
     * Upsamples a probability mask from prototype resolution
     * (160x160) to the original image resolution.
     *
     * Uses bilinear interpolation.
     */
    private fun upsampleMask(
        mask: Array<FloatArray>,
        targetWidth: Int,
        targetHeight: Int
    ): Array<FloatArray> {

        val srcHeight = mask.size
        val srcWidth = mask[0].size

        val output = Array(targetHeight) {
            FloatArray(targetWidth)
        }

        val scaleX = srcWidth.toFloat() / targetWidth
        val scaleY = srcHeight.toFloat() / targetHeight

        for (y in 0 until targetHeight) {

            val srcY = y * scaleY
            val y0 = srcY.toInt().coerceIn(0, srcHeight - 1)
            val y1 = (y0 + 1).coerceIn(0, srcHeight - 1)
            val wy = srcY - y0

            for (x in 0 until targetWidth) {

                val srcX = x * scaleX
                val x0 = srcX.toInt().coerceIn(0, srcWidth - 1)
                val x1 = (x0 + 1).coerceIn(0, srcWidth - 1)
                val wx = srcX - x0

                val top =
                    mask[y0][x0] * (1f - wx) +
                            mask[y0][x1] * wx

                val bottom =
                    mask[y1][x0] * (1f - wx) +
                            mask[y1][x1] * wx

                output[y][x] =
                    top * (1f - wy) +
                            bottom * wy
            }
        }

        return output
    }

    /**
     * Converts a probability mask into a binary mask.
     *
     * Pixels >= threshold become 1.
     * Everything else becomes 0.
     */
    private fun thresholdMask(
        mask: Array<FloatArray>,
        threshold: Float = 0.5f
    ): Array<ByteArray> {

        val height = mask.size
        val width = mask[0].size

        val output = Array(height) {
            ByteArray(width)
        }

        for (y in 0 until height) {
            for (x in 0 until width) {

                output[y][x] =
                    if (mask[y][x] >= threshold)
                        1
                    else
                        0
            }
        }

        return output
    }

    /**
     * Converts a binary mask into an Android Bitmap.
     *
     * Foreground pixels are cyan.
     * Background pixels are transparent.
     */
    private fun binaryMaskToBitmap(
        binaryMask: Array<ByteArray>
    ): Bitmap {

        val height = binaryMask.size
        val width = binaryMask[0].size

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {

                pixels[y * width + x] =
                    if (binaryMask[y][x].toInt() == 1)
                        Color.CYAN
                    else
                        Color.TRANSPARENT
            }
        }

        bitmap.setPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        return bitmap
    }

    /**
     * Computes Intersection over Union (IoU)
     * between two bounding boxes.
     */
    private fun computeIoU(
        boxA: RectF,
        boxB: RectF
    ): Float {

        val left = maxOf(boxA.left, boxB.left)
        val top = maxOf(boxA.top, boxB.top)
        val right = minOf(boxA.right, boxB.right)
        val bottom = minOf(boxA.bottom, boxB.bottom)

        val intersectionWidth = maxOf(0f, right - left)
        val intersectionHeight = maxOf(0f, bottom - top)

        val intersectionArea =
            intersectionWidth * intersectionHeight

        val areaA =
            boxA.width() * boxA.height()

        val areaB =
            boxB.width() * boxB.height()

        val union =
            areaA + areaB - intersectionArea

        return if (union <= 0f)
            0f
        else
            intersectionArea / union
    }

    /**
     * Removes duplicate detections using Non-Maximum Suppression (NMS).
     *
     * Detections are first sorted by confidence (highest first).
     * The highest-confidence detection is kept, and any remaining
     * detections with IoU greater than the threshold are discarded.
     */
    private fun nonMaximumSuppression(
        detections: List<Detection>,
        iouThreshold: Float = IOU_THRESHOLD
    ): List<Detection> {

        if (detections.isEmpty()) {
            return emptyList()
        }

        // Highest confidence first
        val sorted = detections
            .sortedByDescending { it.confidence }
            .toMutableList()

        val selected = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {

            // Keep the best detection
            val current = sorted.removeAt(0)
            selected.add(current)

            // Remove overlapping detections
            val iterator = sorted.iterator()

            while (iterator.hasNext()) {

                val candidate = iterator.next()

                // Only suppress detections of the SAME class
                if (candidate.classId != current.classId) {
                    continue
                }

                val iou = computeIoU(
                    current.box,
                    candidate.box
                )

                if (iou > iouThreshold) {

                    Log.d(
                        TAG,
                        "NMS removed detection " +
                                "(conf=${candidate.confidence}) " +
                                "IoU=$iou"
                    )

                    iterator.remove()
                }
            }
        }

        Log.d(
            TAG,
            "NMS kept ${selected.size} detections"
        )

        return selected
    }

    override fun close() {
        interpreter.close()
        Log.d(TAG, "Interpreter closed.")
    }
}