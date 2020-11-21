package com.chowis.cma.dermopicotest.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chowis.cma.dermopicotest.db.DatabaseHelper
import com.chowis.cma.dermopicotest.view_model.AnalyzeViewModel

class ViewModelFactory(private val dbHelper: DatabaseHelper) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyzeViewModel::class.java)) {
            return AnalyzeViewModel(dbHelper) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}
