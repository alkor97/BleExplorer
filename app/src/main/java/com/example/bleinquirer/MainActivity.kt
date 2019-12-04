package com.example.bleinquirer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_COARSE_LOCATION_PERMISSION = 2
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val appContext: BtLeApplication by lazy {
        applicationContext as BtLeApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setTitle("This app needs location update")
                    setMessage("Please grant location update so this app can detect beacons.")
                    setPositiveButton(android.R.string.ok, null)
                    setOnDismissListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            REQUEST_COARSE_LOCATION_PERMISSION
                        )
                    }
                }.show()
            } else {
                ensureBluetoothIsEnabled()
            }
        } else {
            ensureBluetoothIsEnabled()
        }

        val rv = rv_device_list as RecyclerView
        rv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        val adapter = BtLeDevicesAdapter(this)
        rv.adapter = adapter

        val observer = DevicesObserver(adapter)
        observer.onChanged(appContext.getDevicesModel().value ?: listOf())
        appContext.getDevicesModel().observe(this, observer)

        val deviceScanButton = device_search_button
        appContext.isScanningInProgress().observe(this, Observer<Boolean> {
            if (it) {
                deviceScanButton.hide()
            } else {
                deviceScanButton.show()
            }
        })
    }

    private class DevicesObserver(val adapter: BtLeDevicesAdapter) : Observer<List<BtLeDeviceModel>> {
        override fun onChanged(newItems: List<BtLeDeviceModel>) {
            val oldItems = adapter.getItems()
            val result = DiffUtil.calculateDiff(DevicesDiffCallback(oldItems, newItems))
            adapter.replaceItems(newItems)
            result.dispatchUpdatesTo(adapter)
        }
    }

    private class DevicesDiffCallback(val oldList: List<BtLeDeviceModel>, val newList: List<BtLeDeviceModel>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = areContentsTheSame(oldItemPosition, newItemPosition)
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_COARSE_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ensureBluetoothIsEnabled()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setTitle("Functionality limited")
                    setMessage("Since location update has not been granted, this app will not be able to discover beacons when in the background.")
                    setPositiveButton(android.R.string.ok, null)
                    setOnDismissListener { }
                }.show()
            }
        }
    }

    private fun ensureBluetoothIsEnabled() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter?.apply {
                if (!isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    Log.d("ble", "Bluetooth enabling requested")
                }
            }
        } else {
            Log.e("ble", "Bluetooth not available")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            Log.i("ble", "user rejected request of enabling Bluetooth")
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun searchButtonClicked(view: View) {
        appContext.scanForDevices()
    }
}
