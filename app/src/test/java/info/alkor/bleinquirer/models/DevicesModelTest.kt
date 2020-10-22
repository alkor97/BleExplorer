package info.alkor.bleinquirer.models

import android.bluetooth.BluetoothDevice
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import info.alkor.bleinquirer.bluetooth.specific.XiaomiSensor
import info.alkor.bleinquirer.persistence.BtNameMapper
import info.alkor.bleinquirer.ui.BtLeDeviceModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DevicesModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val nameMapper = mockk<BtNameMapper>()
    private val instance = DevicesModel(nameMapper)

    private val address = "12:34:56:78:90:AB"
    private val name = "device#1"
    private var device = mockk<BluetoothDevice>()

    @Before
    fun before() {
        every { device.address } returns address
        every { device.name } returns name
    }

    @Test
    fun `device is added`() {
        mockNameMapping(device, name)

        instance.addDevice(device, null)
        verifyNonNullDevice {
            assertEquals(address, it.address)
            assertEquals(name, it.name)
            assertFalse(it.useCustomName)
        }
    }

    @Test
    fun `device is added successfully and uses custom name`() {
        val customName = "custom-name"
        assertNotEquals(name, customName)

        mockNameMapping(device, customName)

        instance.addDevice(device, null)
        verifyNonNullDevice {
            assertEquals(address, it.address)
            assertEquals(customName, it.name)
            assertTrue(it.useCustomName)
        }
    }

    @Test
    fun `device name is updated and uses custom name`() {
        val customName = "custom-name"
        assertNotEquals(name, customName)

        mockNameMapping(device, name)

        instance.addDevice(device, null)
        verifyNonNullDevice {
            assertEquals(address, it.address)
            assertEquals(name, it.name)
            assertFalse(it.useCustomName)
        }

        instance.updateDeviceName(address, customName)

        verifyNonNullDevice {
            assertEquals(address, it.address)
            assertEquals(customName, it.name)
            assertTrue(it.useCustomName)
        }
    }

    @Test
    fun `device details are added`() {
        mockNameMapping(device, name)

        val sensor = XiaomiSensor(
            header = mockk(), address,
            temperature = 12.3, humidity = 35.0, battery = 97, luminance = 543, moisture = 123,
            fertility = 534
        )
        instance.addDevice(device, sensor)

        verifyNonNullDevice {
            assertEquals(sensor.temperature, it.temperature)
            assertEquals(sensor.humidity, it.humidity)
            assertEquals(sensor.battery, it.battery)
            assertEquals(sensor.luminance, it.luminance)
            assertEquals(sensor.moisture, it.moisture)
            assertEquals(sensor.fertility, it.fertility)
        }
    }

    @Test
    fun `device details are updated`() {
        mockNameMapping(device, name)

        val original = XiaomiSensor(
            header = mockk(), address,
            temperature = 12.3, humidity = 35.0, battery = 97, luminance = 543, moisture = 123,
            fertility = 534
        )
        instance.addDevice(device, original)

        val updated = XiaomiSensor(
            header = mockk(), address,
            temperature = 12.4, humidity = 35.1, battery = 96, luminance = 544, moisture = 124,
            fertility = 535
        )
        assertNotEquals(updated, original)
        instance.updateDevice(device, updated)

        verifyNonNullDevice {
            assertEquals(updated.temperature, it.temperature)
            assertEquals(updated.humidity, it.humidity)
            assertEquals(updated.battery, it.battery)
            assertEquals(updated.luminance, it.luminance)
            assertEquals(updated.moisture, it.moisture)
            assertEquals(updated.fertility, it.fertility)
            assertFalse(it.useCustomName)
        }
    }

    @Test
    fun `device is updated and custom name is not overwritten`() {
        val customName = "custom-name"
        assertNotEquals(name, customName)

        mockNameMapping(device, name)

        instance.addDevice(device, null)
        verifyNonNullDevice {
            // after adding original name is used
            assertEquals(name, it.name)
            assertFalse(it.useCustomName)
        }

        instance.updateDeviceName(address, customName)
        verifyNonNullDevice {
            // custom name is set
            assertEquals(customName, it.name)
            assertTrue(it.useCustomName)
        }

        instance.updateDevice(device, null)
        verifyNonNullDevice {
            // updating device details preserves custom name
            assertEquals(customName, it.name)
            assertTrue(it.useCustomName)
        }
    }

    private fun mockNameMapping(device: BluetoothDevice, customName: String) {
        coEvery { nameMapper.getName(device) } returns customName
    }

    private fun verifyNonNullDevice(verifier: (BtLeDeviceModel) -> Unit) {
        val device = getDevice()
        assertNotNull(device)
        device?.let(verifier)
    }

    private fun getDevice() = instance.devices().value!!.getOrNull(0)
}