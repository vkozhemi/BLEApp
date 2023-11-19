package com.vkozhemi.bleapp.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vkozhemi.bleapp.BleApplication
import com.vkozhemi.bleapp.model.Repository
import com.vkozhemi.bleapp.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.schedule

@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    val status: LiveData<String> =
        repository.fetchStatusText.asLiveData(viewModelScope.coroutineContext)

    val isConnected: LiveData<Event<Boolean>>
        get() = repository.isConnected

    private val bleManager: BluetoothManager =
        BleApplication.applicationContext()
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bleAdapter: BluetoothAdapter?
        get() = bleManager.adapter

    private val requestEnableBLE = MutableLiveData<Event<Boolean>>()
    val requestEnableBluetooth: LiveData<Event<Boolean>>
        get() = requestEnableBLE

    private val listUpdate = MutableLiveData<Event<ArrayList<BluetoothDevice>?>>()
    val listDevices: LiveData<Event<ArrayList<BluetoothDevice>?>>
        get() = listUpdate

    var isScanning = ObservableBoolean(false)
    var isConnect = ObservableBoolean(false)

    var scanResults: ArrayList<BluetoothDevice>? = ArrayList()

    fun onClickScan() {
        startScan()
    }

    private fun startScan() {
        if (bleAdapter == null || !bleAdapter?.isEnabled!!) {
            requestEnableBLE.postValue(Event(true))
            repository.status = "Scan failed, ble is not enabled"
            repository.isStatusChange = true
            return
        }

        val filters: MutableList<ScanFilter> = ArrayList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bleAdapter?.bluetoothLeScanner?.startScan(filters, settings, leScanCallback)

        repository.status = "Scanning...."
        repository.isStatusChange = true
        isScanning.set(true)

        Timer("Scanning", false).schedule(AWAIT) { stopScan() }
    }

    private fun stopScan() {
        bleAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        isScanning.set(false)
        repository.status = "Scan finished. Select device to connect by tapping on it"
        repository.isStatusChange = true
        scanResults = ArrayList()
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.i(TAG, "Device name: " + result.device.name)
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with errorCode $errorCode")
            repository.status = "scan failed with errorCode $errorCode"
            repository.isStatusChange = true
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            for (dev in scanResults!!) {
                if (dev.address == deviceAddress) return
            }
            scanResults?.add(result.device)
            repository.status = "add scanned device: $deviceAddress"
            repository.isStatusChange = true
            listUpdate.postValue(Event(scanResults))
        }
    }

    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        repository.connectDevice(bluetoothDevice)
    }

    fun registerBroadCastReceiver() {
        repository.registerGattReceiver()
    }

    fun unregisterReceiver() {
        repository.unregisterReceiver()
    }

    companion object {
        private const val TAG: String = "SearchViewModel"
        private const val AWAIT: Long = 5000
    }
}