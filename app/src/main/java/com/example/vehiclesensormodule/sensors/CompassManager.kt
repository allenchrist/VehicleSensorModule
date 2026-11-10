package com.example.vehiclesensormodule.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Works out the heading from the magnetic field and gravity. Gravity comes
 * from AccelerometerManager, so the accelerometer is only listened to once.
 */
class CompassManager(context: Context) : SensorEventListener {

    private companion object {
        // Each reading moves the heading this far towards the new value. It
        // settles in well under a second, but small jitter is smoothed out.
        const val SMOOTHING = 0.5f
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val magnetometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var onHeadingChanged: ((Float) -> Unit)? = null

    private val smoother = HeadingSmoother(SMOOTHING)

    fun startListening(callback: (Float) -> Unit) {
        onHeadingChanged = callback

        magnetometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun onGravityChanged(x: Float, y: Float, z: Float) {
        gravity[0] = x
        gravity[1] = y
        gravity[2] = z
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_MAGNETIC_FIELD) {
            return
        }

        System.arraycopy(event.values, 0, geomagnetic, 0, 3)

        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientation)

            onHeadingChanged?.invoke(smoother.add(orientation[0]))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
