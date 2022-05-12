package com.example.hellowork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class DateChangedReceiver: BroadcastReceiver()  {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            Log.d("[DateChangedReceiver]", "onReceive")
            if(Intent.ACTION_DATE_CHANGED == intent!!.action) {
                val logger = LogHelper.getLogger(this::class.simpleName)
                logger.debug("DateChangedReceiver called!")
                val stepService = StepService()
                stepService.resetSteps(context!!)
//                val stepManager : StepManager = StepManager.getInstance()
//                logger.debug("StepManager : ${stepManager}")
//                stepManager.resetSteps()
            }
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