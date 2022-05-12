package com.example.hellowork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            val logger = LogHelper.getLogger(this::class.simpleName)
            logger.debug("RebootReceiver called!")
            Log.d("[Reboot]", "${intent?.action}")
            StepService.startService(context!!, "걸음수 : 0")
        } catch (e : Exception) {
            val date = Date(System.currentTimeMillis())
            val format = SimpleDateFormat("yyyyMMddhhmmss")
            val time: String = format.format(date)
            val folderPath = context!!.getExternalFilesDir(null)!!.absolutePath.toString() + "/" + "logs"
            val logFile = File(folderPath, "log_${time}.txt")
            logFile.writeText(e.toString())
        }
    }
}