package info.alkor.bleinquirer.persistence

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*

class NameMapper(context: Context) {

    private val dao: NameMappingDao by lazy { AppDatabase.getInstance(context).nameMappingDao() }

    fun getName(device: BluetoothDevice): String =
        dao.getNameOf(upperCaseAddress(device)) ?: device.name

    fun setName(address: String, name: String) =
        if (!name.isBlank())
            dao.storeNameMapping(NameMapping(address, name))
        else
            dao.deleteNameMapping(address)

    private fun upperCaseAddress(device: BluetoothDevice) = device.address.toUpperCase(Locale.US)
}