package com.duzi.cameraxsample.utils

import android.graphics.Matrix
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.lang.ref.WeakReference

class AutoFitPreviewBuilder private constructor(
    config: PreviewConfig, viewFinderRef: WeakReference<TextureView>) {

    val useCase: Preview

    init {
        val viewFinder = viewFinderRef.get() ?:
                throw IllegalArgumentException("레퍼런스 찾을 수 없음")

        useCase = Preview(config)

        useCase.onPreviewOutputUpdateListener = Preview.OnPreviewOutputUpdateListener {
            val viewFinder =
                viewFinderRef.get() ?: return@OnPreviewOutputUpdateListener

            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture

            updateTransform(viewFinder)
        }

        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val viewFinder = view as TextureView
            updateTransform(viewFinder)
        }

    }

    private fun updateTransform(viewFinder: TextureView) {
        val matrix = Matrix()
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotation = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotation.toFloat(), centerX, centerY)
        viewFinder.setTransform(matrix)
    }

    companion object {

        fun getRotation(rotationCompensation: Int) : Int{
            return when (rotationCompensation) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> FirebaseVisionImageMetadata.ROTATION_0
            }
        }


        fun build(config: PreviewConfig, viewFinder: TextureView) =
            AutoFitPreviewBuilder(config, WeakReference(viewFinder)).useCase
    }
}