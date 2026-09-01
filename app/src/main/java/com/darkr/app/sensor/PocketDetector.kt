package com.darkr.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Calibrated Smart Pocket & Proximity Detector.
 * Automatically detects when device is placed into a pocket or laid face-down on a surface,
 * triggering battery-saving AMOLED blackout and touch freeze.
 */
class PocketDetector(
    context: Context,
    private val onPocketStateChanged: (isInPocket: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var isNear = false
    private var isDeviceInvertedOrFaceDown = false
    private var currentlyInPocket = false

    private var lastStateChangeTimestamp: Long = 0
    private val debounceDelayMs: Long = 600

    fun startListening(): Boolean {
        if (isListening || sensorManager == null || proximitySensor == null) return false

        isListening = true
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        return true
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        isNear = false
        isDeviceInvertedOrFaceDown = false
        currentlyInPocket = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                // Binary proximity or distance less than 4cm
                isNear = distance < maxRange && distance < 4.0f
                evaluatePocketState()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val y = event.values[1] // Y axis (vertical: -9.8 = upside down)
                val z = event.values[2] // Z axis (face-down: -9.8)

                // In pocket typically means upside down (Y < -4) or face down (Z < -4) or tilted down
                val isUpsideDown = y < -4.0f
                val isFaceDown = z < -4.0f

                isDeviceInvertedOrFaceDown = isUpsideDown || isFaceDown || abs(z) > 7.0f
                evaluatePocketState()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun evaluatePocketState() {
        val now = System.currentTimeMillis()
        if (now - lastStateChangeTimestamp < debounceDelayMs) return

        // If proximity is near AND either inverted/face-down or close proximity
        val shouldBeInPocket = isNear

        if (shouldBeInPocket != currentlyInPocket) {
            currentlyInPocket = shouldBeInPocket
            lastStateChangeTimestamp = now
            onPocketStateChanged(currentlyInPocket)
        }
    }
}
