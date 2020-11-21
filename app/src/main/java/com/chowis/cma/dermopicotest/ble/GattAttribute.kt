package com.chowis.cma.dermopicotest.ble

import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

enum class GattAttribute constructor(val uuidString: String) {
    CCC_DESCRIPTOR("00002902-0000-1000-8000-00805f9b34fb"),
    KEY_SERVICE("0000ffe1-0000-1000-8000-00805f9b34fb"),
    KEY_CHARACTERISTIC("0000ffe9-0000-1000-8000-00805f9b34fb"),
    BATTERY_SERVICE("0000180F-0000-1000-8000-00805f9b34fb"),
    BATTERY_CHARACTERISTIC("00002a19-0000-1000-8000-00805f9b34fb"),
    MOISTURE_SERVICE("0000ffe2-0000-1000-8000-00805f9b34fb"),
    MOISTURE_CHARACTERISTIC("0000ffea-0000-1000-8000-00805f9b34fb");

    fun getUUID(): UUID = UUID.fromString(uuidString)

    companion object {
        @JvmStatic
        fun getUuidName(characteristic: BluetoothGattCharacteristic): String {
            val uuidString = characteristic.uuid.toString()
            val gattAttribute = values().find {
                it.uuidString == uuidString
            }
            if (gattAttribute != null)
                return gattAttribute.name
            return uuidString
        }
    }
}

enum class BleMode constructor(val byte: Int) {
    //POWER_OFF(0x12),
    //LED_MODE(0x18);
    VSL(0x21),
    UV(0x22),
    MOISTURE(0x41),
    MODE_OFF(0x80);
}