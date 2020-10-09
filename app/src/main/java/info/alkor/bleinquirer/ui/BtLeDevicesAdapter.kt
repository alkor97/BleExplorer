package info.alkor.bleinquirer.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.alkor.bleinquirer.R
import kotlinx.android.synthetic.main.devices_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class BtLeDevicesAdapter(private val context: Context) : RecyclerView.Adapter<BtLeDevicesAdapter.ViewHolder>() {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
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
        holder.tvDeviceBatteryLevel?.text =
            if (item.batteryLevel != null) "${item.batteryLevel}%" else "-"
        holder.tvDeviceTemperature?.text =
            if (item.temperature != null) "${item.temperature}°C" else "-"
        holder.tvDeviceHumidity?.text =
            if (item.humidity != null) "${item.humidity}%" else "-"
        holder.tvDeviceLuminance?.text =
            if (item.luminance != null) "${item.luminance} lx" else "-"
        holder.tvDeviceMoisture?.text =
            if (item.moisture != null) "${item.moisture}%" else "-"
        holder.tvDeviceFertility?.text =
            if (item.fertility != null) "${item.fertility} µS/cm" else "-"
        holder.tvDeviceError?.apply {
            if (item.error != null) {
                text = item.error
                this.setLines(1 + item.error.count { it == '\n' })
            } else {
                text = "-"
            }
        }
        holder.tvDeviceDate?.text = timeFormatter.format(item.lastUpdate)

        holder.vDeviceBatterLevelColumn?.visibility =
            if (item.batteryLevel != null) View.VISIBLE else View.GONE
        holder.vDeviceTemperatureColumn?.visibility =
            if (item.temperature != null) View.VISIBLE else View.GONE
        holder.vDeviceHumidityColumn?.visibility =
            if (item.humidity != null) View.VISIBLE else View.GONE
        holder.vDeviceLuminanceColumn?.visibility =
            if (item.luminance != null) View.VISIBLE else View.GONE
        holder.vDeviceMoistureColumn?.visibility =
            if (item.moisture != null) View.VISIBLE else View.GONE
        holder.vDeviceFertilityColumn?.visibility =
            if (item.fertility != null) View.VISIBLE else View.GONE
        holder.vDeviceErrorColumn?.visibility =
            if (item.error != null) View.VISIBLE else View.GONE
        holder.vDeviceDateColumn?.visibility = View.VISIBLE
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeviceAddress: TextView? = view.device_address
        val tvDeviceName: TextView? = view.device_name
        val tvDeviceBatteryLevel: TextView? = view.device_battery_level
        val tvDeviceTemperature: TextView? = view.device_temperature
        val tvDeviceHumidity: TextView? = view.device_humidity
        val tvDeviceError: TextView? = view.device_error
        val tvDeviceDate: TextView? = view.device_date
        val tvDeviceLuminance: TextView? = view.device_luminance
        val tvDeviceMoisture: TextView? = view.device_moisture
        val tvDeviceFertility: TextView? = view.device_fertility

        val vDeviceBatterLevelColumn: View? = view.device_battery_level_column
        val vDeviceTemperatureColumn: View? = view.device_temperature_column
        val vDeviceHumidityColumn: View? = view.device_humidity_column
        val vDeviceErrorColumn: View? = view.device_error_column
        val vDeviceDateColumn: View? = view.device_date_column
        val vDeviceLuminanceColumn: View? = view.device_luminance_column
        val vDeviceMoistureColumn: View? = view.device_moisture_column
        val vDeviceFertilityColumn: View? = view.device_fertility_column
    }
}
