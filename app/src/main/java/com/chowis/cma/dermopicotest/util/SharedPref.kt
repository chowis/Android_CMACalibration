package com.chowis.cma.dermopicotest.util

import com.chibatching.kotpref.KotprefModel

object SharedPref : KotprefModel() {

    var pairedDevice by stringPref("")
    var pairedDeviceName by stringPref("")
    var pairedDeviceSerial by stringPref("")
    var isFirstUse by booleanPref(true)
    var newDevice by booleanPref(true)

}