package info.alkor.bleinquirer.bluetooth

import android.bluetooth.BluetoothDevice
import info.alkor.bleinquirer.utils.Timeout

class BtLeDevicesScanner(private val scanner: BtLeScanner) {
    private val tag = "DeviceScanner"

    fun stopScanning(error: String? = null) = scanner.stopScanning(error)

    fun scanForDevices(timeout: Timeout, onDeviceFound: (BluetoothDevice) -> Unit): String? =
        scanner.scan(timeout) { onDeviceFound(it.device) }
}
