package info.alkor.bleinquirer.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import info.alkor.bleinquirer.BtLeApplication
import info.alkor.bleinquirer.R

class DeviceEditDialog(ctx: Context) : Dialog(ctx) {

    private var item: BtLeDeviceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.details_dialog)

        val etName: EditText = findViewById(R.id.device_edit_name)

        val btnSave: Button = findViewById(R.id.btn_save)
        btnSave.setOnClickListener {
            updateName(etName.text.toString())
            dismiss()
        }

        val btnCancel: Button = findViewById(R.id.btn_cancel)
        btnCancel.setOnClickListener { dismiss() }
    }

    private fun updateName(newName: String) {
        val model = item
        if (model != null && newName != model.name) {
            val app = context.applicationContext as BtLeApplication
            app.updateNameMapping(model, newName)
        }
    }

    fun showDialog(item: BtLeDeviceModel) {
        show()

        val tvAddress: TextView = findViewById(R.id.device_address)
        tvAddress.text = item.address

        val etName: EditText = findViewById(R.id.device_edit_name)
        etName.setText(item.name)
        etName.requestFocus()

        this.item = item
    }
}