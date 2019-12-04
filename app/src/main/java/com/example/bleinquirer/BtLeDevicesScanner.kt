package com.example.bleinquirer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BtLeDevicesScanner(private val adapter: BluetoothAdapter) {
    private val tag = "DeviceScanner"

    private fun scanErrorCode(value: Int): String = "%s (%d)".format(when (value) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "scan already started"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "application registration failed"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "feature unsupported"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "internal error"
        else -> "unknown"
    }, value)

    fun scanForDevices(timeout: Timeout, onDeviceFound: (BluetoothDevice) -> Unit): String? {
        Log.d(tag, "starting device scan")
        var error: String? = null

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val uniqueAddresses = hashSetOf<String>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.apply {
                    handleScanResult(result)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "scan failed with %s".format(scanErrorCode(errorCode)))
                error = scanErrorCode(errorCode)
                lock.withLock {
                    condition.signal()
                }
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.apply {
                    results.forEach { handleScanResult(it) }
                }
            }
            private fun handleScanResult(result: ScanResult) {
                if (!uniqueAddresses.contains(result.device.address)) {
                    uniqueAddresses.add(result.device.address)
                    Log.d(tag, "%s device found".format(result.device.description))
                    onDeviceFound(result.device)
                }
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            //.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            //.setReportDelay(0L)
            .build()

        try {
            adapter.bluetoothLeScanner.startScan(null, scanSettings, callback)
            Log.d(tag, "device scan started with %s timeout".format(timeout.humanReadable))

            lock.withLock {
                condition.await(timeout.value, timeout.unit)
            }
        } finally {
            adapter.bluetoothLeScanner.stopScan(callback)
            Log.d(tag, "device scan completed")
        }

        return error
    }

    @WorkerThread
    suspend fun scanForDevices(timeout: Long, unit: TimeUnit = TimeUnit.SECONDS): Collection<BluetoothDevice> {
        val devices = mutableMapOf<String,BluetoothDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.apply {
                    handleScanResult(result)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "scan failed with %s".format(scanErrorCode(errorCode)))
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.apply {
                    results.forEach { handleScanResult(it) }
                }
            }
            private fun handleScanResult(result: ScanResult) {
                if (!devices.containsKey(result.device.address)) {
                    Log.d(tag, "%s device found".format(result.device.description))
                    devices[result.device.address] = result.device
                }
            }
        }

        try {
            Log.d(tag, "starting device scan")
            val scanSettings = ScanSettings.Builder()
                //.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                //.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                //.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                //.setReportDelay(0L)
                .build()
            //val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(GattServices.BATTERY_SERVICE)).build())
            //adapter.bluetoothLeScanner.startScan(null, scanSettings, callback)
            adapter.bluetoothLeScanner.startScan(callback)
            Log.d(tag, "device scan started with %d%s timeout".format(timeout, unit.humanReadable))
            delay(unit.toMillis(timeout))
        } finally {
            adapter.bluetoothLeScanner.stopScan(callback)
            Log.d(tag, "device scan completed")
        }
        return devices.values
    }
}

val BluetoothDevice.description: String
    get() = if (name != null && !name.isBlank()) "%s (%s)".format(address, name) else address

val TimeUnit.humanReadable: String
    get() = when (this) {
        TimeUnit.NANOSECONDS -> "ns"
        TimeUnit.MICROSECONDS -> "us"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.SECONDS -> "s"
        TimeUnit.MINUTES -> "min"
        TimeUnit.HOURS -> "h"
        TimeUnit.DAYS -> "d"
    }