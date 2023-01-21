package org.xmtp.androidpushtest

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PushNotificationsService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "PushNotificationService"

        internal const val CHANNEL_ID = "xmtp_android_push_test"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Note: Notifications must be data messages so that this is called on all notifications
     *
     * https://firebase.google.com/docs/cloud-messaging/android/receive#handling_messages
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "On message received.")
        val webAppInt = WebAppInterface(this@PushNotificationsService)

        val handlerThread = object : HandlerThread("WEBVIEW_LOOPER") {
            @SuppressLint("SetJavaScriptEnabled")
            override fun onLooperPrepared() {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                val params = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )

                params.gravity = Gravity.TOP or Gravity.START
                params.x = 0
                params.y = 0
                params.width = 0
                params.height = 0

                val wv = WebView(this@PushNotificationsService)

                wv.webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        Log.d("Error", "loading web view: request: $request error: $error")
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return if (request.url.toString().contains("/endProcess")) {
                            windowManager.removeView(wv)
                            wv.post { wv.destroy() }
                            stopSelf()
                            WebResourceResponse("bgsType", "someEncoding", null)
                        } else {
                            null
                        }
                    }
                }
                wv.settings.userAgentString = System.getProperty("http.agent") + "MyCloud"
                wv.loadUrl("file:///android_asset/main.html")

                wv.settings.javaScriptEnabled = true

                wv.addJavascriptInterface(webAppInt, "Android");
                windowManager.addView(wv, params)
            }
        }

        if (!handlerThread.isAlive) {
            handlerThread.start()
        }

        GlobalScope.launch(Dispatchers.Main) {
            webAppInt.data.observeForever { addedNumber ->
                val pendingIntent = PendingIntent.getActivity(
                    this@PushNotificationsService,
                    0,
                    Intent(this@PushNotificationsService, MainActivity::class.java).apply {
                        putExtra(
                            "number",
                            addedNumber
                        )
                    },
                    (PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )

                val builder = NotificationCompat.Builder(this@PushNotificationsService, CHANNEL_ID)
                    .setContentTitle(addedNumber)
                    .setContentText(addedNumber)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(addedNumber))
                    .setContentIntent(pendingIntent)

                // Use the number as the ID for now until one is passed back from the server.
                NotificationManagerCompat.from(this@PushNotificationsService).apply {
                    notify(addedNumber.hashCode(), builder.build())
                }
            }
        }
    }
}

class WebAppInterface internal constructor(ctx: Context) {
    var mContext: Context
    private val _data: MutableLiveData<String> =
        MutableLiveData()
    val data: LiveData<String>
        get() = _data

    init {
        mContext = ctx
    }

    @JavascriptInterface
    fun sendData(number: String?) {
        _data.postValue(number)
    }
}