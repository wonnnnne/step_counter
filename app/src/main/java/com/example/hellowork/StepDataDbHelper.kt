package com.example.hellowork

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class StepDataDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "step_data.db"
        val logger = LogHelper.getLogger(this::class.simpleName)
    }

    override fun onCreate(db: SQLiteDatabase) {
        logger.debug("db")
        db.execSQL(StepData.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //db.execSQL(StepData.SQL_DELETE_ENTRIES)
        logger.debug("db")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        logger.debug("db")
        onUpgrade(db, oldVersion, newVersion)
    }
}