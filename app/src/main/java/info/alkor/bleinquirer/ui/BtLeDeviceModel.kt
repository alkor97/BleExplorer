package info.alkor.bleinquirer.ui

data class BtLeDeviceModel(
    val address: String,
    val name: String?,
    val batteryLevel: Int?,
    val error: String?
)
