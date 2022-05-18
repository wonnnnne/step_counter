package com.example.hellowork

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*


object StepServiceAction {
    private const val prefix = "com.example.hellowork.action"
    const val MAIN = prefix + "main"
    const val START_FOREGROUND = prefix + "startforeground"
    const val STOP_FOREGROUND = prefix + "stopforeground"
}

class StepService : Service(), SensorEventListener {
    companion object {
        private const val TAG = "[STEP_SERVICE]"
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID = "primary_notification_channel"
        val logger = LogHelper.getLogger(this::class.simpleName)
        private var mStepCount = 0.0f
        private var mBeforeStepCount = 0.0f
        private lateinit var mBeforeDate: LocalDate
        private lateinit var mCurrentDate: LocalDate
        private var mStepCountSensor: Sensor?= null
        private var mFirstLaunching = HelloWork.prefs.getBoolean("first_launching", true)
        private val mReceiver = DateChangedReceiver()
        private lateinit var mBuilder: Notification
        private lateinit var pendingIntent: PendingIntent
        private lateinit var notificationIntent: Intent
        private lateinit var dbHelper: StepDataDbHelper
        lateinit var db: SQLiteDatabase
        private lateinit var cursor: Cursor
        private var sql : String = ""

        fun startService(context: Context, message: String) {
            try {
                logger.debug("startService called")
                HelloWork.prefs.setBoolean("step_counter", true)
                val startIntent = Intent(context, StepService::class.java)
                startIntent.putExtra("inputExtra", message)
                startIntent.action = StepServiceAction.START_FOREGROUND
                ContextCompat.startForegroundService(context, startIntent)
                Log.d("[startService]", "registered")
            }catch (e : Exception) {
                val date = Date(System.currentTimeMillis())
                val format = SimpleDateFormat("yyyyMMddhhmmss")
                val time: String = format.format(date)
                val folderPath = context.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }
    }

    private lateinit var mSensorManager: SensorManager
    private val format = SimpleDateFormat("yyyyMMddhhmmss")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("onStartCommand called!")
        try {
            createNotificationChannel()

            logger.debug("noti setting")
            notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.action = Intent.ACTION_MAIN
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            logger.debug("get mSensorManager")
            mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

            logger.debug("mStepManager initialize")
            val currentStep = initialize()
            if (currentStep == -1.0f) {
                Toast.makeText(this, "[센서없음]", Toast.LENGTH_SHORT).show()
            } else {
                mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon_noti_logo)
                    .setColor(Color.parseColor("#009AE0"))
                    .setContentTitle("${currentStep.toInt()} 걸음")
                    .setContentText("1만 걸음까지 힘내봐요")
                    .setContentIntent(pendingIntent)
                    .build()
                startForeground(NOTIFICATION_ID, mBuilder)
            }
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        logger.debug("onDestroy called!")
        HelloWork.prefs.setBoolean("step_counter", false)
        logger.debug("mSensorManager unregister listener")
        mSensorManager.unregisterListener(this)
        logger.debug("mReceiver unregister listener")
        this.unregisterReceiver(mReceiver)
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun createNotificationChannel() {
        logger.debug("createNotificationChannel called!")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(serviceChannel)
            }
        }catch (e:Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }

    private fun initialize() : Float {
        try {
            logger.debug("initialize called!")
            mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            Log.d("first_launching : " , mFirstLaunching.toString())
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_DATE_CHANGED)
            this.registerReceiver(mReceiver, filter)

            if (mStepCountSensor == null) {
                logger.debug("mStepCountSensor is null")
                return -1.0f
            } else {
                logger.debug("Register Listener")
                mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL)

                mCurrentDate = LocalDate.now()
                logger.debug("db helper")
                dbHelper = StepDataDbHelper(this)
                db = dbHelper.writableDatabase
                logger.debug("db readable database")
                val dbReadable = dbHelper.readableDatabase
                logger.debug("db cursor")
                cursor = dbReadable.rawQuery(StepData.SQL_SELECT_COUNT_ENTRIES, null)
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
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
            return -1.0f
        } finally {
            cursor.close()
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
                    sql = "INSERT INTO step_count (${StepData.Step.COLUMN_TIMESTAMP}, ${StepData.Step.COLUMN_STEP}) " + "VALUES (date('now'), ${mStepCount})\n"
                    db.execSQL(sql)
                    HelloWork.prefs.setBoolean("first_launching", false)
                }

                logger.debug("mStepCount:${mStepCount} | mBeforeStepCount:${mBeforeStepCount}")
                Log.d("[event.values[0]]: ", event.values[0].toString())
                Log.d("[mBeforeStepCount]: " , mBeforeStepCount.toString())
                mStepCount += (event.values[0] - mBeforeStepCount)
                mBeforeStepCount = event.values[0]
                logger.debug("mStepCount:${mStepCount} | mBeforeStepCount:${mBeforeStepCount}")
                Log.d("[STEP_COUNT]", "$mStepCount, $event")

                sendNotification()

                // write database
                logger.debug("db sql")
                sql = "UPDATE step_count SET ${StepData.Step.COLUMN_STEP}=${mStepCount} WHERE ${StepData.Step.COLUMN_TIMESTAMP} = date('now')"
                logger.debug("db execSQL")
                db.execSQL(sql)
            } catch (e : Exception) {
                val date = Date(System.currentTimeMillis())
                val time: String = format.format(date)
                val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
                val logFile = File(folderPath, "log_${time}.txt")
                logFile.writeText(e.toString())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun sendNotification() {
        try {
            logger.debug("noti notify")
            mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_noti_logo)
                .setColor(Color.parseColor("#009AE0"))
                .setContentTitle("${mStepCount.toInt()} 걸음")
                .setContentText("1만 걸음까지 힘내봐요")
                .setContentIntent(pendingIntent)
                .build()
            with(NotificationManagerCompat.from(this)) { notify(NOTIFICATION_ID, mBuilder) }
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }

    private fun resetNotification(context: Context) {
        try {
            logger.debug("noti notify")
            mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_noti_logo)
                .setColor(Color.parseColor("#009AE0"))
                .setContentTitle("${mStepCount.toInt()} 걸음")
                .setContentText("1만 걸음까지 힘내봐요")
                .setContentIntent(pendingIntent)
                .build()
            with(NotificationManagerCompat.from(context)) { notify(NOTIFICATION_ID, mBuilder) }
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val format = SimpleDateFormat("yyyyMMddhhmmss")
            val time: String = format.format(date)
            val folderPath = context.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }

    fun resetSteps(context: Context) {
        try {
            logger.debug("resetSteps called")
            Log.d("[now]: ", LocalDate.now().toString() )
            mStepCount = 0.0f
            mBeforeDate = LocalDate.now()
            logger.debug("mStepCount:${mStepCount} | mBeforeDate:${mBeforeDate}")
            val sql = "INSERT INTO step_count (${StepData.Step.COLUMN_TIMESTAMP}, ${StepData.Step.COLUMN_STEP}) " + "VALUES (date('now'), ${mStepCount})"
            db.execSQL(sql)
            resetNotification(context)
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }
}