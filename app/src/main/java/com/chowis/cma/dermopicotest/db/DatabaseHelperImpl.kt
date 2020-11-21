package com.chowis.cma.dermopicotest.db

import com.chowis.cma.dermopicotest.model.Calibrate

class DatabaseHelperImpl(private val appDatabase: AppDatabase) : DatabaseHelper {

    override suspend fun getCalibrations(): List<Calibrate> = appDatabase.calibrationDao().getAll()

    override suspend fun insertCalibration(cal: Calibrate) = appDatabase.calibrationDao().insertCalibration(cal)

}