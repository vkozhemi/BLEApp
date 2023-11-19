package com.vkozhemi.bleapp.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vkozhemi.bleapp.R
import com.vkozhemi.bleapp.databinding.ActivitySearchBinding
import com.vkozhemi.bleapp.util.PERMISSIONS
import com.vkozhemi.bleapp.util.REQUEST_ALL_PERMISSION
import com.vkozhemi.bleapp.util.Utils
import com.vkozhemi.bleapp.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private var adapter: SearchDeviceAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.showNotification("Device does not support Bluetooth Low Energy")
            return
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search)
        binding.viewModel = viewModel

        binding.listRv.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.listRv.layoutManager = layoutManager

        if (!hasPermissions(this, PERMISSIONS)) {
            requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
        }

        adapter = SearchDeviceAdapter()
        binding.listRv.adapter = adapter
        adapter?.setItemClickListener(object : SearchDeviceAdapter.ItemClickListener {
            override fun onClick(view: View, device: BluetoothDevice?) {
                device?.let { connectToDevice(device) }
            }
        })

        viewModel.requestEnableBluetooth.observe(this) {
            it.getContentIfNotHandled()?.let {
                requestEnableBluetooth()
            }
        }
        viewModel.status.observe(this) {
            binding.statusTv.text = it
        }
        viewModel.listDevices.observe(this) {
            it.getContentIfNotHandled()?.let { scanResults ->
                adapter?.setItem(scanResults)
            }
        }
        viewModel.isConnected.observe(this) {
            it.getContentIfNotHandled()?.let { connect ->
                viewModel.isConnect.set(connect)
            }
        }

        viewModel.registerBroadCastReceiver()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice:: $device")

        val intent = Intent(this, DetailActivity::class.java)
        startActivity(intent)
        viewModel.connectDevice(device)
    }

    private fun requestEnableBluetooth() {
        val bleEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                Log.d(TAG, "requestEnableBluetooth:: $intent")
            }
        }.launch(bleEnableIntent)
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Permissions granted")
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Permissions must be granted")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterReceiver()
    }

    companion object {
        private const val TAG: String = "SearchActivity"
    }
}