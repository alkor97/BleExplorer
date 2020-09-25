package info.alkor.bleinquirer.bluetooth.specific

import java.nio.ByteBuffer
import java.nio.ByteOrder

class XiaomiMijiaHtSensor(bytes: ByteArray) {
    private enum class PayloadType {
        UNKNOWN,
        TEMPERATURE_AND_HUMIDITY,
        TEMPERATURE,
        HUMIDITY,
        BATTERY
    }

    val header = bytes.sliceArray(0..4)
    val address = bytes.sliceArray(5..10).reversedArray().toHexString(":")
    private val payloadType = when (getType(bytes)) {
        0x0d10 -> PayloadType.TEMPERATURE_AND_HUMIDITY
        0x0410 -> PayloadType.TEMPERATURE
        0x0610 -> PayloadType.HUMIDITY
        0x0a10 -> PayloadType.BATTERY
        else -> PayloadType.UNKNOWN
    }
    val temperature = when (payloadType) {
        PayloadType.TEMPERATURE_AND_HUMIDITY -> parseValue(bytes.sliceArray(14..15))
        PayloadType.TEMPERATURE -> parseValue(bytes.sliceArray(14..15))
        else -> null
    }
    val humidity = when (payloadType) {
        PayloadType.TEMPERATURE_AND_HUMIDITY -> parseValue(bytes.sliceArray(16..17))
        PayloadType.HUMIDITY -> parseValue(bytes.sliceArray(14..15))
        else -> null
    }
    val battery = when (payloadType) {
        PayloadType.BATTERY -> bytes.get(14).toInt()
        else -> null
    }

    private fun parseValue(bytes: ByteArray): Double? {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.getShort(0) / 10.0
    }

    private fun getType(bytes: ByteArray) =
        ByteBuffer.wrap(bytes.sliceArray(11..12)).getShort(0).toInt()

    override fun toString() = when (payloadType) {
        PayloadType.TEMPERATURE_AND_HUMIDITY -> "temperature is $temperature°C, humidity is $humidity%"
        PayloadType.TEMPERATURE -> "temperature is $temperature°C"
        PayloadType.HUMIDITY -> "humidity is $humidity%"
        PayloadType.BATTERY -> "battery is $battery%"
        else -> "unknown"
    }
}

fun ByteArray.toHexString(separator: String = " ") =
    map { "%02x".format(it) }.joinToString(separator)
