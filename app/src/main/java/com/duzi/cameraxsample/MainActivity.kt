package com.duzi.cameraxsample

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.duzi.cameraxsample.fragment.CameraFragment
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionlistener = object : PermissionListener {
            override fun onPermissionGranted() {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CameraFragment())
                    .commitAllowingStateLoss()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {

            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("취소하면 설정에서 설정")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
    }
}
