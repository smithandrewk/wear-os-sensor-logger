/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.delta.sensor_logger

import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.delta.sensor_logger.presentation.theme.WearAppTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometerListener: AccelerometerListener
    private lateinit var mLinearAccelerationListener: LinearAccelerationListener
    private lateinit var mGyroscopeListener: GyroscopeListener
    private lateinit var externalFilesDir : File
    private lateinit var appStartTimeReadable: String
    private lateinit var appStartTimeNanos: String
    private lateinit var fAccel: FileOutputStream
    private lateinit var fLinear: FileOutputStream
    private lateinit var fGyro: FileOutputStream
    private lateinit var fLog: FileOutputStream
    private lateinit var mainViewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        appStartTimeNanos = "${SystemClock.elapsedRealtimeNanos()}"
        appStartTimeReadable = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.ENGLISH).format(Date())

        externalFilesDir = getExternalFilesDir(null)!!
        File(externalFilesDir, appStartTimeReadable).mkdir()
        fAccel = FileOutputStream(File(externalFilesDir,"$appStartTimeReadable/acceleration.csv"))
        fLinear = FileOutputStream(File(externalFilesDir,"$appStartTimeReadable/linear_acceleration.csv"))
        fGyro = FileOutputStream(File(externalFilesDir,"$appStartTimeReadable/gyroscope.csv"))
        fLog = FileOutputStream(File(externalFilesDir,"$appStartTimeReadable/log.csv"))

        try {
            val json = JSONObject()
                .put("SystemClock.elapsedRealtimeNanos", appStartTimeNanos)
                .put("App Start Time Readable", appStartTimeReadable)
            File(externalFilesDir, "$appStartTimeReadable/metadata.json").appendText(json.toString())
        } catch (e: Exception) { e.printStackTrace() }

        mainViewModel = MainViewModel()
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometerListener = AccelerometerListener(fAccel,mainViewModel)
        mLinearAccelerationListener = LinearAccelerationListener(fLinear,mainViewModel)
        mGyroscopeListener = GyroscopeListener(fGyro,mainViewModel)
        mSensorManager.registerListener(mAccelerometerListener,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(mGyroscopeListener,mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(mLinearAccelerationListener,mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),SensorManager.SENSOR_DELAY_NORMAL)

        lifecycleScope.launch {
            while(true) {
                val batteryStatus: Intent? = registerReceiver(null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val batteryPct: Float? = batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    level * 100 / scale.toFloat()
                }
                fLog.write("${SystemClock.elapsedRealtimeNanos()},battery,$batteryPct\n".toByteArray())
                delay(300000)
            }
        }

        setContent {
            WearApp(mainViewModel)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(mAccelerometerListener)
        this.fAccel.close()
        this.fGyro.close()
        this.fLinear.close()
    }
}

@Composable
fun WearApp(viewModel: MainViewModel) {
    val count = viewModel.sampleCount
    WearAppTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .selectableGroup(),
            verticalArrangement = Arrangement.Center
        ) {
            Body(count=count)
        }
    }
}


@Composable
fun Body(count: Int) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, count)
    )
}
class MainViewModel(): ViewModel() {
    var sampleCount by mutableStateOf(0)

    fun addSample() {
        sampleCount ++
    }
}
