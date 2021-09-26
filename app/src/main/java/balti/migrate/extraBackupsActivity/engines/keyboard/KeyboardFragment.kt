package balti.migrate.extraBackupsActivity.engines.keyboard

import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.module.baltitoolbox.functions.Misc

class KeyboardFragment: ParentFragmentForExtras(R.layout.extra_fragment_keyboard) {

    override val readTask: ParentReaderForExtras? = null

    override fun onCreateView(savedInstanceState: Bundle?) {
        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    updateKeyboard(
                        Settings.Secure.getString(mActivity?.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).trim()
                    )
                }
                else {
                    deselectExtra(null, listOf(delegateStatusText))
                    AppInstance.keyboardText = null
                }
            }
        }
    }

    private fun getKeyboardPackageName(): String? = AppInstance.keyboardText?.let {
        if (it.contains("/"))
            it.split("/")[0]
        else it
    }

    private fun getKeyboardAppName(): String? = getKeyboardPackageName()?.let {
        val kName = Misc.getAppName(it)
        if (kName.isNotBlank()) kName
        else it
    }

    private fun updateKeyboard(keyboardText: String){
        AppInstance.keyboardText = keyboardText

        val keyboardAppName = getKeyboardAppName()
        val keyboardPackage = getKeyboardPackageName()

        delegateStatusText?.apply {
            visibility = View.VISIBLE
            text = keyboardAppName
        }

        delegateMainItem?.setOnClickListener {
            mActivity?.let { it1 ->

                val message = "${getString(R.string.keyboard_app_name_label)} : $keyboardAppName\n\n" +
                        "${getString(R.string.keyboard_package_label)} : $keyboardPackage\n\n" +
                        "${getString(R.string.keyboard_string_label)} : $keyboardText\n\n"

                AlertDialog.Builder(it1)
                    .setTitle(R.string.keyboard_label)
                    .setMessage(message)
                    .setNegativeButton(R.string.close, null)
                    .show()
            }

        }
    }

    override val viewIdStatusText: Int = R.id.keyboard_read_text_status
    override val viewIdCheckbox: Int = R.id.keyboard_fragment_checkbox
}