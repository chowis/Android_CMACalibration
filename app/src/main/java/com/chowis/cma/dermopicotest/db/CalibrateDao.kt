package com.chowis.cma.dermopicotest.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chowis.cma.dermopicotest.model.Calibrate

@Dao
interface CalibrateDao {
    @Query("SELECT * FROM Calibrate")
    suspend fun getAll(): List<Calibrate>

    @Insert
    suspend fun insertCalibration(cal: Calibrate)
}