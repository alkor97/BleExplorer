package info.alkor.bleinquirer.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import info.alkor.bleinquirer.utils.Timeout
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BtLeScanner(private val adapter: BluetoothAdapter) {
    private val tag = "BtLeScan"

    private fun scanErrorCode(value: Int): String = "%s (%d)".format(
        when (value) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "scanning already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "application registration failed"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "feature unsupported"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "internal error"
            else -> "unknown"
        }, value
    )

    private val lock = ReentrantLock()
    private val scanningCompleted = lock.newCondition()
    private var error: String? = null

    fun stopScanning(error: String? = null) {
        lock.withLock {
            this.error = error
            scanningCompleted.signal()
        }
    }

    private fun awaitScanningCompletion(timeout: Timeout) {
        lock.withLock {
            scanningCompleted.await(timeout.value, timeout.unit)
        }
    }

    private fun getError(): String? = lock.withLock { return this.error }

    fun scan(timeout: Timeout, handleResult: (ScanResult) -> Unit): String? {
        Log.d(tag, "starting the scanning")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.apply {
                    handleScanResult(result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "scan failed with %s".format(scanErrorCode(errorCode)))
                stopScanning(scanErrorCode(errorCode))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.apply {
                    results.forEach { handleScanResult(it) }
                }
            }

            private fun handleScanResult(result: ScanResult) {
                handleResult(result)
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
            Log.d(tag, "scanning started with %s timeout".format(timeout.humanReadable))

            awaitScanningCompletion(timeout)
        } finally {
            adapter.bluetoothLeScanner.stopScan(callback)
            val error = getError()
            Log.d(tag, "scanning completed with status %s".format(error ?: "success"))
            return error
        }
    }
}