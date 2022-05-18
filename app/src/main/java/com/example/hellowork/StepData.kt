package com.example.hellowork

import android.provider.BaseColumns

object StepData {
    object Step : BaseColumns {
        const val TABLE_NAME = "step_count"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_STEP = "step"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE IF NOT EXISTS ${StepData.Step.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${StepData.Step.COLUMN_TIMESTAMP} TEXT," +
                "${StepData.Step.COLUMN_STEP} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${StepData.Step.TABLE_NAME}"

    const val SQL_SELECT_COUNT_ENTRIES = "SELECT MAX(${StepData.Step.COLUMN_TIMESTAMP}), ${StepData.Step.COLUMN_STEP} from ${StepData.Step.TABLE_NAME} \n" +
            "WHERE (strftime('%s', ${StepData.Step.COLUMN_TIMESTAMP})  - strftime('%s', date('now', 'localtime'))) >= 0\n" +
            "AND strftime('%s', ${StepData.Step.COLUMN_TIMESTAMP})  - (strftime('%s', date('now', 'localtime') ) + 86400) < 0\n"
}

