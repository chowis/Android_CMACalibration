package com.chowis.cma.dermopicotest.util

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.SizeSelector
import com.otaliastudios.cameraview.size.SizeSelectors
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object CommonUtil {
    fun getAppVersion(context: Context): String {
        val manager = context.packageManager
        val info = manager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)

        return info.versionName
    }

    fun getCurrentDate(): String {
        var dateText : String  = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            dateText =  current.format(formatter)
        } else {
            var date = Date()
            val formatter = SimpleDateFormat("MMM dd yyyy HH:mma")
            dateText = formatter.format(date)
        }
        return dateText
    }
    fun getCurrenTime(): String {
        var dateText : String  = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            dateText =  current.format(formatter)
        } else {
            var date = Date()
            val formatter = SimpleDateFormat("HH:mma")
            dateText = formatter.format(date)
        }
        return dateText
    }
}