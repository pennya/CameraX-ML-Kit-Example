package com.duzi.cameraxsample

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duzi.cameraxsample.utils.AutoFitPreviewBuilder
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.concurrent.TimeUnit

class ObjectDetectAnalyzer : ImageAnalysis.Analyzer {

    private val _objectDetectTextLiveData =  MutableLiveData<String>()
    val objectDetectTextLiveData: LiveData<String>
        get() =  _objectDetectTextLiveData

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp >=
            TimeUnit.SECONDS.toMillis(1)) {

            lastAnalyzedTimestamp = currentTimestamp

            val y = image.planes[0]
            val u = image.planes[1]
            val v = image.planes[2]

            val Yb = y.buffer.remaining()
            val Ub = u.buffer.remaining()
            val Vb = v.buffer.remaining()

            val data = ByteArray(Yb + Ub + Vb)

            y.buffer.get(data, 0, Yb)
            u.buffer.get(data, Yb, Ub)
            v.buffer.get(data, Yb + Ub, Vb)

            val metadata = FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                .setHeight(image.height)
                .setWidth(image.width)
                .setRotation(AutoFitPreviewBuilder.getRotation(rotationDegrees))
                .build()

            val labelImage = FirebaseVisionImage.fromByteArray(data, metadata)

            val labeler = FirebaseVision.getInstance().onDeviceImageLabeler
            labeler.processImage(labelImage)
                .addOnSuccessListener { labels ->
                    if( labels.size >= 1 ) {
                        _objectDetectTextLiveData.postValue("${labels[0].text} ${labels[0].confidence}")
                    }
                }
        }


    }
}