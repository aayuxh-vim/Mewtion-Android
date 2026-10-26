package com.example.mewtion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class SensorBridgeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var linearAccel: Sensor? = null
    private var gyroscope: Sensor? = null

    @Volatile private var ax = 0f
    @Volatile private var ay = 0f
    @Volatile private var az = 0f
    @Volatile private var gx = 0f
    @Volatile private var gy = 0f
    @Volatile private var gz = 0f

    private var serverSocket: ServerSocket? = null
    @Volatile private var isRunning = true

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Register both sensors at SENSOR_DELAY_GAME (~50Hz - 60Hz)
        linearAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        startServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun startAsForeground() {
        val channelId = "sensor_bridge_channel"
        val channelName = "Sensor Bridge Service"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mewtion Bridge Active")
            .setContentText("Streaming sensor data...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startServer() {
        thread {
            serverSocket = ServerSocket(8765)
            while (isRunning) {
                try {
                    val socket: Socket = serverSocket?.accept() ?: break
                    val output: OutputStream = socket.getOutputStream()

                    while (isRunning && socket.isConnected && !socket.isClosed) {
                        val payload = String.format(
                            "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            ax, ay, az, gx, gy, gz
                        )
                        output.write(payload.toByteArray(Charsets.UTF_8))
                        output.flush()
                        Thread.sleep(16) // ~60 Hz tick
                    }
                } catch (e: Exception) {
                    // Connection closed or reset; loop back to accept next connection
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                ax = event.values[0]
                ay = event.values[1]
                az = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                gx = event.values[0]
                gy = event.values[1]
                gz = event.values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        isRunning = false
        sensorManager.unregisterListener(this)
        try { serverSocket?.close() } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}