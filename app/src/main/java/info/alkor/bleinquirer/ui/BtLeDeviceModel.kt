package info.alkor.bleinquirer.ui

import java.util.*

data class BtLeDeviceModel(
    val address: String,
    val name: String?,
    val batteryLevel: Int?,
    val error: String?,

    val temperature: Double?,
    val humidity: Double?,

    val lastUpdate: Date
)
