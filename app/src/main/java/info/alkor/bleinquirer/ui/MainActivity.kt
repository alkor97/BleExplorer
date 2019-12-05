package info.alkor.bleinquirer.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.alkor.bleinquirer.BtLeApplication
import info.alkor.bleinquirer.R
import info.alkor.bleinquirer.bluetooth.BluetoothEnabler
import info.alkor.bleinquirer.utils.PermissionRequester
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val appContext: BtLeApplication by lazy {
        applicationContext as BtLeApplication
    }

    private val permissionsRequester: PermissionRequester by lazy {
        PermissionRequester(this)
    }

    private val bluetoothEnabler: BluetoothEnabler by lazy {
        BluetoothEnabler(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ensureDependenciesAvailability()

        val rv = rv_device_list as RecyclerView
        rv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        val adapter = BtLeDevicesAdapter(this)
        rv.adapter = adapter

        val observer =
            DevicesObserver(adapter)
        observer.onChanged(appContext.getDevicesModel().value ?: listOf())
        appContext.getDevicesModel().observe(this, observer)

        val deviceScanButton = device_search_button
        deviceScanButton.show()

        val deviceScanStopButton = device_stop_search_button
        deviceScanStopButton.hide()

        appContext.isScanningInProgress().observe(this, Observer<Boolean> {
            if (it) {
                deviceScanButton.hide()
                deviceScanStopButton.show()
            } else {
                deviceScanButton.show()
                deviceScanStopButton.hide()
            }
        })
    }

    private class DevicesObserver(val adapter: BtLeDevicesAdapter) :
        Observer<List<BtLeDeviceModel>> {
        override fun onChanged(newItems: List<BtLeDeviceModel>) {
            val oldItems = adapter.getItems()
            val result = DiffUtil.calculateDiff(
                DevicesDiffCallback(
                    oldItems,
                    newItems
                )
            )
            adapter.replaceItems(newItems)
            result.dispatchUpdatesTo(adapter)
        }
    }

    private class DevicesDiffCallback(
        val oldList: List<BtLeDeviceModel>,
        val newList: List<BtLeDeviceModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areContentsTheSame(oldItemPosition, newItemPosition)

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    private fun ensureDependenciesAvailability() {
        GlobalScope.launch {
            try {
                if (permissionsRequester.requestPermissions()) {
                    if (!bluetoothEnabler.ensureBluetoothIsEnabled()) {
                        Log.e("permissions", "Bluetooth not enabled!")
                    }
                } else {
                    Log.e("permissions", "requested permissions not granted!")
                }
            } catch (e: Throwable) {
                Log.e("permissions", "requesting permissions failed", e)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsRequester.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        bluetoothEnabler.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun searchButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        appContext.scanForDevices()
    }

    fun stopButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        appContext.stopScanning()
    }
}
