package info.alkor.bleinquirer.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PermissionRequester(private val activity: Activity) {

    companion object {
        const val REQUEST_COARSE_LOCATION_PERMISSION = 213
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private fun await() = lock.withLock { condition.await() }
    private fun signal() = lock.withLock { condition.signalAll() }

    @WorkerThread
    fun requestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!permissionsGranted()) {
                runInUiThread {
                    val builder = AlertDialog.Builder(activity)
                    builder.apply {
                        setTitle("This app needs location update")
                        setMessage("Please grant location update so this app can detect beacons.")
                        setPositiveButton(android.R.string.ok, null)
                        setOnDismissListener {
                            activity.requestPermissions(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                REQUEST_COARSE_LOCATION_PERMISSION
                            )
                        }
                    }.show()
                }
                await()
            }
        }
        return permissionsGranted()
    }

    private fun permissionsGranted() =
        activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun runInUiThread(block: () -> Unit) = GlobalScope.launch(Dispatchers.Main) { block() }

    @UiThread
    fun onRequestPermissionsResult(
        requestCode: Int,
        @Suppress("UNUSED_PARAMETER") permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_COARSE_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                signal()
            } else {
                val builder = AlertDialog.Builder(activity)
                builder.apply {
                    setTitle("Functionality limited")
                    setMessage("Since location update has not been granted, this app will not be able to discover beacons when in the background.")
                    setPositiveButton(android.R.string.ok, null)
                    setOnDismissListener { }
                }.show()
            }
        }
    }
}
