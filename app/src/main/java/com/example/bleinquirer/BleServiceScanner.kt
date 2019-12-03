package com.example.bleinquirer

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.bluetooth.tools.Service
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

class BleServiceScanner(private val context: Context) {

    private val tag = "ServiceScanner"

    private fun logTag(gatt: BluetoothGatt) = "%s-%s".format(tag, gatt.device.description)

    @WorkerThread
    suspend fun scanServices(device: BluetoothDevice, timeout: Long, unit: TimeUnit = TimeUnit.SECONDS): List<UUID> {
        val services = mutableListOf<UUID>()
        val callback = object : BluetoothGattCallback() {
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.d(tag, "services discovered with status %s".format(BleGatt.statusToString(status)))
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    Log.d(logTag(gatt), "%d services discovered".format(gatt.services.size))
                    for (service in gatt.services) {
                        Log.d(logTag(gatt), " - " + Service.getFullName(service.uuid))
                        services.add(service.uuid)
                    }
                }
            }
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(logTag(gatt), "connection state changed to %s, status %s".format(
                    BleGatt.connectionStateToString(newState),
                    BleGatt.statusToString(status)))
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    if (gatt.discoverServices()) {
                        Log.d(logTag(gatt), "service discovery started")
                    } else {
                        Log.d(logTag(gatt), "service discovery has not been started")
                        gatt.close()
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }
        }

        val gatt = device.connectGatt(context, true, callback)
        Log.d(logTag(gatt), "service discovery started with timeout of %d%s".format(timeout, unit.humanReadable))
        try {
            delay(unit.toMillis(timeout))
        } finally {
            gatt.close()
            Log.d(logTag(gatt), "service discovery completed")
        }

        return services
    }
}
