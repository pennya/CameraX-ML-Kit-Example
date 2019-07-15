package com.duzi.cameraxsample

import android.graphics.Matrix
import android.media.Image
import android.util.Size
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duzi.cameraxsample.model.TextAnalyzerResult
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.concurrent.TimeUnit

class TextDetectAnalyzer: ImageAnalysis.Analyzer {

    private val _visionTextsLiveData = MutableLiveData<TextAnalyzerResult>()
    val visionTextLiveData: LiveData<TextAnalyzerResult>
        get() = _visionTextsLiveData

    var rotation = FirebaseVisionImageMetadata.ROTATION_0
    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {

        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp >=
            TimeUnit.SECONDS.toMillis(1)) {

            lastAnalyzedTimestamp = currentTimestamp

            val img = image.image ?: return
            val visionImage = FirebaseVisionImage.fromMediaImage(img, rotation)
            val recognizer = FirebaseVision.getInstance()
                .onDeviceTextRecognizer

            recognizer.processImage(visionImage)
                .addOnSuccessListener { visionTexts ->

                    _visionTextsLiveData.postValue(
                        TextAnalyzerResult(
                            rotation,
                            visionTexts,
                            Size(640, 480)
                        )
                    )
                }
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    companion object {
        fun calcFitMatrix(result: TextAnalyzerResult, targetView: View, displayDegree: Int): Matrix {
            val resultDegree = when (result.rotation) {
                FirebaseVisionImageMetadata.ROTATION_0 -> 0
                FirebaseVisionImageMetadata.ROTATION_90 -> 90
                FirebaseVisionImageMetadata.ROTATION_180 -> 180
                FirebaseVisionImageMetadata.ROTATION_270 -> 270
                else -> 0
            }

            val degree = displayDegree - resultDegree
            val imageSize = result.imageSize
            val matrix = Matrix()

            val oddRotate = (Math.abs(degree / 90) % 2 == 0)
            val w = (if (oddRotate) imageSize.height else imageSize.width).toFloat()
            val h = (if (oddRotate) imageSize.width else imageSize.height).toFloat()

            val sx = targetView.width.toFloat() / w
            val sy = targetView.height.toFloat() / h
            val scale = Math.max(sx, sy)

            matrix.postScale(1f / imageSize.width, 1f / imageSize.height)
            matrix.postTranslate(-0.5f, -0.5f)
            matrix.postRotate(-degree.toFloat())
            matrix.postScale(w, h)
            matrix.postScale(scale, scale)
            matrix.postTranslate(targetView.width / 2f, targetView.height / 2f)

            return matrix
        }
    }
}