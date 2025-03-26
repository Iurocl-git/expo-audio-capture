package expo.modules.audiocapture

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
//import expo.modules.core.interfaces.ActivityEventListener
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ActivityEventListener

private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
class ExpoAudioCaptureModule : Module(), ActivityEventListener {

  private var mediaProjectionManager: MediaProjectionManager? = null
  private var mediaProjection: MediaProjection? = null
  private var recorder: AudioRecord? = null
  private var recordingJob: Job? = null
  private var isRecording = false
  private var udpSocket: DatagramSocket? = null
  private var ipAddress: InetAddress? = null
  private var udpPort: Int = 8888
  private var previousData = DoubleArray(1024)
  private var broadcastReceiver: BroadcastReceiver? = null
  private var isReceiverRegistered = false

  private val SCREEN_CAPTURE_REQUEST_CODE = 1001

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun definition() = ModuleDefinition {
    Name("ExpoAudioCapture")
    Events("onFftData")

    OnCreate {
      val activity = appContext.currentActivity
      val context = appContext.reactContext

      Log.d("AudioCapture", "Module created")

      (context as? ReactApplicationContext)?.addActivityEventListener(this@ExpoAudioCaptureModule)

      // Инициализируем и регистрируем BroadcastReceiver сразу
      broadcastReceiver = object : BroadcastReceiver() {
          override fun onReceive(context: Context?, intent: Intent?) {
              Log.d("AudioCapture", "Received broadcast from MediaProjectionService")
              Log.d("AudioCapture", "Intent extras: ${intent?.extras?.keySet()}")
              
              val resultCode = intent?.getIntExtra("resultCode", -1) ?: run {
                  Log.e("AudioCapture", "No resultCode in intent")
                  return
              }
              
              Log.d("AudioCapture", "Got resultCode: $resultCode")
              
              // Получаем оригинальный Intent из onActivityResult
              val originalData = intent?.getParcelableExtraCompat<Intent>("data")
              if (originalData == null) {
                  Log.e("AudioCapture", "No data in broadcast intent")
                  return
              }
              
              Log.d("AudioCapture", "Got original data intent")
              mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, originalData)
              
              if (mediaProjection != null) {
                  Log.d("AudioCapture", "MediaProjection created successfully")
                  startRecording()
              } else {
                  Log.e("AudioCapture", "Failed to create MediaProjection")
              }
          }
      }

      // Регистрируем BroadcastReceiver сразу
      try {
          val filter = IntentFilter(MediaProjectionService.ACTION_MEDIA_PROJECTION_STARTED)
          context?.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
          isReceiverRegistered = true
          Log.d("AudioCapture", "BroadcastReceiver registered in OnCreate")
      } catch (e: Exception) {
          Log.e("AudioCapture", "Failed to register BroadcastReceiver in OnCreate", e)
          isReceiverRegistered = false
      }
    }

    OnDestroy {
      (appContext.reactContext as? ReactApplicationContext)?.removeActivityEventListener(this@ExpoAudioCaptureModule)
      try {
          if (isReceiverRegistered && broadcastReceiver != null) {
              appContext.reactContext?.unregisterReceiver(broadcastReceiver)
              isReceiverRegistered = false
              Log.d("AudioCapture", "BroadcastReceiver unregistered")
          }
      } catch (e: Exception) {
          Log.e("AudioCapture", "Error unregistering BroadcastReceiver", e)
      }
    }

    Function("setUdpConfig") { ip: String, port: Int ->
      try {
        ipAddress = InetAddress.getByName(ip)
        udpPort = port
        Log.d("AudioCapture", "UDP Config set: IP = $ip, Port = $port")
      } catch (e: Exception) {
        Log.e("AudioCapture", "Invalid IP or Port", e)
      }
    }

    Function("startCapture") {
      val activity = appContext.activityProvider?.currentActivity ?: return@Function null
      val context = appContext.reactContext ?: return@Function null

      Log.d("AudioCapture", "Starting capture process")
      
      // Проверяем и перерегистрируем BroadcastReceiver если нужно
      if (!isReceiverRegistered && broadcastReceiver != null) {
          try {
              val filter = IntentFilter(MediaProjectionService.ACTION_MEDIA_PROJECTION_STARTED)
              context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
              isReceiverRegistered = true
              Log.d("AudioCapture", "BroadcastReceiver re-registered in startCapture")
          } catch (e: Exception) {
              Log.e("AudioCapture", "Failed to re-register BroadcastReceiver", e)
              return@Function null
          }
      }
      
      mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      val captureIntent = mediaProjectionManager!!.createScreenCaptureIntent()
      
      Log.d("AudioCapture", "Launching screen capture intent")
      activity.startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }

    Function("stopCapture") {
      Log.d("AudioCapture", "Stopping capture")
      isRecording = false
      try {
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingJob?.cancel()
        
        // Останавливаем MediaProjection
        mediaProjection?.stop()
        mediaProjection = null
        
        // Останавливаем сервис
        val serviceIntent = Intent(appContext.reactContext, MediaProjectionService::class.java)
        appContext.reactContext?.stopService(serviceIntent)
        
        Log.d("AudioCapture", "Capture stopped successfully")
      } catch (e: Exception) {
        Log.e("AudioCapture", "Error stopping capture", e)
      }
    }
  }

  override fun onActivityResult(
    activity: Activity?,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    Log.d("AudioCapture", "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")
    
    if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
      // Проверяем, что результат успешный (RESULT_OK = -1) и есть данные
      if (resultCode == Activity.RESULT_OK && data != null) {
        try {
          Log.d("AudioCapture", "Starting MediaProjectionService with valid result")
          Log.d("AudioCapture", "Data extras: ${data.extras?.keySet()}")

          val serviceIntent = Intent(appContext.reactContext, MediaProjectionService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
          }

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("AudioCapture", "Starting foreground service")
            appContext.reactContext?.startForegroundService(serviceIntent)
          } else {
            Log.d("AudioCapture", "Starting regular service")
            appContext.reactContext?.startService(serviceIntent)
          }

          // Ждем, пока данные будут готовы в сервисе
          CoroutineScope(Dispatchers.Default).launch {
            var attempts = 0
            while (!MediaProjectionService.isReady() && attempts < 10) {
              delay(100)
              attempts++
              Log.d("AudioCapture", "Waiting for service data, attempt $attempts")
            }

            if (MediaProjectionService.isReady()) {
              val serviceResultCode = MediaProjectionService.getResultCode()
              val serviceData = MediaProjectionService.getData()
              
              if (serviceData != null) {
                Log.d("AudioCapture", "Got data from service")
                mediaProjection = mediaProjectionManager?.getMediaProjection(serviceResultCode, serviceData)
                
                if (mediaProjection != null) {
                  Log.d("AudioCapture", "MediaProjection created successfully")
                  startRecording()
                } else {
                  Log.e("AudioCapture", "Failed to create MediaProjection")
                }
              } else {
                Log.e("AudioCapture", "No data in service")
              }
            } else {
              Log.e("AudioCapture", "Service data not ready after $attempts attempts")
            }
          }
        } catch (e: Exception) {
          Log.e("AudioCapture", "Error starting MediaProjectionService", e)
          sendEvent("onCaptureError", mapOf(
            "error" to "Failed to start service: ${e.message}",
            "resultCode" to resultCode
          ))
        }
      } else {
        val errorMessage = when {
          resultCode == Activity.RESULT_CANCELED -> "User cancelled the request"
          resultCode == Activity.RESULT_FIRST_USER -> "User denied permission"
          data == null -> "No data received"
          else -> "Unknown error"
        }
        Log.e("AudioCapture", "MediaProjection failed: $errorMessage. ResultCode: $resultCode")
        sendEvent("onCaptureError", mapOf(
          "error" to errorMessage,
          "resultCode" to resultCode
        ))
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {
    // No need to handle
  }

  private fun startRecording() {
    try {
      if (mediaProjection == null) {
        Log.e("AudioCapture", "MediaProjection is null")
        return
      }

      val context = appContext.reactContext ?: return
      
      // Проверяем все необходимые разрешения
//      if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
//          != PackageManager.PERMISSION_GRANTED) {
//        Log.e("AudioCapture", "RECORD_AUDIO permission not granted")
//        return
//      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
            != PackageManager.PERMISSION_GRANTED) {
          Log.e("AudioCapture", "FOREGROUND_SERVICE_MEDIA_PROJECTION permission not granted")
          return
        }
      }

      val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
        .build()

      val fhtSize = 1024
      val bufferSize = fhtSize * 2

      Log.d("AudioCapture", "Creating AudioRecord")
      try {
        recorder = AudioRecord.Builder()
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(44100)
              .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setAudioPlaybackCaptureConfig(config)
          .build()
      } catch (e: SecurityException) {
        Log.e("AudioCapture", "SecurityException while creating AudioRecord", e)
        return
      }

      recorder?.startRecording()
      isRecording = true

      Log.d("AudioCapture", "Creating recordingJob")

      recordingJob = CoroutineScope(Dispatchers.Default).launch {
        try {
          val audioBuffer = ShortArray(bufferSize / 2)
          val currentData = DoubleArray(fhtSize)
          val combinedData = DoubleArray(fhtSize * 2)

          while (isRecording) {
            try {
              val readSamples = recorder?.read(audioBuffer, 0, audioBuffer.size) ?: 0
              if (readSamples > 0) {
                for (i in 0 until fhtSize) {
                  currentData[i] = if (i < readSamples) audioBuffer[i].toDouble() else 0.0
                }

                System.arraycopy(previousData, 0, combinedData, 0, fhtSize)
                System.arraycopy(currentData, 0, combinedData, fhtSize, fhtSize)

                performFHT(combinedData)
                System.arraycopy(currentData, 0, previousData, 0, fhtSize)

                val magnitudes = DoubleArray(combinedData.size / 2) { i ->
                  kotlin.math.abs(combinedData[i])
                }

                var lowSum = 0.0
                var midSum = 0.0
                var highSum = 0.0
                val freqResolution = 44100.0 / combinedData.size

                for (i in 0 until fhtSize / 2) {
                  val freq = i * freqResolution
                  when {
                    freq < 250 -> lowSum += magnitudes[i]
                    freq < 2000 -> midSum += magnitudes[i]
                    freq <= 20000 -> highSum += magnitudes[i]
                  }
                }

                val normLow = (lowSum / 230 / 64654 * 255).coerceAtMost(255.0).toInt()
                val normMid = (midSum / 1750 / 8427 * 255).coerceAtMost(255.0).toInt()
                val normHigh = (highSum / 18000 / 1351 * 255).coerceAtMost(255.0).toInt()

                val data = mapOf("low" to normLow, "mid" to normMid, "high" to normHigh)
                sendEvent("onFftData", data)

                sendUdpData(normLow, normMid, normHigh)
                delay(5)
              }
            } catch (e: SecurityException) {
              Log.e("AudioCapture", "SecurityException while reading audio data", e)
              stopRecording()
              break
            }
          }
        } catch (e: Exception) {
          Log.e("AudioCapture", "Error in recording job", e)
          stopRecording()
        }
      }
    } catch (e: Exception) {
      Log.e("AudioCapture", "Error starting recording", e)
      stopRecording()
    }
  }

  private fun stopRecording() {
    Log.d("AudioCapture", "Stopping recording")
    isRecording = false
    try {
      recorder?.stop()
      recorder?.release()
      recorder = null
      recordingJob?.cancel()
      
      // Останавливаем MediaProjection
      mediaProjection?.stop()
      mediaProjection = null
      
      // Останавливаем сервис
      val serviceIntent = Intent(appContext.reactContext, MediaProjectionService::class.java)
      appContext.reactContext?.stopService(serviceIntent)
      
      Log.d("AudioCapture", "Recording stopped successfully")
    } catch (e: Exception) {
      Log.e("AudioCapture", "Error stopping recording", e)
    }
  }

  private fun performFHT(data: DoubleArray) {
    val n = data.size
    var j = 0
    val n2 = n / 2
    for (i in 1 until n - 1) {
      var k = n2
      while (j >= k) {
        j -= k
        k /= 2
      }
      j += k
      if (i < j) {
        val temp = data[i]
        data[i] = data[j]
        data[j] = temp
      }
    }
    var step = 1
    while (step < n) {
      val theta = Math.PI / step
      val wtemp = Math.sin(0.5 * theta)
      val wpr = -2.0 * wtemp * wtemp
      val wpi = Math.sin(theta)
      var wr = 1.0
      var wi = 0.0
      for (m in 0 until step) {
        var i = m
        while (i < n) {
          val j1 = i + step
          val tempr = wr * data[j1]
          data[j1] = data[i] - tempr
          data[i] += tempr
          i += step * 2
        }
        val wtemp2 = wr
        wr = wtemp2 * wpr - wi * wpi + wr
        wi = wi * wpr + wtemp2 * wpi + wi
      }
      step *= 2
    }
  }

  private fun sendUdpData(low: Int, mid: Int, high: Int) {
    try {
      val message = "low:$low, mid:$mid, high:$high"
      val data = message.toByteArray()
      val packet = DatagramPacket(data, data.size, ipAddress, udpPort)
      udpSocket = DatagramSocket()
      udpSocket!!.send(packet)
      udpSocket!!.close()
    } catch (e: Exception) {
      Log.e("AudioCapture", "Error sending UDP packet", e)
    }
  }
}