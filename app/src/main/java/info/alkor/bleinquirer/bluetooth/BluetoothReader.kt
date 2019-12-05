package info.alkor.bleinquirer.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.bluetooth.tools.Characteristic
import com.bluetooth.tools.Service
import info.alkor.bleinquirer.bluetooth.handler.Error
import info.alkor.bleinquirer.bluetooth.handler.NotConnected
import info.alkor.bleinquirer.bluetooth.handler.Success
import info.alkor.bleinquirer.bluetooth.handler.TemplateHandler
import info.alkor.bleinquirer.utils.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class BluetoothReader(private val context: Context, private val device: BluetoothDevice) {

    private val tag = "BatteryLevelReader"
    private fun tag(device: BluetoothDevice) = "%s-%s".format(tag, device.description)
    private fun tag(gatt: BluetoothGatt) = tag(gatt.device)

    private val handler = TemplateHandler()
    private val requester = handler.requester
    private val responder = handler.responder

    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(
                tag(gatt), "connection state changed to %s, status %s".format(
                    BleGatt.connectionStateToString(newState),
                    BleGatt.statusToString(status)
                )
            )

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                responder.handleConnected()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                responder.handleDisconnected(
                    if (status == BluetoothGatt.GATT_SUCCESS) Success else Error(
                        BleGatt.statusToString(status)
                    )
                )
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(tag, "services discovered with status %s".format(BleGatt.statusToString(status)))
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@BluetoothReader.gatt = gatt
                responder.handleServicesDiscovered()
            } else {
                responder.handleServicesDiscoveryError(
                    Error(
                        BleGatt.statusToString(status)
                    )
                )
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(
                tag(gatt),
                "characteristic read with status %s".format(BleGatt.statusToString(status))
            )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@BluetoothReader.characteristic = characteristic
                responder.handleReadingSuccess()
            } else {
                responder.handleReadingError(
                    Error(
                        BleGatt.statusToString(status)
                    )
                )
            }
        }
    }

    private fun connect(timeout: Timeout) = requester.handleConnectionRequest(timeout) {
        gatt = device.connectGatt(context, true, callback, BluetoothDevice.TRANSPORT_LE)
        return@handleConnectionRequest if (gatt != null) Success else Error(
            "Connection failed"
        )
    }

    private fun disconnect(timeout: Timeout) = requester.handleDisconnectionRequest(timeout) {
        gatt?.let {
            it.disconnect()
            Success
        } ?: return@handleDisconnectionRequest Success
    }

    private fun discover(timeout: Timeout) = requester.handleDiscoveryRequest(timeout) {
        gatt?.let {
            return@handleDiscoveryRequest if (it.discoverServices()) Success else Error(
                "Discovery failed"
            )
        } ?: return@handleDiscoveryRequest NotConnected
    }

    private fun read(serviceId: UUID, characteristicId: UUID, timeout: Timeout) =
        requester.handleReadRequest(timeout) {
            gatt?.let {
                val service = it.getService(serviceId)
                if (service == null) {
                    val name = Service.getFullName(serviceId)
                    return@handleReadRequest Error(
                        "Service $name not available"
                    )
                }

                val characteristic = service.getCharacteristic(characteristicId)
                if (characteristic == null) {
                    val name = Characteristic.getFullName(characteristicId)
                    return@handleReadRequest Error(
                        "Characteristic $name not available"
                    )
                }

                if (!it.readCharacteristic(characteristic)) {
                    val name = Characteristic.getFullName(characteristicId)
                    return@handleReadRequest Error(
                        "Cannot read $name characteristic"
                    )
                }

                return@handleReadRequest Success
            } ?: return@handleReadRequest NotConnected
        }

    private class StopWatch(timeout: Timeout) {
        private val startTime = now()
        private val timeout = timeout.to(TimeUnit.MILLISECONDS).value

        private fun now() = System.currentTimeMillis()

        val elapsed: Timeout
            get() = Timeout(
                now() - startTime,
                TimeUnit.MILLISECONDS
            )
        val remaining: Timeout
            get() = Timeout(
                elapsed.let { if (it.value < timeout) timeout - it.value else 0 },
                TimeUnit.MILLISECONDS
            )
    }

    fun readBatteryLevel(timeout: Timeout, report: (String) -> Unit): Pair<Int?, String> {
        val stopWatch =
            StopWatch(timeout)

        Log.d(tag(device), "trying to connect")
        var result = connect(stopWatch.remaining)
        report("connect $result")
        if (result !is Success) {
            Log.d(tag(device), "connecting failed: $result")
            return Pair(null, result.toString())
        }

        try {
            Log.d(tag(device), "trying to discover services")
            result = discover(stopWatch.remaining)
            report("discover $result")
            if (result !is Success) {
                Log.d(tag(device), "discovering failed: $result")
                return Pair(null, result.toString())
            }

            result = read(
                Service.BATTERY_SERVICE.uuid,
                Characteristic.BATTERY_LEVEL.uuid,
                stopWatch.remaining
            )
            Log.d(tag(device), "trying to read")
            report("read $result")
            if (result !is Success) {
                Log.d(tag(device), "reading failed: $result")
                return Pair(null, result.toString())
            }

            Log.d(tag(device), "reading succeeded: $result")
            return Pair(
                characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0),
                result.toString()
            )
        } finally {
            Log.d(tag(device), "trying to disconnect")
            result = disconnect(timeout)
            report("disconnect $result")
            if (result !is Success) {
                Log.d(tag(device), "disconnecting failed: $result")
                gatt?.close()
                return Pair(null, result.toString())
            }
        }
    }
}