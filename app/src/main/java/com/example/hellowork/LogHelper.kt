package com.example.hellowork

import android.os.Environment
import org.apache.log4j.DailyRollingFileAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import java.io.File
import java.io.IOException

object LogHelper {
    init {
        configuration()
    }

    fun configuration(){
        val patternLayout = createPatternLayout()
        val rollingAppender = createDailyRollingLogFileAppender(patternLayout)
        setAppenderWithRootLogger(rollingAppender)
    }

    private fun createPatternLayout(): PatternLayout {
        val patternLayout = PatternLayout()
        val conversionPattern = "[%d] %c %M - [%p] %m%n"
        patternLayout.conversionPattern = conversionPattern

        return patternLayout
    }

    private fun createDailyRollingLogFileAppender(patternLayout: PatternLayout): DailyRollingFileAppender {
        val rollingAppender = DailyRollingFileAppender()
        val path = makeDirectory()
        val fileName = "$path/LogFile.log"
        rollingAppender.file = fileName
        rollingAppender.datePattern = "'.'yyyy-MM-dd"
        rollingAppender.layout = patternLayout
        rollingAppender.activateOptions()

        return rollingAppender
    }

    private fun makeDirectory(): String {
        val path = HelloWork.prefs.getString("file_path","")
        val logDir = File(path)
        if (!logDir.exists()) {
            try {
                logDir.mkdir()
            } catch (e: IOException) {
                e.printStackTrace()
                return HelloWork.prefs.getString("file_path","")
            }
        }
        return path
    }

    private fun setAppenderWithRootLogger(rollingAppender: DailyRollingFileAppender) {
        val rootLogger = Logger.getRootLogger()
        rootLogger.level = Level.DEBUG
        rootLogger.addAppender(rollingAppender)
    }

    fun getLogger(name: String?): Logger {
        return Logger.getLogger(name)
    }
}