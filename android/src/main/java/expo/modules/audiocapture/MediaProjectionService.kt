package expo.modules.audiocapture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class MediaProjectionService : Service() {
    companion object {
        const val ACTION_MEDIA_PROJECTION_STARTED = "expo.modules.audiocapture.MEDIA_PROJECTION_STARTED"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "AudioCaptureChannel"
        private var resultCode: Int = -1
        private var data: Intent? = null
        private var isDataReady = false

        fun getResultCode(): Int = resultCode
        fun getData(): Intent? = data
        fun isReady(): Boolean = isDataReady
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d("MediaProjectionService", "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MediaProjectionService", "Service onStartCommand")
        
        if (intent == null) {
            Log.e("MediaProjectionService", "Intent is null")
            stopSelf()
            return START_NOT_STICKY
        }

        resultCode = intent.getIntExtra("resultCode", -1)
        data = intent.getParcelableExtra<Intent>("data")

        Log.d("MediaProjectionService", "Received resultCode: $resultCode")
        Log.d("MediaProjectionService", "Data extras: ${data?.extras?.keySet()}")
        Log.d("MediaProjectionService", "Data class: ${data?.javaClass?.simpleName}")

        // Проверяем, что результат успешный (RESULT_OK = -1) и есть данные
        if (resultCode == -1 && data != null) {
            isDataReady = true
            Log.d("MediaProjectionService", "Data is ready")
        } else {
            val errorMessage = when {
                data == null -> "No data received"
                else -> "Invalid result code: $resultCode"
            }
            Log.e("MediaProjectionService", "Invalid data: $errorMessage")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for audio capture"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("MediaProjectionService", "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Capture")
            .setContentText("Capturing audio from screen")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MediaProjectionService", "Service onDestroy")
        isDataReady = false
    }
}