package com.duzi.cameraxsample

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duzi.cameraxsample.fragment.CameraFragment
import com.duzi.cameraxsample.utils.FLAGS_FULLSCREEN
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class MainActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout

    val permissionlistener = object : PermissionListener {
        override fun onPermissionGranted() {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CameraFragment())
                .commitAllowingStateLoss()
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_container)

        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("취소하면 설정에서 설정")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
    }

    override fun onResume() {
        super.onResume()
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when(keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }

    }
}
