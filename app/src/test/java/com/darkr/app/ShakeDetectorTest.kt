package com.darkr.app

import com.darkr.app.sensor.ShakeDetector
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class ShakeDetectorTest {

    @Test
    fun testShakeThresholds() {
        assertEquals(13.5f, ShakeDetector.DEFAULT_SHAKE_THRESHOLD, 0.01f)
        assertEquals(1500L, ShakeDetector.SHAKE_COOLDOWN_MS)
        assertEquals(80L, ShakeDetector.SAMPLE_INTERVAL_MS)
    }

    @Test
    fun testAccelerationCalculationLogic() {
        val deltaX = 12f
        val deltaY = 8f
        val deltaZ = 5f
        val diffTime = 80L

        val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / diffTime * 10000
        val thresholdScaled = ShakeDetector.DEFAULT_SHAKE_THRESHOLD * 100

        assertTrue("High acceleration should exceed threshold", speed > thresholdScaled)

        val smallDeltaX = 0.2f
        val smallDeltaY = 0.1f
        val smallDeltaZ = 0.1f
        val smallSpeed = sqrt((smallDeltaX * smallDeltaX + smallDeltaY * smallDeltaY + smallDeltaZ * smallDeltaZ).toDouble()) / diffTime * 10000

        assertFalse("Subtle movement should NOT trigger shake", smallSpeed > thresholdScaled)
    }
}
