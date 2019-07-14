package com.duzi.cameraxsample.fragment


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duzi.cameraxsample.KEY_EVENT_ACTION
import com.duzi.cameraxsample.KEY_EVENT_EXTRA
import com.duzi.cameraxsample.R
import com.duzi.cameraxsample.utils.AutoFitPreviewBuilder
import com.duzi.cameraxsample.utils.simulateClick
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var broadcastManager: LocalBroadcastManager

    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)
        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        viewFinder.post {
            updateCameraUi()
            bindCameraUseCases()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        broadcastManager.unregisterReceiver(volumeDownReceiver)
    }

    private fun updateCameraUi() {
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {

        }
    }

    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)

        // image capture
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // image analyze
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()
        }

        // apply camera x
        CameraX.bindToLifecycle(
            viewLifecycleOwner, preview, imageCapture, imageAnalyzer)
    }

    private val volumeDownReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context, p1: Intent) {
            val keyCode = p1.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)
            when(keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                        .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                // Compute average luminance for the image
                val luma = pixels.average()
                // Log the new luma value
                Log.d("CameraXApp", "Average luminosity: $luma")
                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
            }
        }
    }

    companion object {
        private const val TAG = "CameraX-XL-Kit"
    }
}
