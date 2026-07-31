package com.example.siliguide.core.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.siliguide.domain.model.SegmentationDetection

class DetectionRenderer {

    private val boxPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val labelBackgroundPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 42f
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val maskPaint = Paint().apply {
        isAntiAlias = true
        alpha = 120
    }

    private val polygonPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        alpha = 120
        isAntiAlias = true
    }

    private fun intersectsAny(
        rect: RectF,
        occupiedRects: List<RectF>
    ): Boolean {

        return occupiedRects.any {
            RectF.intersects(rect, it)
        }
    }

    private data class LabelInfo(
        val rect: RectF,
        val text: String,
        val baseline: Float
    )


    /**
     * Creates a transparent overlay containing
     * all instance segmentation masks.
     * Use by live analysis
     */
    fun render(
        imageWidth: Int,
        imageHeight: Int,
        detections: List<LeafSegmenter.Detection>,
        classNames: List<String>
    ): Bitmap {

        val overlay = createBitmap(imageWidth, imageHeight)
        overlay.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(overlay)

        // -----------------------------------------
        // Responsive drawing sizes
        // -----------------------------------------
        val scale = minOf(imageWidth, imageHeight) / 640f

        boxPaint.strokeWidth = (4f * scale).coerceAtLeast(2f)
        textPaint.textSize = (28f * scale).coerceAtLeast(18f)

        val labelPadding = (6f * scale).coerceAtLeast(4f)
        val labelMargin = (4f * scale).coerceAtLeast(2f)

        val occupiedLabelRects = mutableListOf<RectF>()
        val labelsToDraw = mutableListOf<LabelInfo>()

        val sortedDetections = detections.sortedByDescending { it.confidence }

        // ==========================================================
        // PASS 1
        // Draw ALL masks
        // ==========================================================
        sortedDetections.forEach { detection ->

            detection.mask?.let {

                canvas.drawBitmap(
                    it,
                    0f,
                    0f,
                    maskPaint
                )
            }
        }

        // ==========================================================
        // PASS 2
        // Draw ALL boxes and compute label positions
        // ==========================================================
        sortedDetections.forEach { detection ->

            canvas.drawRect(
                detection.box,
                boxPaint
            )

            val confidencePercent = (detection.confidence * 100).toInt()
            val label = "${classNames[detection.classId]} ${confidencePercent}%"

            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize

            val labelWidth = textWidth + labelPadding * 2
            val labelHeight = textHeight + labelPadding * 2

            val left = minOf(
                detection.box.left,
                imageWidth - labelWidth
            ).coerceAtLeast(0f)

            // First choice: above box
            var top = (detection.box.top - labelHeight + 1f).coerceAtLeast(0f)

            var labelRect = RectF(
                left,
                top,
                left + labelWidth,
                top + labelHeight
            )

            // Second choice: below box
            if (intersectsAny(labelRect, occupiedLabelRects)) {

                top = (detection.box.bottom).coerceAtMost(imageHeight - labelHeight)

                labelRect = RectF(
                    left,
                    top,
                    left + labelWidth,
                    top + labelHeight
                )
            }

            // Third choice: move right until free
            while (intersectsAny(labelRect, occupiedLabelRects)) {

                if (labelRect.right + labelWidth + labelMargin > imageWidth) {
                    break
                }

                labelRect.offset(
                    labelWidth + labelMargin,
                    0f
                )
            }

            occupiedLabelRects.add(labelRect)

            val baseline =
                labelRect.top +
                        textHeight +
                        labelPadding * 0.35f

            labelsToDraw.add(
                LabelInfo(
                    rect = labelRect,
                    text = label,
                    baseline = baseline
                )
            )
        }

        // ==========================================================
        // PASS 3
        // Draw ALL labels on top of everything
        // ==========================================================
        labelsToDraw.forEach { label ->

            canvas.drawRect(
                label.rect,
                labelBackgroundPaint
            )

            canvas.drawText(
                label.text,
                label.rect.left + labelPadding,
                label.baseline,
                textPaint
            )
        }

        return overlay
    }


    // used by history analysis that is from database
    fun render(
        imageWidth: Int,
        imageHeight: Int,
        detections: List<SegmentationDetection>
    ): Bitmap {

        val overlay = createBitmap(imageWidth, imageHeight)
        overlay.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(overlay)

        val scale = minOf(imageWidth, imageHeight) / 640f

        boxPaint.strokeWidth = (4f * scale).coerceAtLeast(2f)
        textPaint.textSize = (28f * scale).coerceAtLeast(18f)

        val labelPadding = (6f * scale).coerceAtLeast(4f)
        val labelMargin = (4f * scale).coerceAtLeast(2f)

        val occupiedLabelRects = mutableListOf<RectF>()
        val labelsToDraw = mutableListOf<LabelInfo>()

        val sortedDetections =
            detections.sortedByDescending { it.confidence }

        // ===============================
        // PASS 1
        // Draw polygons
        // ===============================
        sortedDetections.forEach { detection ->

            if (detection.polygon.isNotEmpty()) {

                val path = Path()

                path.moveTo(
                    detection.polygon.first().x,
                    detection.polygon.first().y
                )

                detection.polygon
                    .drop(1)
                    .forEach {

                        path.lineTo(
                            it.x,
                            it.y
                        )
                    }

                path.close()

                canvas.drawPath(
                    path,
                    polygonPaint
                )
            }
        }

        // ===============================
        // PASS 2
        // Draw boxes
        // ===============================
        sortedDetections.forEach { detection ->

            val box = RectF(
                detection.boundingBox.left,
                detection.boundingBox.top,
                detection.boundingBox.right,
                detection.boundingBox.bottom
            )

            canvas.drawRect(
                box,
                boxPaint
            )

            val confidencePercent =
                (detection.confidence * 100).toInt()

            val label =
                "${detection.label} ${confidencePercent}%"

            val textWidth =
                textPaint.measureText(label)

            val textHeight =
                textPaint.textSize

            val labelWidth =
                textWidth + labelPadding * 2

            val labelHeight =
                textHeight + labelPadding * 2

            val left =
                minOf(
                    box.left,
                    imageWidth - labelWidth
                ).coerceAtLeast(0f)

            var top =
                (box.top - labelHeight + 1f)
                    .coerceAtLeast(0f)

            var labelRect = RectF(
                left,
                top,
                left + labelWidth,
                top + labelHeight
            )

            if (intersectsAny(labelRect, occupiedLabelRects)) {

                top =
                    box.bottom
                        .coerceAtMost(imageHeight - labelHeight)

                labelRect = RectF(
                    left,
                    top,
                    left + labelWidth,
                    top + labelHeight
                )
            }

            while (intersectsAny(labelRect, occupiedLabelRects)) {

                if (labelRect.right + labelWidth + labelMargin > imageWidth)
                    break

                labelRect.offset(
                    labelWidth + labelMargin,
                    0f
                )
            }

            occupiedLabelRects.add(labelRect)

            labelsToDraw.add(
                LabelInfo(
                    rect = labelRect,
                    text = label,
                    baseline =
                        labelRect.top +
                                textHeight +
                                labelPadding * 0.35f
                )
            )
        }

        // ===============================
        // PASS 3
        // Draw labels
        // ===============================
        labelsToDraw.forEach {

            canvas.drawRect(
                it.rect,
                labelBackgroundPaint
            )

            canvas.drawText(
                it.text,
                it.rect.left + labelPadding,
                it.baseline,
                textPaint
            )
        }

        return overlay
    }

}