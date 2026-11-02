package com.example.siliguide.core.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import com.example.siliguide.domain.model.PolygonPoint

class LeafSegmenter(
    context: Context,
    modelPath: String
) : Closeable {

    companion object {
        private const val TAG = "LeafSegmenter"

        const val DEFAULT_INPUT_SIZE = 640

        // Number of mask coefficients produced by YOLO
        const val DEFAULT_NUM_MASK_COEFFS = 32

        const val DEFAULT_NUM_DETECTIONS = 300
        const val DEFAULT_DETECTION_DIM = 38
        const val DEFAULT_PROTO_SIZE = 160

        // Detection confidence threshold
        private const val CONF_THRESHOLD = 0.60f

        // Only kept for future use if we need additional NMS
        private const val IOU_THRESHOLD = 0.45f

        // CPU threads for the interpreter. Benchmark values (4, 6, 8) via the "Timing" log.
        private const val NUM_THREADS = 4

        // Box sizes at or below this mean the model outputs normalized (0..1) coordinates
        private const val NORMALIZED_COORD_MAX = 1.5f

        // Longest side of the downscaled mask used for contour extraction
        private const val POLYGON_MAX_SIDE = 256
    }

    /**
     * Single detected leaf
     */
    data class Detection(
        val box: RectF,
        val confidence: Float,
        val classId: Int,
        val coeffs: FloatArray,

        var mask: Bitmap? = null,

        var polygon: List<PolygonPoint> = emptyList()
    )

    private val interpreter: Interpreter

    val inputWidth: Int
    val inputHeight: Int
    val isNCHW: Boolean
    val isRawLiteRT: Boolean

    val numDetections: Int
    val detectionDim: Int
    val numMaskCoeffs: Int
    val numClasses: Int
    val protoHeight: Int
    val protoWidth: Int

    // Letterbox state – populated in preprocess(), consumed in postprocess()
private var letterboxRatio: Float = 1f
    private var letterboxPadLeft: Int = 0
    private var letterboxPadTop: Int = 0
    private var letterboxSrcWidth: Int = 0
    private var letterboxSrcHeight: Int = 0

    private val detectionTensorIndex: Int
    private val protoTensorIndex: Int

    init {

        val options = Interpreter.Options().apply {
            setNumThreads(NUM_THREADS)
        }
        Log.d(TAG, "Interpreter threads=$NUM_THREADS (cores=${Runtime.getRuntime().availableProcessors()})")

        interpreter = Interpreter(
            loadModelFile(context, modelPath),
            options
        )

        // 1. Resolve Input Dimensions & Format (NCHW vs NHWC)
        val inputShape = interpreter.getInputTensor(0).shape()
        val nchw = inputShape.size == 4 && inputShape[1] == 3
        isNCHW = nchw

        inputHeight = when {
            inputShape.size == 4 && nchw && inputShape[2] > 0 -> inputShape[2]
            inputShape.size == 4 && !nchw && inputShape[1] > 0 -> inputShape[1]
            else -> DEFAULT_INPUT_SIZE
        }

        inputWidth = when {
            inputShape.size == 4 && nchw && inputShape[3] > 0 -> inputShape[3]
            inputShape.size == 4 && !nchw && inputShape[2] > 0 -> inputShape[2]
            else -> DEFAULT_INPUT_SIZE
        }

        // 2. Resolve Output Tensors (Detections vs Prototypes)
        var detIdx = 0
        var protoIdx = 1
        var rawLiteRT = false
        var nDetections = DEFAULT_NUM_DETECTIONS
        var detDim = DEFAULT_DETECTION_DIM
        var nMaskCoeffs = DEFAULT_NUM_MASK_COEFFS
        var pHeight = inputHeight / 4
        var pWidth = inputWidth / 4

        for (i in 0 until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(i)
            val shape = tensor.shape()
            if (shape.size == 3) {
                detIdx = i
                if (shape[2] > shape[1] && shape[2] >= 1000) {
                    // Modern Google LiteRT native output: e.g. [1, 40, 8400]
                    rawLiteRT = true
                    detDim = shape[1]      // 40 channels (coords + classes + mask coeffs)
                    nDetections = shape[2] // 8400 spatial anchors
                } else {
                    // Legacy NMS output: e.g. [1, 300, 38]
                    rawLiteRT = false
                    if (shape[1] > 0) nDetections = shape[1]
                    if (shape[2] > 0) detDim = shape[2]
                }
            } else if (shape.size == 4) {
                // Prototypes tensor: e.g. [1, 32, 160, 160] (NCHW) or [1, 160, 160, 32] (NHWC)
                protoIdx = i
                if (shape[1] in 1..64) {
                    if (shape[1] > 0) nMaskCoeffs = shape[1]
                    if (shape[2] > 0) pHeight = shape[2]
                    if (shape[3] > 0) pWidth = shape[3]
                } else {
                    if (shape[1] > 0) pHeight = shape[1]
                    if (shape[2] > 0) pWidth = shape[2]
                    if (shape[3] > 0) nMaskCoeffs = shape[3]
                }
            }
        }

        isRawLiteRT = rawLiteRT
        detectionTensorIndex = detIdx
        protoTensorIndex = protoIdx
        numDetections = nDetections
        detectionDim = detDim
        numMaskCoeffs = nMaskCoeffs
        protoHeight = pHeight
        protoWidth = pWidth
        numClasses = if (rawLiteRT) (detDim - 4 - nMaskCoeffs).coerceAtLeast(1) else 4

        logModelInfo()
    }

    // Reusable buffers – allocated once, reused by every segment() call.
    // Safe because segment() is @Synchronized and results are copied out before it returns.
    private val inputTensorBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(4 * 3 * inputWidth * inputHeight).order(ByteOrder.nativeOrder())
    private val pixelBuffer = IntArray(inputWidth * inputHeight)
    private val letterboxBitmap: Bitmap =
        Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
    private val letterboxCanvas = Canvas(letterboxBitmap)
    private val letterboxPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private val outputPrototypes = Array(1) {
        Array(numMaskCoeffs) { Array(protoHeight) { FloatArray(protoWidth) } }
    }
    private val outputRaw: Array<Array<FloatArray>>? =
        if (isRawLiteRT) Array(1) { Array(detectionDim) { FloatArray(numDetections) } } else null
    private val outputLegacy: Array<Array<FloatArray>>? =
        if (!isRawLiteRT) Array(1) { Array(numDetections) { FloatArray(detectionDim) } } else null

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
            "Configured dimensions: input=${inputWidth}x${inputHeight} (layout=${if (isNCHW) "NCHW" else "NHWC"}), " +
                    "detections tensor=$detectionTensorIndex (shape=[1, ${if (isRawLiteRT) "$detectionDim, $numDetections" else "$numDetections, $detectionDim"}]), " +
                    "proto tensor=$protoTensorIndex [1, $numMaskCoeffs, $protoHeight, $protoWidth], " +
                    "isRawLiteRT=$isRawLiteRT (classes=$numClasses)"
        )

        Log.d(
            TAG,
            "Interpreter initialized successfully."
        )
    }

    /**
     * Runs YOLO instance segmentation.
     */
    @Synchronized
    fun segment(
        bitmap: Bitmap
    ): List<Detection> {

        Log.d(
            TAG,
            "Starting segmentation on ${bitmap.width}x${bitmap.height}"
        )

        val t0 = System.nanoTime()
        val inputBuffer = preprocess(bitmap)
        val t1 = System.nanoTime()

        // Output Prototypes: [1, numMaskCoeffs, protoHeight, protoWidth]
        val outputs = HashMap<Int, Any>()
        outputs[protoTensorIndex] = outputPrototypes

        val format: String
        val t2: Long
        val detections: List<Detection> = if (isRawLiteRT) {
            // LiteRT native tensor shape: [1, detectionDim, numDetections] e.g. [1, 40, 8400]
            val outputRaw = outputRaw!!
            outputs[detectionTensorIndex] = outputRaw
            format = "LiteRT native"

            interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
            t2 = System.nanoTime()

            postProcessRawLiteRT(
                outputRaw[0],
                outputPrototypes[0],
                bitmap.width,
                bitmap.height
            )
        } else {
            // Legacy tensor shape: [1, numDetections, detectionDim] e.g. [1, 300, 38]
            val outputLegacy = outputLegacy!!
            outputs[detectionTensorIndex] = outputLegacy
            format = "Legacy"

            interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
            t2 = System.nanoTime()

            postProcessLegacy(
                outputLegacy[0],
                outputPrototypes[0],
                bitmap.width,
                bitmap.height
            )
        }
        val t3 = System.nanoTime()

        Log.d(
            TAG,
            "Timing [$format]: preprocess=${(t1 - t0) / 1_000_000} ms, " +
                    "inference=${(t2 - t1) / 1_000_000} ms, " +
                    "postprocess=${(t3 - t2) / 1_000_000} ms, " +
                    "total=${(t3 - t0) / 1_000_000} ms, " +
                    "detections=${detections.size}"
        )

        return detections
    }

    /**
     * Converts Bitmap to normalized Float32 Tensor Buffer using YOLO Letterboxing.
     *
     * Letterboxing preserves the original aspect ratio by:
     *   1. Scaling proportionally so the longest edge fits within [inputWidth x inputHeight].
     *   2. Padding the remaining space with neutral gray (RGB 114, 114, 114).
     *
     * The letterbox parameters (ratio, padLeft, padTop) are stored as instance fields
     * so that postprocess() can invert them and map detections back to original image space.
     */
    private fun preprocess(
        bitmap: Bitmap
    ): ByteBuffer {

        // 1. Compute uniform scale ratio so the image fits inside the tensor without cropping
        val ratio = minOf(
            inputWidth.toFloat() / bitmap.width,
            inputHeight.toFloat() / bitmap.height
        )
        val scaledW = (bitmap.width * ratio).toInt()
        val scaledH = (bitmap.height * ratio).toInt()

        // 2. Centre-pad offsets (symmetric, integer pixels)
        val padLeft = (inputWidth - scaledW) / 2
        val padTop  = (inputHeight - scaledH) / 2

        // 3. Persist so postprocess() can invert the transform
        letterboxRatio   = ratio
        letterboxPadLeft = padLeft
        letterboxPadTop  = padTop
        letterboxSrcWidth  = bitmap.width
        letterboxSrcHeight = bitmap.height

        Log.d(TAG, "Letterbox: ratio=$ratio scaledW=$scaledW scaledH=$scaledH padLeft=$padLeft padTop=$padTop")

        // 4. Reuse the gray canvas (Ultralytics default fill = 114, 114, 114)
        letterboxCanvas.drawColor(Color.rgb(114, 114, 114))

        // 5. Draw the image scaled into the centred destination rect (no intermediate bitmap)
        letterboxCanvas.drawBitmap(
            bitmap,
            null,
            Rect(padLeft, padTop, padLeft + scaledW, padTop + scaledH),
            letterboxPaint
        )

        // 6. Read pixels from the letterboxed canvas into the reusable buffers
        val buffer = inputTensorBuffer
        buffer.clear()

        val pixels = pixelBuffer
        letterboxBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        // 7. Encode to tensor (NCHW planar or NHWC interleaved)
        if (isNCHW) {
            // Planar format: all R, then all G, then all B
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
        } else {
            // Interleaved format: R, G, B per pixel
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                buffer.putFloat((pixel and 0xFF) / 255f)
            }
        }

        buffer.rewind()

        return buffer
    }

    /**
     * Inverts the letterbox transform, mapping a bounding box from tensor/640 space
     * back to original image pixel coordinates.
     *
     * Formula: original_coord = (tensor_coord - pad) / ratio
     */
    private fun invertLetterbox(x1: Float, y1: Float, x2: Float, y2: Float): RectF {
        val maxX = letterboxSrcWidth.toFloat()
        val maxY = letterboxSrcHeight.toFloat()
        return RectF(
            ((x1 - letterboxPadLeft) / letterboxRatio).coerceIn(0f, maxX),
            ((y1 - letterboxPadTop)  / letterboxRatio).coerceIn(0f, maxY),
            ((x2 - letterboxPadLeft) / letterboxRatio).coerceIn(0f, maxX),
            ((y2 - letterboxPadTop)  / letterboxRatio).coerceIn(0f, maxY)
        )
    }

    /**
     * True when every width/height value is <= [NORMALIZED_COORD_MAX], i.e. the model
     * emits normalized (0..1) coordinates instead of tensor-pixel coordinates.
     */
    private fun isNormalizedCoords(widths: FloatArray, heights: FloatArray): Boolean {
        for (i in widths.indices) {
            if (widths[i] > NORMALIZED_COORD_MAX || heights[i] > NORMALIZED_COORD_MAX) return false
        }
        return true
    }

    /**
     * Decodes native LiteRT YOLO segmentation outputs [channels][anchors] e.g. [40][8400].
     *
     * Channels layout:
     * - [0..3]: [cx, cy, w, h] box center and dimensions (normalized 0..1 by Ultralytics)
     * - [4 .. 4 + numClasses - 1]: class confidence probabilities
     * - [4 + numClasses .. 4 + numClasses + numMaskCoeffs - 1]: prototype mask coefficients (32)
     */
    private fun postProcessRawLiteRT(
        raw: Array<FloatArray>, // [detectionDim][numDetections] e.g. [40][8400]
        protos: Array<Array<FloatArray>>,
        imgW: Int,
        imgH: Int
    ): List<Detection> {

        val detections = mutableListOf<Detection>()
        val coeffStart = 4 + numClasses

        // Decide once per output whether coords are normalized (0..1) or pixel-space.
        // Checked over every anchor: a pixel-space box width/height can't all stay <= 1.5.
        val normalized = isNormalizedCoords(raw[2], raw[3])
        val scaleX = if (normalized) inputWidth.toFloat() else 1f
        val scaleY = if (normalized) inputHeight.toFloat() else 1f

        for (a in 0 until numDetections) {

            // 1. Find highest scoring class among the model's classes
            var maxConfidence = -1f
            var bestClassId = -1

            for (c in 0 until numClasses) {
                val score = raw[4 + c][a]
                if (score > maxConfidence) {
                    maxConfidence = score
                    bestClassId = c
                }
            }

            if (maxConfidence < CONF_THRESHOLD) {
                continue
            }

            // 2. Decode coordinates [cx, cy, w, h] from channels 0, 1, 2, 3
            val cx = raw[0][a]
            val cy = raw[1][a]
            val w = raw[2][a]
            val h = raw[3][a]

            // Convert to input-tensor pixel space regardless of normalization
            val x1_t = (cx - w / 2f) * scaleX
            val y1_t = (cy - h / 2f) * scaleY
            val x2_t = (cx + w / 2f) * scaleX
            val y2_t = (cy + h / 2f) * scaleY

            // Invert letterbox -> original image coordinates
            val box = invertLetterbox(x1_t, y1_t, x2_t, y2_t)

            // 3. Extract mask coefficients from channels (4 + numClasses .. 4 + numClasses + numMaskCoeffs - 1)
            val coeffs = FloatArray(numMaskCoeffs) { i ->
                raw[coeffStart + i][a]
            }

            val det = Detection(
                box = box,
                confidence = maxConfidence,
                classId = bestClassId,
                coeffs = coeffs
            )

            detections.add(det)
        }

        Log.d(TAG, "LiteRT parsed detections before NMS = ${detections.size}")

        val filteredDetections = nonMaximumSuppression(detections)
        generateMasksAndPolygons(filteredDetections, protos, imgW, imgH)

        Log.d(TAG, "LiteRT parsed detections after NMS = ${filteredDetections.size}")

        return filteredDetections
    }

    /**
     * Decodes legacy model output format [numDetections][detectionDim] e.g. [300][38].
     */
    private fun postProcessLegacy(
        results: Array<FloatArray>,
        protos: Array<Array<FloatArray>>,
        imgW: Int,
        imgH: Int
    ): List<Detection> {

        val detections = mutableListOf<Detection>()

        // Decide once per output whether coords are normalized (0..1) or pixel-space
        var maxCoord = 0f
        for (row in results) {
            maxCoord = maxOf(maxCoord, row[2], row[3])
        }
        val normalized = maxCoord <= NORMALIZED_COORD_MAX
        val scaleX = if (normalized) inputWidth.toFloat() else 1f
        val scaleY = if (normalized) inputHeight.toFloat() else 1f

        results.forEachIndexed { index, detection ->

            val x1 = detection[0] * scaleX
            val y1 = detection[1] * scaleY
            val x2 = detection[2] * scaleX
            val y2 = detection[3] * scaleY

            val confidence = detection[4]
            val classId = detection[5].toInt()

            if (confidence < CONF_THRESHOLD) {
                return@forEachIndexed
            }

            // Legacy coords are in tensor pixel space (0..inputWidth/Height);
            // invert letterbox to map back to original image coordinates.
            val box = invertLetterbox(x1, y1, x2, y2)

            val coeffs = detection.copyOfRange(
                6,
                6 + numMaskCoeffs
            )

            val det = Detection(
                box = box,
                confidence = confidence,
                classId = classId,
                coeffs = coeffs
            )

            detections.add(det)
        }

        Log.d(TAG, "Legacy parsed detections before NMS = ${detections.size}")

        val filteredDetections = nonMaximumSuppression(detections)
        generateMasksAndPolygons(filteredDetections, protos, imgW, imgH)

        Log.d(TAG, "Legacy parsed detections after NMS = ${filteredDetections.size}")

        return filteredDetections
    }

    /**
     * Reconstructs, upsamples, crops, and vectorizes prototype masks for filtered detections.
     */
    private fun generateMasksAndPolygons(
        detections: List<Detection>,
        protos: Array<Array<FloatArray>>,
        imgW: Int,
        imgH: Int
    ) {
        // Unpadded region of the proto mask, in proto coordinates (inverts the letterbox)
        val protoScaleX = protoWidth.toFloat() / inputWidth
        val protoScaleY = protoHeight.toFloat() / inputHeight
        val regionLeft = letterboxPadLeft * protoScaleX
        val regionTop = letterboxPadTop * protoScaleY
        val regionWidth = (imgW * letterboxRatio).toInt() * protoScaleX
        val regionHeight = (imgH * letterboxRatio).toInt() * protoScaleY

        val stepX = regionWidth / imgW
        val stepY = regionHeight / imgH

        for (det in detections) {

            // Mask logits at proto resolution (sigmoid(x) >= 0.5 <=> x >= 0)
            val logits = reconstructMask(det.coeffs, protos)

            // Only the pixels inside the detection box can be foreground
            val left = det.box.left.toInt().coerceIn(0, imgW - 1)
            val top = det.box.top.toInt().coerceIn(0, imgH - 1)
            val right = det.box.right.toInt().coerceIn(left, imgW)
            val bottom = det.box.bottom.toInt().coerceIn(top, imgH)
            val boxW = right - left
            val boxH = bottom - top

            // Full-size ARGB buffer (renderer draws the mask at 0,0); only the box area is filled
            val pixels = IntArray(imgW * imgH)

            for (y in top until bottom) {
                for (x in left until right) {
                    if (sampleLogit(logits, x, y, regionLeft, regionTop, stepX, stepY) >= 0f) {
                        pixels[y * imgW + x] = Color.CYAN
                    }
                }
            }

            val bitmap = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, imgW, 0, 0, imgW, imgH)
            det.mask = bitmap

            det.polygon = if (boxW > 0 && boxH > 0) {
                extractPolygonDownscaled(pixels, imgW, left, top, boxW, boxH)
            } else {
                emptyList()
            }

            Log.d(
                TAG,
                "Generated mask: ${bitmap.width}x${bitmap.height} (box ${boxW}x${boxH}), polygon points: ${det.polygon.size}"
            )
        }
    }

    /**
     * Bilinear sample of the proto-resolution logit mask at an original-image pixel.
     */
    private fun sampleLogit(
        logits: Array<FloatArray>,
        x: Int,
        y: Int,
        regionLeft: Float,
        regionTop: Float,
        stepX: Float,
        stepY: Float
    ): Float {

        val srcHeight = logits.size
        val srcWidth = logits[0].size

        val srcX = (regionLeft + (x + 0.5f) * stepX - 0.5f).coerceIn(0f, srcWidth - 1f)
        val srcY = (regionTop + (y + 0.5f) * stepY - 0.5f).coerceIn(0f, srcHeight - 1f)

        val x0 = srcX.toInt()
        val y0 = srcY.toInt()
        val x1 = (x0 + 1).coerceAtMost(srcWidth - 1)
        val y1 = (y0 + 1).coerceAtMost(srcHeight - 1)
        val wx = srcX - x0
        val wy = srcY - y0

        val top = logits[y0][x0] * (1f - wx) + logits[y0][x1] * wx
        val bottom = logits[y1][x0] * (1f - wx) + logits[y1][x1] * wx

        return top * (1f - wy) + bottom * wy
    }

    /**
     * Extracts the polygon from a downscaled copy (longest side <= [POLYGON_MAX_SIDE]) of the
     * box region, then maps the points back to original image coordinates.
     */
    private fun extractPolygonDownscaled(
        pixels: IntArray,
        imgW: Int,
        left: Int,
        top: Int,
        boxW: Int,
        boxH: Int
    ): List<PolygonPoint> {

        val factor = minOf(1f, POLYGON_MAX_SIDE.toFloat() / maxOf(boxW, boxH))
        val smallW = maxOf(1, (boxW * factor).toInt())
        val smallH = maxOf(1, (boxH * factor).toInt())

        val small = IntArray(smallW * smallH)
        for (sy in 0 until smallH) {
            val y = top + ((sy + 0.5f) * boxH / smallH).toInt().coerceAtMost(boxH - 1)
            for (sx in 0 until smallW) {
                val x = left + ((sx + 0.5f) * boxW / smallW).toInt().coerceAtMost(boxW - 1)
                small[sy * smallW + sx] = pixels[y * imgW + x]
            }
        }

        val smallBitmap = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
        smallBitmap.setPixels(small, 0, smallW, 0, 0, smallW, smallH)

        val polygon = ContourExtractor.extractPolygon(smallBitmap)
        smallBitmap.recycle()

        val scaleX = boxW.toFloat() / smallW
        val scaleY = boxH.toFloat() / smallH

        return polygon.map {
            PolygonPoint(
                x = left + it.x * scaleX,
                y = top + it.y * scaleY
            )
        }
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

                for (c in 0 until numMaskCoeffs) {
                    value += coeffs[c] * protos[c][y][x]
                }

                mask[y][x] = value
            }
        }

        return mask
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

    @Synchronized
    override fun close() {
        interpreter.close()
        letterboxBitmap.recycle()
        Log.d(TAG, "Interpreter closed.")
    }
}
