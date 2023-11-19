package com.vkozhemi.bleapp.util

import android.widget.Toast
import com.vkozhemi.bleapp.BleApplication

class Utils {
    companion object {

        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

        const val ACTION_START_FOREGROUND = "com.example.bluetooth.le.ACTION_START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.example.bluetooth.le.ACTION_STOP_FOREGROUND"
        const val ACTION_DISCONNECT_DEVICE = "com.example.bluetooth.le.ACTION_DISCONNECT_DEVICE"
        const val ACTION_STATUS_MSG = "com.example.bluetooth.le.ACTION_STATUS_MSG"
        const val ACTION_MSG_DATA = "com.example.bluetooth.le.ACTION_DATA_MSG"

        fun showNotification(msg: String) {
            Toast.makeText(BleApplication.applicationContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}