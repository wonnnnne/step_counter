package com.example.hellowork
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var btnStart : Button
    lateinit var stepStatus : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("yyyyMMddhhmmss")
        val time: String = format.format(date)
        val folderPath = File(getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs")
        if (!folderPath.exists()) {
            folderPath.mkdirs()
        }

        stepStatus = findViewById(R.id.step_status)
        checkBtn()
        btnStart = findViewById(R.id.btn_start)
        btnStart.setOnClickListener {
            try {
                if (HelloWork.prefs.getBoolean("step_counter", false)) {
                    Toast.makeText(this, "이미 걸음수를 측정중입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 신체활동 권한
                    if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestPermissions(
                                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                                1
                            )
                        } else {
                            StepService.startService(this@MainActivity, "걸음수 : 0")
                            Toast.makeText(this, "걸음수 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
                            checkBtn()
                        }
                    } else {
                        StepService.startService(this@MainActivity, "걸음수 : 0")
                        Toast.makeText(this, "걸음수 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
                        checkBtn()
                    }
                }
            } catch (e: Exception) {
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }

        val btnStop = findViewById<Button>(R.id.btn_stop)
        btnStop.setOnClickListener {
            try{
                if (!HelloWork.prefs.getBoolean("step_counter", false)) {
                    Toast.makeText(this,"걸음수 측정을 하고 있지않습니다.",Toast.LENGTH_SHORT).show()
                } else {
                    StepService.stopService(this@MainActivity)
                    Toast.makeText(this,"걸음수 측정을 중단합니다.",Toast.LENGTH_SHORT).show()
                    checkBtn()
                }
            } catch (e : Exception) {
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }
//        switch.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                try {
//                    // 신체활동 권한
//                    if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                            requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 1)
//                        } else {
//                            StepService.startService(this@MainActivity, "걸음수 : 0")
//                            Toast.makeText(this,"걸음수 측정을 시작합니다.",Toast.LENGTH_SHORT).show()
//                            checkBtn()
//                        }
//                    }
//
////                    // 배터리 최적화 제외
////                    val powerManager = getSystemService(POWER_SERVICE) as PowerManager
////                    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
////                        val intent = Intent()
////                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
////                        intent.data = Uri.parse("package:" + this@MainActivity.packageName)
////                        startActivity(intent)
////                    }
//
////                    StepService.startService(this@MainActivity, "걸음수 : 0")
////                    Toast.makeText(this,"걸음수 측정을 시작합니다.",Toast.LENGTH_SHORT).show()
//                } catch (e : Exception) {
//                    val logFile = File(folderPath, "log_${time}.txt")
//                    logFile.writeText(e.toString())
//                }
//            } else {
//                try{
//                    StepService.stopService(this@MainActivity)
//                    Toast.makeText(this,"걸음수 측정을 중단합니다.",Toast.LENGTH_SHORT).show()
//                    checkBtn()
//                } catch (e : Exception) {
//                    val logFile = File(folderPath, "log_${time}.txt")
//                    logFile.writeText(e.toString())
//                }
//            }
//        }
//        val btnStart = findViewById<Button>(R.id.btn_start)
//        btnStart.setOnClickListener {
//            // 신체활동 권한
//            if (ContextCompat.checkSelfPermission(this,
//                    android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 1)
//                }
//            }
//
//            // 배터리 최적화 제외
//            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
//            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
//                val intent = Intent()
//                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
//                intent.data = Uri.parse("package:" + this.packageName)
//                startActivity(intent)
//            }
//
//            StepService.startService(this, "걸음수 : 0")
//        }
//
//        val btnStop = findViewById<Button>(R.id.btn_stop)
//        btnStop.setOnClickListener {
//            StepService.stopService(this)
//        }
    }

    fun checkBtn() {
        if ( HelloWork.prefs.getBoolean("step_counter", false)) {
            stepStatus.text = "ON"
        } else {
            stepStatus.text = "OFF"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StepService.startService(this@MainActivity, "걸음수 : 0")
                    Toast.makeText(this,"걸음수 측정을 시작합니다.",Toast.LENGTH_SHORT).show()
                    checkBtn()
                } else {
                    Toast.makeText(this,"권한 허용이 필요합니다.",Toast.LENGTH_SHORT).show()
                    checkBtn()
                }
            }
        }
    }
}