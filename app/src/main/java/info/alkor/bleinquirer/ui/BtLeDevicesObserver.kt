package info.alkor.bleinquirer.ui

import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil

class BtLeDevicesObserver(val adapter: BtLeDevicesAdapter) :
    Observer<List<BtLeDeviceModel>> {
    override fun onChanged(newItems: List<BtLeDeviceModel>) {
        val oldItems = adapter.getItems()
        val result = DiffUtil.calculateDiff(
            BtLeDevicesDiffCallback(
                oldItems,
                newItems
            )
        )
        adapter.replaceItems(newItems)
        result.dispatchUpdatesTo(adapter)
    }

    private class BtLeDevicesDiffCallback(
        val oldList: List<BtLeDeviceModel>,
        val newList: List<BtLeDeviceModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].address == newList[newItemPosition].address

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}