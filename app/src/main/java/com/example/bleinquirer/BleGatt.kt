package com.example.bleinquirer

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

object BleGatt {
    fun statusToString(value: Int) = "%s (%d)".format(when (value) {
        BluetoothGatt.GATT_SUCCESS -> "success"
        BluetoothGatt.GATT_CONNECTION_CONGESTED -> "connection congested"
        BluetoothGatt.GATT_FAILURE -> "failure"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "insufficient authentication"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "insufficient encryption"
        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid attribute length"
        BluetoothGatt.GATT_INVALID_OFFSET -> "invalid offset"
        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "read not permitted"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "request not supported"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "write not permitted"
        else -> "unknown"
    }, value)

    fun connectionStateToString(value: Int) = "%s (%d)".format(when (value) {
        BluetoothProfile.STATE_CONNECTED -> "connected"
        BluetoothProfile.STATE_CONNECTING -> "connecting"
        BluetoothProfile.STATE_DISCONNECTED -> "disconnected"
        BluetoothProfile.STATE_DISCONNECTING -> "disconnecting"
        else -> "unknown"
    }, value)
}