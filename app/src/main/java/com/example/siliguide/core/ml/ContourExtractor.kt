package com.example.siliguide.core.ml

import com.example.siliguide.domain.model.PolygonPoint
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.imgproc.Imgproc
import android.graphics.Bitmap

object ContourExtractor {

    fun extractPolygon(
        bitmap: Bitmap
    ): List<PolygonPoint> {

        val src = Mat()

        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()

        if (src.channels() == 4) {
            Imgproc.cvtColor(
                src,
                gray,
                Imgproc.COLOR_RGBA2GRAY
            )
        } else {
            Imgproc.cvtColor(
                src,
                gray,
                Imgproc.COLOR_RGB2GRAY
            )
        }

        val contours = mutableListOf<MatOfPoint>()

        val hierarchy = Mat()

        Imgproc.findContours(
            gray,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) {
            return emptyList()
        }

        val largest =
            contours.maxByOrNull {
                Imgproc.contourArea(it)
            } ?: return emptyList()

        val curve = MatOfPoint2f(
            *largest.toArray()
        )

        val approx = MatOfPoint2f()

        Imgproc.approxPolyDP(
            curve,
            approx,
            2.0,
            true
        )

        return approx.toArray().map {

            PolygonPoint(
                x = it.x.toFloat(),
                y = it.y.toFloat()
            )
        }
    }
}