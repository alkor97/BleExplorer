package info.alkor.bleinquirer.bluetooth.specific

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XiaomiSensorTest {

    @Test
    fun testTemperatureAndHumidityParsing() {
        val data = bytes("50 20 aa 01 b4 f6 16 33 34 2d 58 0d 10 04 e8 00 5f 02")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertEquals(23.2, sensor.temperature)
        assertEquals(60.7, sensor.humidity)
    }

    @Test
    fun testNegativeTemperatureAndHumidityParsing() {
        val data = bytes("50 20 aa 01 28 36 f9 32 34 2d 58 0d 10 04 da ff 60 02")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:32:f9:36", sensor.address)
        assertEquals(-3.8, sensor.temperature)
        assertEquals(60.8, sensor.humidity)
    }

    @Test
    fun testTemperatureParsing() {
        val data = bytes("50 20 aa 01 c0 f6 16 33 34 2d 58 04 10 02 e8 00")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertEquals(23.2, sensor.temperature)
        assertNull(sensor.humidity)
    }

    @Test
    fun testNegativeTemperatureParsing() {
        val data = bytes("50 20 aa 01 60 36 f9 32 34 2d 58 04 10 02 d7 ff")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:32:f9:36", sensor.address)
        assertEquals(-4.1, sensor.temperature)
        assertNull(sensor.humidity)
    }

    @Test
    fun testHumidityParsing() {
        val data = bytes("50 20 aa 01 ba f6 16 33 34 2d 58 06 10 02 5f 02")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertNull(sensor.temperature)
        assertEquals(60.7, sensor.humidity)
    }

    @Test
    fun testBatteryParsing() {
        val data = bytes("50 20 aa 01 ba f6 16 33 34 2d 58 0a 10 01 5d")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("58:2d:34:33:16:f6", sensor.address)
        assertNull(sensor.temperature)
        assertNull(sensor.humidity)
        assertEquals(93, sensor.battery)
    }

    @Test
    fun testMoistureParsing() {
        val data = bytes("71 20 98 00 bf 1f af 88 ca ea 80 0d 08 10 01 11")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("80:ea:ca:88:af:1f", sensor.address)
        assertEquals(17, sensor.moisture)
    }

    @Test
    fun testFertilityParsing() {
        val data = bytes("71 20 98 00 60 1f af 88 ca ea 80 0d 09 10 02 6d 0c")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("80:ea:ca:88:af:1f", sensor.address)
        assertEquals(109, sensor.fertility)
    }

    @Test
    fun testLuminanceParsing() {
        val data = bytes("71 20 98 00 62 1f af 88 ca ea 80 0d 07 10 03 4f 00 00")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("80:ea:ca:88:af:1f", sensor.address)
        assertEquals(79, sensor.luminance)
    }

    @Test
    fun testKettleTemperature() {
        val data = bytes("71 20 13 01 71 f2 5f 44 6f 7c b8 09 05 10 02 00 15")
        val sensor = XiaomiSensor.parse(data)

        assertEquals("b8:7c:6f:44:5f:f2", sensor.address)
        assertEquals(21.0, sensor.temperature)
    }

    private fun bytes(data: String) = data.split(" ")
        .map { Integer.parseInt(it, 16).toByte() }
        .toByteArray()
}