package info.alkor.bleinquirer.bluetooth.specific

import java.nio.ByteBuffer
import java.nio.ByteOrder

// https://github.com/AlCalzone/ioBroker.ble/blob/master/src/plugins/lib/xiaomi_protocol.ts
data class XiaomiSensor(
    val header: Header,
    val address: String? = null,
    val capabilities: Capabilities? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val battery: Int? = null,
    val luminance: Int? = null,
    val moisture: Int? = null,
    val fertility: Int? = null
) {
    override fun toString(): String {
        val sb = ArrayList<String>()
        if (temperature != null) sb.add("temperature is $temperature°C")
        if (humidity != null) sb.add("humidity is $humidity%")
        if (luminance != null) sb.add("luminance is $luminance lx")
        if (moisture != null) sb.add("moisture is $moisture%")
        if (fertility != null) sb.add("fertility is $fertility µS/cm")
        if (battery != null) sb.add("battery is $battery%")
        return sb.joinToString(", ")
    }

    data class Header(
        val version: Int,
        val flags: Flags
    )

    data class Flags(
        val isNewFactory: Boolean,
        val isConnected: Boolean,
        val isCentral: Boolean,
        val isEncrypted: Boolean,
        val hasMacAddress: Boolean,
        val hasCapabilities: Boolean,
        val hasEvent: Boolean,
        val hasCustomData: Boolean,
        val hasSubtitle: Boolean,
        val isBindingFrame: Boolean
    )

    data class Capabilities(
        val connectible: Boolean,
        val centralCapable: Boolean,
        val encryptable: Boolean,
        val io: Boolean
    )

    companion object {
        private const val NewFactory = 1 shl 0
        private const val Connected = 1 shl 1
        private const val Central = 1 shl 2
        private const val Encrypted = 1 shl 3
        private const val MacAddress = 1 shl 4
        private const val Capabilities = 1 shl 5
        private const val Event = 1 shl 6
        private const val CustomData = 1 shl 7
        private const val Subtitle = 1 shl 8
        private const val Binding = 1 shl 9

        private const val Connectible = 1 shl 0
        private const val CentralCapable = 1 shl 1
        private const val Encryptable = 1 shl 2
        private const val IO = (1 shl 3) or (1 shl 4)

        private const val Temperature = 0x1004
        private const val KettleStatusAndTemperature = 0x1005
        private const val Humidity = 0x1006
        private const val Illuminance = 0x1007
        private const val Moisture = 0x1008
        private const val Fertility = 0x1009
        private const val Battery = 0x100A
        private const val TemperatureAndHumidity = 0x100D

        private fun parseHeader(bytes: ByteArray): Header {
            val frameControl = readUnsignedShort(bytes, 0)
            val version = frameControl shr 12
            val flags = frameControl and 0xfff
            return Header(
                version = version,
                flags = Flags(
                    isNewFactory = (flags and NewFactory) != 0,
                    isConnected = (flags and Connected) != 0,
                    isCentral = (flags and Central) != 0,
                    isEncrypted = (flags and Encrypted) != 0,
                    hasMacAddress = (flags and MacAddress) != 0,
                    hasCapabilities = (flags and Capabilities) != 0,
                    hasEvent = (flags and Event) != 0,
                    hasCustomData = (flags and CustomData) != 0,
                    hasSubtitle = (flags and Subtitle) != 0,
                    isBindingFrame = (flags and Binding) != 0
                )
            )
        }

        private fun parseCapabilities(value: Int) = Capabilities(
            connectible = (value and Connectible) != 0,
            centralCapable = (value and CentralCapable) != 0,
            encryptable = (value and Encryptable) != 0,
            io = (value and IO) != 0
        )

        fun parse(bytes: ByteArray): XiaomiSensor {
            val header = parseHeader(bytes)
            var offset = 5

            var macAddress: String? = null
            if (header.flags.hasMacAddress) {
                macAddress =
                    (0..5).map { "%02x".format(bytes[offset + it]) }.reversed().joinToString(":")
                offset += 6
            }

            var capabilities: Capabilities? = null
            if (header.flags.hasCapabilities) {
                capabilities = parseCapabilities(bytes.get(offset).toInt())
                ++offset
            }

            var temperature: Double? = null
            var humidity: Double? = null
            var battery: Int? = null
            var luminance: Int? = null
            var moisture: Int? = null
            var fertility: Int? = null
            if (header.flags.hasEvent) {
                val eventId = readUnsignedShort(bytes, offset)
                offset += 2
                val dataLength = readByte(bytes, offset)
                ++offset
                if (eventId == Temperature) {
                    temperature = readShort(bytes, offset) / 10.0
                } else if (eventId == Humidity) {
                    humidity = readUnsignedShort(bytes, offset) / 10.0
                } else if (eventId == TemperatureAndHumidity) {
                    temperature = readShort(bytes, offset) / 10.0
                    humidity = readUnsignedShort(bytes, offset + 2) / 10.0
                } else if (eventId == KettleStatusAndTemperature) {
                    temperature = readByte(bytes, offset + 1).toDouble()
                } else if (eventId == Battery) {
                    battery = readByte(bytes, offset)
                } else if (eventId == Illuminance) {
                    luminance = readByte(bytes, offset)
                } else if (eventId == Moisture) {
                    moisture = readByte(bytes, offset)
                } else if (eventId == Fertility) {
                    fertility = readByte(bytes, offset)
                }
                offset += dataLength
            }

            return XiaomiSensor(
                header = header,
                address = macAddress,
                capabilities = capabilities,
                temperature = temperature,
                humidity = humidity,
                battery = battery,
                luminance = luminance,
                moisture = moisture,
                fertility = fertility
            )
        }

        private fun readByte(bytes: ByteArray, offset: Int) = bytes[offset].toInt() and 0xff
        private fun readShort(bytes: ByteArray, offset: Int) = ByteBuffer.wrap(bytes, offset, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short.toInt()

        private fun readUnsignedShort(bytes: ByteArray, offset: Int) =
            ByteBuffer.wrap(bytes, offset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .short.toInt() and 0xffff
    }
}

fun ByteArray.toHexString(separator: String = " ") =
    map { "%02x".format(it) }.joinToString(separator)
