package com.chowis.cma.dermopicotest.util

import com.chowis.cma.dermopicotest.model.Calibrate

interface ClickListener {
    fun onClickListener( isChecked: Boolean,history : Calibrate)
}