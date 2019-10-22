package com.example.bleinquirer

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class Action {
    CONNECT, DISCOVER, READ
}

enum class Outcome {
    SUCCESS, FAILURE, PENDING
}

typealias Reporter = (Action, Outcome, String?) -> Unit

class BtBatteryLevelReader(private val context: Context, private val reporter: Reporter) {
    private val tag = "BatteryLevelReader"
    private val BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private fun tag(device: BluetoothDevice) = "%s-%s".format(tag, device.description)
    private fun tag(gatt: BluetoothGatt) = tag(gatt.device)

    private fun report(action: Action, result: Outcome, reason: String? = null) {
        reporter(action, result, reason)
    }

    fun readBatteryLevel(device: BluetoothDevice, timeout: Long, unit: TimeUnit = TimeUnit.SECONDS): Pair<Int?, String> {
        var batteryLevel: Int? = null
        val lock = ReentrantLock()
        val condition = lock.newCondition()
        var error = "timeout"

        val callback = object : BluetoothGattCallback() {
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d(tag, "services discovered with status %s".format(BleGatt.statusToString(status)))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    report(Action.DISCOVER, Outcome.SUCCESS)
                    val service = gatt.getService(GattServices.BATTERY_SERVICE)
                    if (service == null) {
                        error = "Battery service not available"
                        report(Action.READ, Outcome.FAILURE, "battery service not available")
                        Log.e(tag(gatt), "battery service not available")
                        gatt.disconnect()
                        return
                    }

                    val characteristic = service.getCharacteristic(BATTERY_LEVEL)
                    if (characteristic == null) {
                        Log.e(tag(gatt), "battery level characteristic not available")
                        error = "Battery characteristic not available"
                        report(Action.READ, Outcome.FAILURE, "battery level not available")
                        gatt.disconnect()
                        return
                    }

                    if (gatt.readCharacteristic(characteristic)) {
                        report(Action.READ, Outcome.PENDING)
                        Log.d(tag(gatt), "battery level characteristic requested")
                    } else {
                        error = "Battery characteristic reading failed"
                        Log.e(tag(gatt), "reading battery level characteristic failed")
                        report(Action.READ, Outcome.FAILURE, "battery level reading failed")
                        gatt.disconnect()
                    }
                } else {
                    report(Action.DISCOVER, Outcome.FAILURE)
                }
            }
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(tag(gatt), "connection state changed to %s, status %s".format(
                    BleGatt.connectionStateToString(newState),
                    BleGatt.statusToString(status)))

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    report(Action.CONNECT, Outcome.SUCCESS)
                    if (gatt.discoverServices()) {
                        Log.d(tag(gatt), "service discovery started")
                        report(Action.DISCOVER, Outcome.PENDING)
                    } else {
                        error = "Service discovery failed"
                        report(Action.DISCOVER, Outcome.FAILURE, BleGatt.statusToString(status))
                        Log.d(tag(gatt), "service discovery has not been started, disconnecting")
                        gatt.disconnect()
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    gatt.close()
                    lock.withLock {
                        condition.signal()
                    }
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                Log.d(tag(gatt), "characteristic read with status %s".format(BleGatt.statusToString(status)))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic.uuid == BATTERY_LEVEL) {
                        batteryLevel = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                        error = "OK"
                        report(Action.READ, Outcome.SUCCESS)
                        Log.d(tag(gatt), "battery level is %d%%".format(batteryLevel))
                    }
                } else {
                    report(Action.READ, Outcome.FAILURE, BleGatt.statusToString(status))
                }

                // all information is already fetched, disconnect
                gatt.disconnect()
            }
        }

        Log.d(tag(device), "connecting to GATT")
        val gatt = device.connectGatt(context, true, callback, BluetoothDevice.TRANSPORT_LE)
        Log.d(tag(gatt), "GATT connection started with timeout of %d%s".format(timeout, unit.humanReadable))
        report(Action.CONNECT, Outcome.PENDING)

        try {
            lock.withLock {
                condition.await(timeout, unit)
            }
        } finally {
            gatt.close()
            Log.d(tag(gatt), "GATT connection completed")
        }

        return Pair(batteryLevel, error)
    }
}