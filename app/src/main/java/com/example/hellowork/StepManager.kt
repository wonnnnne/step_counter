package com.example.hellowork

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class StepManager private constructor(): SensorEventListener {

    companion object {
        @Volatile private var instance: StepManager? = null

        fun getInstance(): StepManager {
            return instance ?: synchronized(this) {
                instance ?: StepManager().also { instance = it }
            }
        }
    }

    private var mStepCount = 0.0f
    private var mBeforeStepCount = 0.0f
    private lateinit var mBeforeDate: LocalDate
    private lateinit var mSensorManager: SensorManager
    private var mStepCountSensor: Sensor?= null
    private var mFirstLaunching = HelloWork.prefs.getBoolean("first_launching", true)
    private var mContext: Context? = null
    private val mReceiver = DateChangedReceiver()
    private val format = SimpleDateFormat("yyyyMMddhhmmss")
    private val logger = LogHelper.getLogger(this::class.simpleName)


    @SuppressLint("Recycle")
    fun initialize(context: Context, sensorManager: SensorManager) : Float {
        try {
            logger.debug("initialize called!")
            mSensorManager = sensorManager
            mContext = context
            mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            Log.d("first_launching : " , mFirstLaunching.toString())
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_DATE_CHANGED)
            mContext!!.registerReceiver(mReceiver, filter)

            if (mStepCountSensor == null) {
                logger.debug("mStepCountSensor is null")
                return -1.0f
            } else {
                logger.debug("Register Listener")
                mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL)

//            startDayCheckThread()

                logger.debug("db helper")
                val dbHelper = StepDataDbHelper(mContext!!)
                logger.debug("db readable database")
                val db = dbHelper.readableDatabase
                logger.debug("db cursor")
                val cursor = db.rawQuery(StepData.SQL_SELECT_COUNT_ENTRIES, null)
                if (mFirstLaunching) {
                    logger.debug("db first launching")
                    mStepCount = 0.0f
                } else {
                    while (cursor.moveToNext()) {
                        logger.debug("db cursor get float")
                        mStepCount = cursor.getFloat(1)
                    }
                }
                return mStepCount
            }

        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = mContext!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
            return -1.0f
        }
    }

    fun stop() {
        try {
            logger.debug("mSensorManager unregister listener")
            mSensorManager.unregisterListener(this)
            logger.debug("mReceiver unregister listener")
            mContext!!.unregisterReceiver(mReceiver)
        }catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = mContext!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        logger.debug("onSensorChanged called!")
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            try {
                if (mBeforeStepCount < 1.0f) {
                    logger.debug("firstLaunching")
                    mBeforeStepCount = event.values[0]
                    mFirstLaunching = false
                    mBeforeDate = LocalDate.now()
                    HelloWork.prefs.setBoolean("first_launching", false)
                }
//                Log.d("[now]: ", LocalDate.now().toString())
//                if (mBeforeDate != LocalDate.now()) {
//                    mStepCount = 0.0f
//                    mBeforeDate = LocalDate.now()
//                }

                logger.debug("mStepCount:${mStepCount} | mBeforeStepCount:${mBeforeStepCount}")
                Log.d("[event.values[0]]: ", event.values[0].toString())
                Log.d("[mBeforeStepCount]: " , mBeforeStepCount.toString())
                mStepCount += (event.values[0] - mBeforeStepCount)
                mBeforeStepCount = event.values[0]
                logger.debug("mStepCount:${mStepCount} | mBeforeStepCount:${mBeforeStepCount}")
                Log.d("[STEP_COUNT]", "$mStepCount, $event")

                sendNotification()

                // write database
                logger.debug("db get db helper")
                val dbHelper = StepDataDbHelper(mContext!!)
                logger.debug("db write")
                val db = dbHelper.writableDatabase
                logger.debug("db sql")
                val sql = "INSERT INTO step_count (${StepData.Step.COLUMN_TIMESTAMP}, , ${StepData.Step.COLUMN_STEP}) " +
                        "VALUES ((datetime('now', 'localtime')), ${event.accuracy}, ${mStepCount})"
                logger.debug("db execSQL")
                db.execSQL(sql)
            } catch (e : Exception) {
                val date = Date(System.currentTimeMillis())
                val time: String = format.format(date)
                val folderPath = mContext!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }
    }

    private fun sendNotification() {

        try {
            // notification
            logger.debug("noti setting")
            val notificationIntent = Intent(mContext!!, MainActivity::class.java)
            notificationIntent.action = Intent.ACTION_MAIN
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


//            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                PendingIntent.getActivity(mContext!!,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
//            } else {
//                PendingIntent.getActivity(mContext!!,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//            }

            val pendingIntent = PendingIntent.getActivity(mContext!!,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            logger.debug("noti notify")
            val notification = NotificationCompat.Builder(mContext!!, StepService.CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_noti_logo)
                .setColor(Color.parseColor("#009AE0"))
//                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentTitle("${mStepCount.toInt()} 걸음")
                .setContentText("1만 걸음까지 힘내봐요")
                .setContentIntent(pendingIntent)
                .build()

            with(NotificationManagerCompat.from(mContext!!)) { notify(StepService.NOTIFICATION_ID, notification) }
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = mContext!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }

    fun resetSteps() {
        try {
            logger.debug("resetStepcs called")
            Log.d("[now]: ", LocalDate.now().toString() )
            mStepCount = 0.0f
            mBeforeDate = LocalDate.now()
            logger.debug("mStepCount:${mStepCount} | mBeforeDate:${mBeforeDate}")
            sendNotification()
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = mContext!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }

    }

//    @DelicateCoroutinesApi
//    private fun startDayCheckThread() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mBeforeDate = LocalDate.now()
//            GlobalScope.launch {
//                while (true) {
//                    val now = LocalDate.now()
//                    if (mBeforeDate != now) {
//                        mStepCount = 0.0f
//                        sendNotification()
//                    }
//                    mBeforeDate = now
//                    delay(1000)
//                }
//            }
//        }
//    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}