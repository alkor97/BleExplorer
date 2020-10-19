package info.alkor.bleinquirer.ui

import java.util.*

data class BtLeDeviceModel(
    val address: String,
    val name: String?,
    val battery: Int?,
    val error: String?,

    val temperature: Double?,
    val humidity: Double?,
    val luminance: Int?,
    val moisture: Int?,
    val fertility: Int?,

    val lastUpdate: Date,

    val useCustomName: Boolean = false
)
