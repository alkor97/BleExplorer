package info.alkor.bleinquirer.persistence

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*

class NameMapper(context: Context) {

    private val dao: NameMappingDao by lazy { AppDatabase.getInstance(context).nameMappingDao() }

    fun getName(device: BluetoothDevice): String = blockForResponse {
        dao.getNameOf(upperCaseAddress(device)) ?: device.name
    }

    fun setName(address: String, name: String) = blockForResponse {
        if (!name.isBlank())
            dao.storeNameMapping(NameMapping(address, name))
        else
            dao.deleteNameMapping(address)
    }

    private fun <T> blockForResponse(block: suspend CoroutineScope.() -> T) =
        runBlocking(Dispatchers.IO, block)

    private fun upperCaseAddress(device: BluetoothDevice) = device.address.toUpperCase(Locale.US)
}