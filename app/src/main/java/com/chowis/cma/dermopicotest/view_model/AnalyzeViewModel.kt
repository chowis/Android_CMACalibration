package com.chowis.cma.dermopicotest.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chowis.cma.dermopicotest.db.DatabaseHelper
import com.chowis.cma.dermopicotest.model.Calibrate
import kotlinx.coroutines.launch

class AnalyzeViewModel (private val dbHelper: DatabaseHelper) :
    ViewModel() {
    private val calibration = MutableLiveData<List<Calibrate>>()

     fun calibrateToDb() {
        viewModelScope.launch {
            try {
                val calibrations = dbHelper.getCalibrations()
                calibration.postValue(calibrations)
            } catch (e: Exception) {

            }
        }
    }
    fun insetCalibration(cal: Calibrate) = viewModelScope.launch {
        dbHelper.insertCalibration(cal)
    }
    fun getCalibration(): LiveData<List<Calibrate>> {
        return calibration
    }

}