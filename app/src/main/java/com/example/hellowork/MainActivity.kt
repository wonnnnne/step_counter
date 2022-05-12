package com.example.hellowork
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.apache.log4j.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var btnStart : Button
    lateinit var stepStatus : TextView
    private lateinit var logger : Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("yyyyMMddhhmmss")
        val time: String = format.format(date)
        val folderPath = File(getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs")
        HelloWork.prefs.setString("file_path", folderPath.toString())
        logger = LogHelper.getLogger(this::class.simpleName)
//        if (!folderPath.exists()) {
//            folderPath.mkdirs()
//            HelloWork.prefs.setString("file_path", folderPath.toString())
//        }

        stepStatus = findViewById(R.id.step_status)
        checkBtn()
        btnStart = findViewById(R.id.btn_start)
        btnStart.setOnClickListener {
            logger.debug("btnStart clicked!")
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
                            logger.debug("service start!")
                            checkBtn()
                        }
                    } else {
                        StepService.startService(this@MainActivity, "걸음수 : 0")
                        Toast.makeText(this, "걸음수 측정을 시작합니다.", Toast.LENGTH_SHORT).show()
                        logger.debug("service start!")
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
                    if (isServiceRunning(StepService::class.java)) {
                        logger.debug("service stopped!")
                        val intent = Intent(this@MainActivity, StepService::class.java)
                        stopService(intent)

                    }
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

    @Suppress("DEPRECATION")
    fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StepService.startService(this@MainActivity, "걸음수 : 0")
                    Toast.makeText(this,"걸음수 측정을 시작합니다.",Toast.LENGTH_SHORT).show()
                    logger.debug("service start!")
                    checkBtn()
                } else {
                    Toast.makeText(this,"권한 허용이 필요합니다.",Toast.LENGTH_SHORT).show()
                    checkBtn()
                }
            }
        }
    }
}