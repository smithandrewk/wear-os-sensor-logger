package com.delta.logger

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class AccelerometerListener(f: FileOutputStream,mainViewModel: MainViewModel) : SensorEventListener {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val events = Channel<SensorEvent>(Channel.UNLIMITED).also {
        scope.launch {
            for (msg in it) {
                f.write("${msg.timestamp},${msg.values[0]},${msg.values[1]},${msg.values[2]}\n".toByteArray())
                mainViewModel.addSample()
            }
        }
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            offer(event)
        }
    }
    private fun offer(event: SensorEvent) = runBlocking { events.send(event) }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
