package com.chowis.cma.dermopicotest.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chowis.cma.dermopicotest.model.Calibrate

@Database(entities = [Calibrate::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrateDao
}