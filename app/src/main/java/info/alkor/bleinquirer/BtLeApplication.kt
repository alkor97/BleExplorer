package info.alkor.bleinquirer

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import com.bluetooth.tools.Characteristic
import com.bluetooth.tools.Service
import info.alkor.bleinquirer.bluetooth.BluetoothReader
import info.alkor.bleinquirer.bluetooth.BtLeScanner
import info.alkor.bleinquirer.bluetooth.description
import info.alkor.bleinquirer.bluetooth.specific.XiaomiSensor
import info.alkor.bleinquirer.bluetooth.specific.toHexString
import info.alkor.bleinquirer.ui.BtLeDeviceModel
import info.alkor.bleinquirer.ui.BtNameMapper
import info.alkor.bleinquirer.utils.LiveObject
import info.alkor.bleinquirer.utils.Timeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BtLeApplication : Application() {

    private val btAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanningInProgress =
        LiveObject(AtomicBoolean(false)) { it.get() }
    fun isScanningInProgress(): LiveData<Boolean> = scanningInProgress.live()

    private val devicesModel =
        LiveObject<List<BtLeDeviceModel>, MutableList<BtLeDeviceModel>>(mutableListOf())

    fun getDevicesModel() = devicesModel.live()

    private fun setScanningInProgress() = scanningInProgress.update { it.compareAndSet(false, true) }
    private fun resetScanningInProgress() = scanningInProgress.update {
        it.set(false)
        true
    }

    private var scanner: BtLeScanner? = null
    private val nameMapper: BtNameMapper by lazy { BtNameMapper() }

    fun scanForDevices(): Boolean {
        val scanTimeout = DEFAULT_SCAN_TIMEOUT

        if (setScanningInProgress()) {
            GlobalScope.launch {
                try {
                    scanForDevices(scanTimeout)
                    showToast("Scanning completed with %d devices".format(devicesModel.live().value?.size))
                    resetScanningInProgress()
                } catch (e: Throwable) {
                    Log.e("BtCoroutine", "exception while processing", e)
                }
            }
            return true
        }
        return false
    }

    private fun scanForDevices(timeout: Timeout) {
        if (scanner == null) {
            try {
                scanner = BtLeScanner(btAdapter!!)
                scanner?.let {
                    it.scan(timeout) { result ->
                        val device = result.device
                        if (device.name != null) {
                            addOrUpdateDevice(result)
                        }
                    }?.apply {
                        showToast("Scanning failed due to %s".format(this))
                    }
                }
            } finally {
                scanner = null
            }
        }
    }

    fun stopScanning() {
        scanner?.stopScanning("stopped by user")
    }

    private fun addOrUpdateDevice(result: ScanResult) {
        val date = Date()
        val device = result.device
        val sensor =
            result.scanRecord?.serviceData?.filter { Characteristic.MI_SERVICE.uuid == it.key.uuid }
                ?.map { XiaomiSensor.parse(it.value) }
                ?.onEach { Log.i("xiaomi", device.name + ": " + it.toString()) }
                ?.getOrNull(0)
        result.scanRecord?.serviceData?.onEach {
            val service = it.key.uuid.getDescription()
            Log.d(result.device.description, "$service = ${it.value.toHexString()}")
        }
        devicesModel.update { list ->
            var found = false
            for (i in 0 until list.size) {
                val model = list[i]
                if (model.address == device.address) {
                    if (sensor != null) {
                        list[i] = BtLeDeviceModel(
                            model.address,
                            getName(device),
                            sensor.battery ?: model.batteryLevel,
                            null,
                            sensor.temperature ?: model.temperature,
                            sensor.humidity ?: model.humidity,
                            sensor.luminance ?: model.luminance,
                            sensor.moisture ?: model.moisture,
                            sensor.fertility ?: model.fertility,
                            date
                        )
                    }
                    found = true
                    break
                }
            }
            if (!found) {
                list.add(
                    BtLeDeviceModel(
                        device.address,
                        getName(device),
                        sensor?.battery,
                        null,
                        sensor?.temperature,
                        sensor?.humidity,
                        sensor?.luminance,
                        sensor?.moisture,
                        sensor?.fertility,
                        date
                    )
                )
            }
            true
        }
    }

    private fun getName(device: BluetoothDevice) = nameMapper.getName(device)

    private fun showToast(text: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun readBatteryLevel(
        device: BluetoothDevice,
        timeout: Timeout,
        reporter: (String) -> Unit
    ) = BluetoothReader(
        applicationContext,
        device
    ).readBatteryLevel(timeout, reporter)

    companion object {
        val DEFAULT_SCAN_TIMEOUT =
            Timeout(30, TimeUnit.SECONDS)
        val DEFAULT_BATTERY_READ_TIMEOUT =
            Timeout(30, TimeUnit.SECONDS)
    }
}

fun UUID.getDescription(): String {
    val service = Service.fromUuid(this)
    if (service != null) {
        return service.fullName
    } else {
        return Characteristic.getFullName(this)
    }
}
