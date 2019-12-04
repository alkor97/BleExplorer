package com.example.bleinquirer

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BtLeApplication : Application() {

    private val btAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanningInProgress = LiveObject(AtomicBoolean(false)) { it.get() }
    fun isScanningInProgress(): LiveData<Boolean> = scanningInProgress.live()

    private val devicesModel =
        LiveObject<List<BtLeDeviceModel>, MutableList<BtLeDeviceModel>>(mutableListOf())

    fun getDevicesModel() = devicesModel.live()
    private val devices = mutableListOf<BluetoothDevice>()

    private fun setScanningInProgress() = scanningInProgress.update { it.compareAndSet(false, true) }
    private fun resetScanningInProgress() = scanningInProgress.update {
        it.set(false)
        true
    }

    private var scanner: BtLeDevicesScanner? = null

    fun scanForDevices(): Boolean {
        val scanTimeout = DEFAULT_SCAN_TIMEOUT
        //val readTimeout = DEFAULT_BATTERY_READ_TIMEOUT

        if (setScanningInProgress()) {
            devices.clear()
            devicesModel.update {
                it.clear()
                true
            }

            GlobalScope.launch {
                try {
                    scanForDevices(scanTimeout)
                    showToast("Scanning completed with %d devices".format(devices.size))

                    /*devices.forEachIndexed { index, device ->
                        val (batteryLevel, error) = readBatteryLevel(device, readTimeout) {
                            val msg = it
                            devicesModel.update { devicesModel ->
                                devicesModel[index] = BtLeDeviceModel(
                                    device.address,
                                    device.name,
                                    devicesModel[index].batteryLevel,
                                    (devicesModel[index].error ?: "") + "\n$msg"
                                )
                                true
                            }
                        }
                        if (batteryLevel != null) {
                            showToast(
                                "Battery level of %s is %d%%".format(
                                    device.description,
                                    batteryLevel
                                )
                            )
                        } else {
                            showToast(
                                "Battery level of %s is unknown due to %s".format(
                                    device.description,
                                    error
                                )
                            )
                        }

                        devicesModel.update {
                            it[index] = BtLeDeviceModel(
                                device.address,
                                device.name,
                                batteryLevel,
                                it[index].error
                            )
                            true
                        }
                    }*/

                    resetScanningInProgress()
                } catch (e: Throwable) {
                    Log.e("BtCoroutine", "exception while processing", e)
                }
            }
            return true
        }
        return false
    }

    private fun scanForDevices(timeout: Timeout) {
        if (scanner == null) {
            try {
                scanner = BtLeDevicesScanner(btAdapter!!)
                scanner?.let {scanner ->
                    scanner.scanForDevices(timeout) {device ->
                        if (device.name != null) {
                            addDevice(device)
                        }
                    }?.apply {
                        showToast("Scanning failed due to %s".format(this))
                    }
                }
            } finally {
                scanner = null
            }
        }
    }

    fun stopScanning() {
        scanner?.stopScanning("stopped by user")
    }

    private fun addDevice(device: BluetoothDevice) {
        devices.add(device)
        devicesModel.update {
            it.add(
                BtLeDeviceModel(
                    device.address,
                    device.name,
                    null,
                    null
                )
            )
            true
        }
    }

    private fun showToast(text: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun readBatteryLevel(
        device: BluetoothDevice,
        timeout: Timeout,
        reporter: (String) -> Unit
    ) = BluetoothReader(applicationContext, device).readBatteryLevel(timeout, reporter)

    companion object {
        val DEFAULT_SCAN_TIMEOUT = Timeout(30, TimeUnit.SECONDS)
        val DEFAULT_BATTERY_READ_TIMEOUT = Timeout(30, TimeUnit.SECONDS)
    }
}
