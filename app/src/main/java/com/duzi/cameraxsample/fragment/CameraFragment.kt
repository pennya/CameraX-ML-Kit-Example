package com.duzi.cameraxsample.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.duzi.cameraxsample.R

class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView

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

        viewFinder.post {
            updateCameraUi()
        }
    }

    private fun updateCameraUi() {
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)
    }
}
