package com.vkozhemi.bleapp.model

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.vkozhemi.bleapp.BleApplication
import com.vkozhemi.bleapp.services.BleGattService
import com.vkozhemi.bleapp.util.Event
import com.vkozhemi.bleapp.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class Repository {
    var isConnected = MutableLiveData<Event<Boolean>>()
    var isStatusChange: Boolean = false
    var status: String = ""
    var deviceToConnect: BluetoothDevice? = null
    val fetchStatusText = flow {
        while (true) {
            if (isStatusChange) {
                emit(status)
                isStatusChange = false
            }
        }
    }.flowOn(Dispatchers.IO)

    private var gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "action ${intent.action}")
            when (intent.action) {
                Utils.ACTION_GATT_CONNECTED -> {
                    isConnected.postValue(Event(true))
                    intent.getStringExtra(Utils.ACTION_MSG_DATA)?.let {
                        status = it
                        isStatusChange = true
                    }
                }

                Utils.ACTION_GATT_DISCONNECTED -> {
                    stopForegroundService()
                    isConnected.postValue(Event(false))
                    intent.getStringExtra(Utils.ACTION_MSG_DATA)?.let {
                        status = it
                        isStatusChange = true
                    }
                }
            }

        }
    }

    fun registerGattReceiver() {
        BleApplication.applicationContext().registerReceiver(
            gattUpdateReceiver,
            makeGattUpdateIntentFilter()
        )
    }

    fun unregisterReceiver() {
        BleApplication.applicationContext().unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Utils.ACTION_GATT_CONNECTED)
        intentFilter.addAction(Utils.ACTION_GATT_DISCONNECTED)
        return intentFilter
    }

    fun connectDevice(device: BluetoothDevice?) {
        deviceToConnect = device
        startForegroundService()
    }

    private fun startForegroundService() {
        Intent(
            BleApplication.applicationContext(),
            BleGattService::class.java
        ).also { intent ->
            intent.action = Utils.ACTION_START_FOREGROUND
            BleApplication.applicationContext().startForegroundService(intent)
        }
    }

    fun stopForegroundService() {
        Intent(
            BleApplication.applicationContext(),
            BleGattService::class.java
        ).also { intent ->
            intent.action = Utils.ACTION_STOP_FOREGROUND
            BleApplication.applicationContext().startForegroundService(intent)
        }
    }

    fun disconnectGattServer() {
        Intent(
            BleApplication.applicationContext(),
            BleGattService::class.java
        ).also { intent ->
            intent.action = Utils.ACTION_DISCONNECT_DEVICE
            BleApplication.applicationContext().startForegroundService(intent)
        }
        deviceToConnect = null
    }

    companion object {
        private const val TAG: String = "Repository"
    }
}