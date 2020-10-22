package info.alkor.bleinquirer.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
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
            hideDialog()
        }

        val btnCancel: Button = findViewById(R.id.btn_cancel)
        btnCancel.setOnClickListener { hideDialog() }
    }

    private fun updateName(newName: String) {
        val swiDeleteName: SwitchCompat = findViewById(R.id.device_delete_name)
        val model = item
        if (model != null && (newName != model.name || swiDeleteName.isChecked)) {
            val app = context.applicationContext as BtLeApplication
            app.updateNameMapping(model, if (swiDeleteName.isChecked) "" else newName)
        }
    }

    private fun hideDialog() {
        hideKeyboard()
        dismiss()
    }

    fun showDialog(item: BtLeDeviceModel) {
        show()

        val tvAddress: TextView = findViewById(R.id.device_address)
        tvAddress.text = item.address

        val etName: EditText = findViewById(R.id.device_edit_name)
        etName.setText(item.name)
        etName.requestFocus()
        showKeyboard()

        this.item = item
    }

    private fun showKeyboard() =
        getInputMethodManager().toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

    private fun hideKeyboard() =
        getInputMethodManager().toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

    private fun getInputMethodManager() =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}