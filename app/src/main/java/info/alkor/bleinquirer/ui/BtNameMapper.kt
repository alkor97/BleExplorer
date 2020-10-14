package info.alkor.bleinquirer.ui

import android.bluetooth.BluetoothDevice
import java.util.*

class BtNameMapper {

    private val properties: Properties by lazy { loadNameMapping() }

    fun getName(device: BluetoothDevice): String =
        properties.getProperty(keyOf(device), device.name)

    private fun keyOf(device: BluetoothDevice) = "mac_" + device.address.replace(":", "")

    private fun loadNameMapping() =
        javaClass.classLoader!!.getResourceAsStream("private/name-mapping.properties")
            .use {
                Properties().apply { load(it) }
            }
}