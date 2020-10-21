package info.alkor.bleinquirer.models

import android.bluetooth.BluetoothDevice
import info.alkor.bleinquirer.bluetooth.specific.XiaomiSensor
import info.alkor.bleinquirer.ui.BtLeDeviceModel
import info.alkor.bleinquirer.ui.BtNameMapper
import info.alkor.bleinquirer.utils.LiveObject
import java.util.*

class DevicesModel(private val nameMapper: BtNameMapper = BtNameMapper()) {

    private val devices =
        LiveObject<List<BtLeDeviceModel>, MutableList<BtLeDeviceModel>>(mutableListOf())

    fun devices() = devices.live()

    fun addDevice(device: BluetoothDevice, sensor: XiaomiSensor?) {
        val name = nameMapper.getName(device)
        devices.update { list ->
            list.add(
                BtLeDeviceModel(
                    device.address,
                    name,
                    sensor?.battery,
                    null,
                    sensor?.temperature,
                    sensor?.humidity,
                    sensor?.luminance,
                    sensor?.moisture,
                    sensor?.fertility,
                    Date(),
                    useCustomName = name != device.name
                )
            )
        }
    }

    fun updateDevice(updated: BtLeDeviceModel) = doUpdate(updated.address) { updated }

    fun updateDevice(device: BluetoothDevice, sensor: XiaomiSensor?) =
        doUpdate(device.address) { original ->
            val name = nameMapper.getName(device)
            return@doUpdate BtLeDeviceModel(
                original.address,
                if (original.useCustomName) original.name else name,
                sensor?.battery ?: original.battery,
                null,
                sensor?.temperature ?: original.temperature,
                sensor?.humidity ?: original.humidity,
                sensor?.luminance ?: original.luminance,
                sensor?.moisture ?: original.moisture,
                sensor?.fertility ?: original.fertility,
                Date(),
                useCustomName = original.useCustomName || name != device.name
            )
        }

    private fun doUpdate(address: String, updater: (BtLeDeviceModel) -> BtLeDeviceModel) =
        devices.update { list ->
            for (i in 0 until list.size) {
                val original = list[i]
                if (original.address == address) {
                    val updated = updater(original)
                    if (updated != original) {
                        list[i] = updated
                        return@update true
                    }
                    return@update false
                }
            }
            false
        }
}