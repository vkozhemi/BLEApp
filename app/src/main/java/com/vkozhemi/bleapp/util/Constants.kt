package com.vkozhemi.bleapp.util

import android.Manifest

const val REQUEST_ALL_PERMISSION = 1
val PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.BLUETOOTH_ADMIN,
)

const val GATT_SERVICE = "4576d562-7e68-11ec-90d6-0242ac120003"
