package info.alkor.bleinquirer.ui

import java.util.*

data class BtLeDeviceModel(
    val address: String,
    val name: String? = null,
    val battery: Int? = null,
    val error: String? = null,

    val temperature: Double? = null,
    val humidity: Double? = null,
    val luminance: Int? = null,
    val moisture: Int? = null,
    val fertility: Int? = null,

    val lastUpdate: Date = Date(),

    val useCustomName: Boolean = false
)
