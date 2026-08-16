package com.example.mewtion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.HandlerThread
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "MewtionService"
private const val PORT = 8765
private const val CHANNEL_ID = "MewtionBridgeChannel"
private const val NOTIFICATION_ID = 101

class SensorBridgeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var sensorThread: HandlerThread

    @Volatile private var clientOutputStream: OutputStream? = null
    private var serverThread: Thread? = null
    @Volatile private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mewtion Active")
            .setContentText("Streaming accelerometer data over USB...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        if (accelerometer == null) {
            Log.e(TAG, "No linear acceleration sensor available!")
            stopSelf()
            return
        }

        sensorThread = HandlerThread("SensorThread").apply { start() }
        val sensorHandler = Handler(sensorThread.looper)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, sensorHandler)

        isRunning = true
        serverThread = Thread(::runServerLoop, "TcpServerThread").apply { start() }
    }

    private fun runServerLoop() {
        val serverSocket = try {
            ServerSocket(PORT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open server socket on port $PORT", e)
            return
        }

        Log.d(TAG, "TCP server listening on port $PORT")

        while (isRunning) {
            val client: Socket = try {
                serverSocket.accept()
            } catch (e: Exception) {
                if (!isRunning) break
                Log.e(TAG, "accept() failed", e)
                continue
            }

            Log.d(TAG, "Laptop connected: ${client.inetAddress}")
            clientOutputStream = client.getOutputStream()

            while (isRunning && clientOutputStream != null) {
                try {
                    Thread.sleep(200)
                } catch (_: InterruptedException) {
                    break
                }
            }

            try {
                client.close()
            } catch (_: Exception) {
            }
        }

        try {
            serverSocket.close()
        } catch (_: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val out = clientOutputStream ?: return
        val (x, y, z) = event.values
        val payload = ByteBuffer.allocate(12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putFloat(x)
            .putFloat(y)
            .putFloat(z)
            .array()

        try {
            out.write(payload)
            out.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Write failed, dropping client", e)
            clientOutputStream = null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        sensorManager.unregisterListener(this)
        if (::sensorThread.isInitialized) {
            sensorThread.quitSafely()
        }
        clientOutputStream = null
        serverThread?.interrupt()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mewtion Background Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps sensor bridge active in the background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}