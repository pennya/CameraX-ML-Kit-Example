package com.duzi.cameraxsample.model

import android.util.Size
import com.google.firebase.ml.vision.text.FirebaseVisionText

data class TextAnalyzerResult(
    val rotation: Int,
    val visionText: FirebaseVisionText,
    val imageSize: Size
)