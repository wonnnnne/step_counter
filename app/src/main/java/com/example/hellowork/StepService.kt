package com.example.hellowork

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log


object StepServiceAction {
    private const val prefix = "com.example.hellowork.action"
    const val MAIN = prefix + "main"
    const val START_FOREGROUND = prefix + "startforeground"
    const val STOP_FOREGROUND = prefix + "stopforeground"
}

class StepService : Service() {
    companion object {
        private const val TAG = "[STEP_SERVICE]"
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID = "primary_notification_channel"
        val logger = LogHelper.getLogger(this::class.simpleName)

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
    private var mStepManager: StepManager = StepManager.getInstance()
    private val format = SimpleDateFormat("yyyyMMddhhmmss")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("onStartCommand called!")
        try {
            createNotificationChannel()

            logger.debug("noti setting")
            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.action = Intent.ACTION_MAIN
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

//                    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
//                    } else {
//                        PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//                    }
            val pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            logger.debug("get mSensorManager")
            mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            logger.debug("get mStepManager")
            mStepManager = StepManager.getInstance()
            logger.debug("mStepManager : ${mStepManager}")

            logger.debug("mStepManager initialize")
            val currentStep = mStepManager.initialize(this, mSensorManager)
            if (currentStep == -1.0f) {
                Toast.makeText(this, "[센서없음]", Toast.LENGTH_SHORT).show()
            } else {
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo)
                    .setColor(Color.parseColor("#009AE0"))
                    .setContentTitle("${currentStep.toInt()} 걸음")
                    .setContentText("1만 걸음까지 힘내봐요")
                    .setContentIntent(pendingIntent)
                    .build()
                startForeground(NOTIFICATION_ID, notification)
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
        stopForeground(true)
        mStepManager.stop()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun createNotificationChannel() {
        logger.debug("createNotificationChannel called!")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW)
                val manager = getSystemService(NotificationManager::class.java)
                manager!!.createNotificationChannel(serviceChannel)
            }
        }catch (e:Exception) {
            val date = Date(System.currentTimeMillis())
            val time: String = format.format(date)
            val folderPath = getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }
}