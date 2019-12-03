package com.example.bleinquirer

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.bluetooth.tools.Characteristic
import com.bluetooth.tools.Service
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NewBatteryLevelReader(private val context: Context) {

    private val tag = "BatteryLevelReader"
    private fun tag(device: BluetoothDevice) = "%s-%s".format(tag, device.description)
    private fun tag(gatt: BluetoothGatt) = tag(gatt.device)
    private fun tag() = gatt?.apply {tag(this)}.let {tag}

    private companion object {
        const val DISCONNECTED = 0
        const val CONNECTING = 1
        const val CONNECTED = 2
        const val DISCONNECTING = 3

        const val NOT_DISCOVERED = 0
        const val DISCOVERING = 1
        const val DISCOVERED = 2

        const val NOT_READ = 0
        const val READ = 1
        const val READING = 2
    }

    private val connectionState = AtomicInteger(DISCONNECTED)
    private val discoveryState = AtomicInteger(NOT_DISCOVERED)
    private val readingState = AtomicInteger(READ)

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var timedOut = false

    private fun awaitCompletion(timeout: Timeout): Boolean {
        timedOut = true
        timedOut = lock.withLock { condition.await(timeout.value, timeout.unit) }
        return timedOut
    }
    private fun signalCompletion() = lock.withLock { condition.signalAll() }

    var gatt: BluetoothGatt? = null
    var characteristic: BluetoothGattCharacteristic? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val stateDescription = BleGatt.connectionStateToString(newState)
            val statusDescription = BleGatt.statusToString(status)
            Log.d(tag(gatt), "connection state changed to $stateDescription, status $statusDescription")

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> handleConnected(gatt)
                BluetoothGatt.STATE_DISCONNECTED -> handleDisconnected(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val statusDescription = BleGatt.statusToString(status)
            Log.d(tag(gatt), "services discovered with status $statusDescription")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleServicesDiscovered(gatt)
            } else {
                handleServicesDiscoveryError(gatt, statusDescription)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val statusDescription = BleGatt.statusToString(status)
            Log.d(tag(gatt), "characteristic read with status $statusDescription")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleReadingSuccess(gatt, characteristic)
            } else {
                handleReadingError(gatt, characteristic, statusDescription)
            }
        }
    }

    private fun handleConnectionRequest(device: BluetoothDevice, timeout: Timeout): Boolean {
        if (connectionState.compareAndSet(DISCONNECTED, CONNECTING)) {
            device.connectGatt(context, true, callback, BluetoothDevice.TRANSPORT_LE)
            awaitCompletion(timeout)
        }
        return connectionState.get() == CONNECTED
    }

    private fun handleDisconnectionRequest(gatt: BluetoothGatt, timeout: Timeout): Boolean {
        if (connectionState.compareAndSet(CONNECTED, DISCONNECTING)) {
            gatt.disconnect()
            awaitCompletion(timeout)
        }
        return connectionState.get() == CONNECTED
    }

    private fun handleConnected(gatt: BluetoothGatt) {
        connectionState.set(CONNECTED)
        this.gatt = gatt
        signalCompletion()
    }

    private fun handleDisconnected(gatt: BluetoothGatt) {
        connectionState.set(DISCONNECTED)
        discoveryState.set(NOT_DISCOVERED)
        readingState.set(NOT_READ)

        gatt.close()
        this.gatt = null

        signalCompletion()
    }

    private fun handleDiscoveryRequest(timeout: Timeout): Boolean {
        gatt?.apply {
            if (connectionState.get() == CONNECTED && discoveryState.compareAndSet(NOT_DISCOVERED, DISCOVERING)) {
                if (discoverServices()) {
                    awaitCompletion(timeout)
                }
            }
        }
        return discoveryState.get() == DISCOVERED
    }

    private fun handleServicesDiscovered(gatt: BluetoothGatt) {
        this.gatt = gatt
        discoveryState.set(DISCOVERED)
        signalCompletion()
    }

    private fun handleServicesDiscoveryError(gatt: BluetoothGatt, message: String) {
        this.gatt = gatt
        discoveryState.set(NOT_DISCOVERED)
        signalCompletion()
        Log.d(tag(), message)
    }

    private fun handleReadRequest(characteristic: BluetoothGattCharacteristic, timeout: Timeout): Boolean {
        gatt?.apply {
            if (connectionState.get() == CONNECTED && discoveryState.get() == DISCOVERED && readingState.compareAndSet(READ, READING)) {
                if (readCharacteristic(characteristic)) {
                    awaitCompletion(timeout)
                }
            }
        }
        return readingState.get() == READ
    }

    private fun handleReadingSuccess(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        this.gatt = gatt
        this.characteristic = characteristic
        readingState.set(READ)
        signalCompletion()
    }

    private fun handleReadingError(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, message: String) {
        this.gatt = gatt
        this.characteristic = characteristic
        readingState.set(READ)
        signalCompletion()
        Log.d(tag(), message)
    }

    private fun getService(uuid: UUID): BluetoothGattService? = gatt?.let { getService(uuid) }

    private class StopWatch(timeout: Timeout) {
        private val startTime = now()
        private val timeout = timeout.to(TimeUnit.MILLISECONDS).value

        private fun now() = System.currentTimeMillis()

        val elapsed: Timeout
            get() = Timeout(now() - startTime, TimeUnit.MILLISECONDS)
        val remaining: Timeout
            get() = Timeout(elapsed.let { if (it.value < timeout) timeout - it.value else 0 }, TimeUnit.MILLISECONDS)
    }

    fun getBatteryLevel(device: BluetoothDevice, timeout: Timeout): Int? {
        val stopWatch = StopWatch(timeout)

        try {
            if (!handleConnectionRequest(device, stopWatch.remaining)) {
                Log.d(tag(device), "unable to connect")
                return null
            }

            if (!handleDiscoveryRequest(stopWatch.remaining)) {
                Log.d(tag(), "services not discovered")
                return null
            }

            val service = getService(Service.BATTERY_SERVICE.uuid)
            if (service == null) {
                Log.d(tag(), "no battery service")
                return null
            }

            val characteristic = service.getCharacteristic(Characteristic.BATTERY_LEVEL.uuid)
            if (characteristic == null) {
                Log.d(tag(), "no battery level characteristic")
                return null
            }

            if (!handleReadRequest(characteristic, stopWatch.remaining)) {
                Log.d(tag(), "error while reading battery level")
                return null
            }

            return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        } finally {
            gatt?.apply {
                handleDisconnectionRequest(this, timeout)
                close()
            }
            gatt = null
        }
    }
}