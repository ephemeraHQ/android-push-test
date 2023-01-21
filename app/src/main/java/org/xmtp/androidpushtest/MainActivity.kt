package org.xmtp.androidpushtest

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNotificationTokenManager.init(this)
        GlobalScope.launch(Dispatchers.IO) {
            PushNotificationTokenManager.ensurePushTokenIsConfigured()
        }
        setContentView(R.layout.activity_main)

    // Uncomment this at least once to set the permissions up for requesting action overlay
        // requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        myIntent.data = Uri.parse("package:$packageName")
        startActivityForResult(myIntent, PERMISSION_REQUEST_CODE)
    }
}