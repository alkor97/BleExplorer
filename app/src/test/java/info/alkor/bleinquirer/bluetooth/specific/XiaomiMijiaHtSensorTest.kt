package info.alkor.bleinquirer.bluetooth.specific

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XiaomiMijiaHtSensorTest {

    @Test
    fun testTemperatureAndHumidityParsing() {
        val data = bytes("50 20 aa 01 b4 f6 16 33 34 2d 58 0d 10 04 e8 00 5f 02")
        val sensor = XiaomiMijiaHtSensor(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertEquals(23.2, sensor.temperature)
        assertEquals(60.7, sensor.humidity)
    }

    @Test
    fun testTemperatureParsing() {
        val data = bytes("50 20 aa 01 c0 f6 16 33 34 2d 58 04 10 02 e8 00")
        val sensor = XiaomiMijiaHtSensor(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertEquals(23.2, sensor.temperature)
        assertNull(sensor.humidity)
    }

    @Test
    fun testHumidityParsing() {
        val data = bytes("50 20 aa 01 ba f6 16 33 34 2d 58 06 10 02 5f 02")
        val sensor = XiaomiMijiaHtSensor(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertNull(sensor.temperature)
        assertEquals(60.7, sensor.humidity)
    }

    @Test
    fun testBatteryParsing() {
        val data = bytes("50 20 aa 01 ba f6 16 33 34 2d 58 0a 10 01 5d")
        val sensor = XiaomiMijiaHtSensor(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertNull(sensor.temperature)
        assertNull(sensor.humidity)
        assertEquals(93, sensor.battery)
    }

    private fun bytes(data: String) = data.split(" ")
        .map { Integer.parseInt(it, 16).toByte() }
        .toByteArray()
}