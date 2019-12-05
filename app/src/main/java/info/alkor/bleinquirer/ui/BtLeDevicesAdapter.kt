package info.alkor.bleinquirer.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.alkor.bleinquirer.R
import kotlinx.android.synthetic.main.devices_list_item.view.*

class BtLeDevicesAdapter(private val context: Context) : RecyclerView.Adapter<BtLeDevicesAdapter.ViewHolder>() {

    private val items = mutableListOf<BtLeDeviceModel>()

    fun getItems() = items as List<BtLeDeviceModel>

    fun replaceItems(newItems: List<BtLeDeviceModel>) {
        items.clear()
        items.addAll(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.devices_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDeviceAddress?.text = item.address
        holder.tvDeviceName?.text = item.name ?: "-"
        holder.tvDeviceBatteryLevel?.text = if (item.batteryLevel != null) "%d%%".format(item.batteryLevel) else "-"
        holder.tvDeviceError?.apply {
            if (item.error != null) {
                text = item.error
                this.setLines(1 + item.error.count { it == '\n' })
            } else {
                text = "-"
            }
        }

        holder.vDeviceBatterLevelColumn?.visibility = if (item.batteryLevel != null) View.VISIBLE else View.GONE
        holder.vDeviceErrorColumn?.visibility = if (item.error != null) View.VISIBLE else View.GONE
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeviceAddress: TextView? = view.device_address
        val tvDeviceName: TextView? = view.device_name
        val tvDeviceBatteryLevel: TextView? = view.device_battery_level
        val tvDeviceError: TextView? = view.device_error

        val vDeviceBatterLevelColumn: View? = view.device_battery_level_column
        val vDeviceErrorColumn: View? = view.device_error_column
    }
}
