package com.example.hellowork
import android.Manifest
import android.Manifest.permission_group.ACTIVITY_RECOGNITION
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.apache.log4j.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    lateinit var btnStart : Button
    lateinit var btnGet : Button
    lateinit var stepStatus : TextView
    private lateinit var logger : Logger
    private var denyCount : Int = 0

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
                if (isServiceRunning(StepService::class.java)) {
                    Toast.makeText(this, "이미 걸음수를 측정중입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 신체활동 권한
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestPermissions(
                                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
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
                if (isServiceRunning(StepService::class.java)) {
                    logger.debug("service stopped!")
                    val intent = Intent(this@MainActivity, StepService::class.java)
                    stopService(intent)
                    Toast.makeText(this,"걸음수 측정을 중단합니다.",Toast.LENGTH_SHORT).show()
                    checkBtn()
                } else {
                    Toast.makeText(this,"걸음수 측정을 하고 있지않습니다.",Toast.LENGTH_SHORT).show()
                }
            } catch (e : Exception) {
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }

        btnGet = findViewById(R.id.btn_get)
        btnGet.setOnClickListener{
            getDailySteps()
        }
    }

    fun getDailySteps() {
//        var query : String ="select * from " + MY_TABLE + " where " + DATE_COL + " BETWEEN " + minDate + " 00:00:00 AND " + maxDate + " 23:59:59";
//
//       query =  "SELECT MAX(${StepData.Step.COLUMN_TIMESTAMP}), ${StepData.Step.COLUMN_STEP} from ${StepData.Step.TABLE_NAME} \n" +
//                "WHERE (strftime('%s', ${StepData.Step.COLUMN_TIMESTAMP})  - strftime('%s', date('now', 'localtime'))) >= 0\n" +
//                "AND strftime('%s', ${StepData.Step.COLUMN_TIMESTAMP})  - (strftime('%s', date('now', 'localtime') ) + 86400) < 0\n"

        val dbHelper: StepDataDbHelper = StepDataDbHelper(this)
        val db: SQLiteDatabase = dbHelper.writableDatabase
        val dbReadable = dbHelper.readableDatabase
        val startTime ="2022-05-17 18:05:22"
        val endTime = "2022-05-17 22:10:49"
        var cursor : Cursor
//        val query = "SELECT * FROM ${StepData.Step.TABLE_NAME} \n" +
//                "WHERE ${StepData.Step.COLUMN_TIMESTAMP}\n" +
//                "BETWEEN '2022-05-17 00:00:00' AND '2022-05-17 23:59:59'"

        val query = "SELECT * FROM ${StepData.Step.TABLE_NAME} \n" +
                "WHERE ${StepData.Step.COLUMN_TIMESTAMP} >= '2022-05-17 19:00:00'"
        cursor = dbReadable.rawQuery(query, null)
        cursor.moveToFirst()
        val get = cursor.getColumnIndex("step")
        Log.d("steps  : ", cursor.getFloat(get).toString())
        var arrayList = ArrayList<String>()
        var mFirstDate = ""
        mFirstDate = cursor.getString(1)
        mFirstDate = mFirstDate.substring(0, 10)
        arrayList.add(mFirstDate)
        Log.d("mFirstDate  : ", mFirstDate)
        var mNextDate = ""
        while (cursor.moveToNext()) {
            mNextDate = cursor.getString(1)
            mNextDate = mNextDate.substring(0, 10)
            Log.d("mNextDate  : ", mNextDate)
            if (mNextDate != mFirstDate) {
                arrayList.add(mNextDate)
                mFirstDate = mNextDate
            }
        }
        cursor.close()
        Log.d("arrayList  : ", arrayList.toString())
//        while (cursor.){
//            Log.d("data : " ,cursor.getFloat(3).toString())
//        }

        var cursor2 : Cursor
        for (i in 0 until arrayList.size) {
            val startDate = arrayList[i] +" 00:00:00"
            val endDate = arrayList[i] +" 23:59:59"
            val query = "SELECT MAX(${StepData.Step.COLUMN_STEP}) FROM ${StepData.Step.TABLE_NAME} \n" +
                    "WHERE ${StepData.Step.COLUMN_TIMESTAMP}\n" +
                    "BETWEEN '$startDate' AND '$endDate'"
            cursor2 = dbReadable.rawQuery(query, null)
            cursor2.moveToFirst()
//            val get = cursor.getColumnIndex("step")
//            Log.d("get  : ", get.toString())
            Log.d("steps  : ", cursor2.getFloat(0).toString())
        }
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
            .any { it.service.className == service.name }
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
                    denyCount++
                    if (denyCount>1) {
                        Toast.makeText(this,"설정에서 권한을 허용해주세요",Toast.LENGTH_SHORT).show()
                    }else {
                        Toast.makeText(this,"권한 허용이 필요합니다.",Toast.LENGTH_SHORT).show()
                        checkBtn()
                    }
                }
            }
        }
    }
}