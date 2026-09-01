package com.darkr.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Hardened accelerometer shake detector.
 * Filters out gravity bias on startup and enforces strict cooldown debounce.
 */
class ShakeDetector(
    context: Context,
    private val onShakeListener: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var isInitialized = false
    private var lastUpdate: Long = 0
    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    companion object {
        const val DEFAULT_SHAKE_THRESHOLD = 13.5f
        const val SHAKE_COOLDOWN_MS = 1500L
        const val SAMPLE_INTERVAL_MS = 80L
    }

    var shakeThreshold: Float = DEFAULT_SHAKE_THRESHOLD

    fun start(): Boolean {
        if (isListening || accelerometer == null || sensorManager == null) return false
        isInitialized = false
        lastUpdate = 0
        lastShakeTime = 0
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = registered
        return registered
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        isInitialized = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val curTime = System.currentTimeMillis()

        if (!isInitialized) {
            lastX = event.values[0]
            lastY = event.values[1]
            lastZ = event.values[2]
            lastUpdate = curTime
            isInitialized = true
            return
        }

        val diffTime = curTime - lastUpdate
        if (diffTime >= SAMPLE_INTERVAL_MS) {
            lastUpdate = curTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ

            val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / diffTime * 10000

            if (speed > shakeThreshold * 100) {
                if (curTime - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = curTime
                    onShakeListener.invoke()
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
