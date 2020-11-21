package com.chowis.cma.dermopicotest.db

import com.chowis.cma.dermopicotest.model.Calibrate

interface DatabaseHelper {
    suspend fun getCalibrations(): List<Calibrate>

    suspend fun insertCalibration(cal : Calibrate)
}