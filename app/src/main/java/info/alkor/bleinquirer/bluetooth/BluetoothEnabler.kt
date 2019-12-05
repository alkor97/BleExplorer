package info.alkor.bleinquirer.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BluetoothEnabler(private val activity: Activity) {

    companion object {
        const val REQUEST_ENABLE_BT = 317
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private fun await() = lock.withLock { condition.await() }
    private fun signal() = lock.withLock { condition.signalAll() }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    @WorkerThread
    fun ensureBluetoothIsEnabled(): Boolean {
        if (bluetoothAdapter != null) {
            bluetoothAdapter?.apply {
                if (!isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivityForResult(
                        enableBtIntent,
                        REQUEST_ENABLE_BT
                    )
                    Log.d("ble", "Bluetooth enabling requested")
                    await()
                }
            }
        }
        val enabled = bluetoothAdapter?.isEnabled ?: false
        Log.d("ble", "Bluetooth is %sabled".format(if (enabled) "en" else "dis"))
        return enabled
    }

    @UiThread
    fun onActivityResult(
        requestCode: Int,
        resultCode: Int, @Suppress("UNUSED_PARAMETER") data: Intent?
    ) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            Log.i("ble", "user rejected request of enabling Bluetooth")
        }
        signal()
    }
}
