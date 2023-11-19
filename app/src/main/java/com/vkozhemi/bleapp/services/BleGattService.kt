package com.vkozhemi.bleapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vkozhemi.bleapp.BleApplication
import com.vkozhemi.bleapp.R
import com.vkozhemi.bleapp.model.Repository
import com.vkozhemi.bleapp.ui.SearchActivity
import com.vkozhemi.bleapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BleGattService : Service() {
    @Inject
    lateinit var repository: Repository

    private val binder: IBinder = LocalBinder()

    private var bluetoothGatt: BluetoothGatt? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action Received = ${intent?.action}")

        when (intent?.action) {
            Utils.ACTION_START_FOREGROUND -> {
                startForegroundService()
            }

            Utils.ACTION_STOP_FOREGROUND -> {
                stopForegroundService()
            }

            Utils.ACTION_DISCONNECT_DEVICE -> {
                disconnectGattServer("Disconnected")
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        startForeground()
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        val service: BleGattService
            get() = this@BleGattService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun startForeground() {
        val channelId =
            createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notificationIntent = Intent(this, SearchActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Service is running in background")
            .setContentText("Tap to open")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        connectDevice(repository.deviceToConnect)
    }

    private fun createNotificationChannel(): String {
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH
        )
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind::")
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy::")
        super.onDestroy()
    }

    private fun broadcastUpdate(action: String, msg: String) {
        val intent = Intent(action)
        intent.putExtra(Utils.ACTION_MSG_DATA, msg)
        sendBroadcast(intent)
    }

    private val gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer("Bluetooth Gatt Failed")
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer("Disconnected")
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // update the connection status message
                broadcastUpdate(Utils.ACTION_GATT_CONNECTED, "Connected")
                Log.d(TAG, "Connected to the GATT server")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(Utils.ACTION_GATT_DISCONNECTED, "Disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer("Device service discovery failed, status: $status")
                return
            }
            bluetoothGatt = gatt
            Log.d(TAG, "Services discovery is successful")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            readCharacteristic(characteristic)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                disconnectGattServer("Characteristic write unsuccessful, status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully")
                readCharacteristic(characteristic)
            } else {
                Log.e(TAG, "Characteristic read failed, status: $status")
            }
        }

        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            val msg = characteristic.value
            Log.d(TAG, "Characteristic: $msg")
        }
    }

    private fun connectDevice(device: BluetoothDevice?) {
        broadcastUpdate(Utils.ACTION_STATUS_MSG, "Connecting to ${device?.address}")
        bluetoothGatt =
            device?.connectGatt(BleApplication.applicationContext(), false, gattClientCallback)
    }

    fun disconnectGattServer(msg: String) {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.disconnect()
            bluetoothGatt!!.close()
        }
        broadcastUpdate(Utils.ACTION_GATT_DISCONNECTED, msg)
    }

    companion object {
        private const val TAG: String = "BtGattService"
    }
}