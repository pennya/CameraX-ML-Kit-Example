package com.duzi.cameraxsample.fragment


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Rational
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duzi.cameraxsample.*
import com.duzi.cameraxsample.R
import com.duzi.cameraxsample.model.TextAnalyzerResult
import com.duzi.cameraxsample.utils.AutoFitPreviewBuilder
import com.duzi.cameraxsample.utils.simulateClick
import com.duzi.cameraxsample.view.GraphicOverlay
import com.duzi.cameraxsample.view.TextGraphic

class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var label: TextView
    private lateinit var graphicOverlay: GraphicOverlay

    private val objectDetectAnalyzer = ObjectDetectAnalyzer()
    private val textDetectAnalyzer = TextDetectAnalyzer()
    private var lensFacing = CameraX.LensFacing.BACK
    private var previewBuilder: AutoFitPreviewBuilder? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observe()
    }

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
        label = container.findViewById(R.id.label)
        graphicOverlay = container.findViewById(R.id.graphic_overlay)
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

    private fun observe() {
        objectDetectAnalyzer.objectDetectTextLiveData
            .observe(this@CameraFragment, Observer {
                label.text = it
            })

        textDetectAnalyzer.visionTextLiveData
            .observe(this@CameraFragment, Observer {
                drawVisionTexts(it)
            })
    }

    private fun updateCameraUi() {
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
            // TODO   save file to directory
        }

        controls.findViewById<Button>(R.id.object_detect_button).setOnClickListener {
            imageAnalyzer?.let { it.analyzer = objectDetectAnalyzer }

            label.visibility = VISIBLE
            graphicOverlay.visibility = GONE
        }

        controls.findViewById<Button>(R.id.text_detect_button).setOnClickListener {
            imageAnalyzer?.let { it.analyzer = textDetectAnalyzer }

            graphicOverlay.clear()
            label.visibility = GONE
            graphicOverlay.visibility = VISIBLE
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

        previewBuilder = AutoFitPreviewBuilder.newInstance(viewFinderConfig, viewFinder)
        preview = previewBuilder?.useCase

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
            // Default
            analyzer = objectDetectAnalyzer
        }

        // apply camera x
        CameraX.bindToLifecycle(
            viewLifecycleOwner, preview, imageCapture, imageAnalyzer)
    }

    private fun drawVisionTexts(result: TextAnalyzerResult) {
        val blocks = result.visionText.textBlocks
        if (blocks.isEmpty()) {
            return
        }

        graphicOverlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic = TextGraphic(graphicOverlay, elements[k])
                    graphicOverlay.add(textGraphic)
                }
            }
        }

        val imageDegree = previewBuilder?.rotationDegrees ?: 0
        val matrix = TextDetectAnalyzer.calcFitMatrix(result, viewFinder, -imageDegree)
        graphicOverlay.matrix = matrix
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

    companion object {
        private const val TAG = "CameraX-XL-Kit"
    }
}
