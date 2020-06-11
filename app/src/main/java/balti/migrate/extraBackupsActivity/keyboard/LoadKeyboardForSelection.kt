package balti.migrate.extraBackupsActivity.keyboard

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_USE_SU_FOR_KEYBOARD
import kotlinx.android.synthetic.main.keyboard_item.view.*
import kotlinx.android.synthetic.main.keyboard_selector.view.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class LoadKeyboardForSelection (private val jobCode: Int, val context: Context,
                                private val menuMainItem: LinearLayout,
                                private val menuSelectedStatus: TextView,
                                private val doBackupCheckbox: CheckBox,
                                private val itemList: ArrayList<BackupDataPacketKotlin> = ArrayList(0)):     //unique
        AsyncTask<Any, Any, Any>() {

    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }
    private val pm by lazy { context.packageManager }

    private val enabledKeyboards by lazy { ArrayList<String>(0) }
    private var error = ""

    override fun onPreExecute() {
        super.onPreExecute()
        vOp.clickableSet(menuMainItem, false)
        vOp.enableSet(doBackupCheckbox, false)
        vOp.visibilitySet(menuSelectedStatus, View.GONE)
    }

    override fun doInBackground(vararg params: Any?): Any? {

        try {

            val useSu = AppInstance.sharedPrefs.getBoolean(PREF_USE_SU_FOR_KEYBOARD, true)

            val keyboardReader = if (useSu)
                Runtime.getRuntime().exec("su")
            else Runtime.getRuntime().exec("ime list -s")

            if (useSu) {
                BufferedWriter(OutputStreamWriter(keyboardReader.outputStream)).let {
                    it.write("ime list -s\n")
                    it.write("exit\n")
                    it.flush()
                }
            }

            BufferedReader(InputStreamReader(keyboardReader.inputStream)).let {
                it.readLines().forEach {line ->
                    enabledKeyboards.add(line)
                }
            }

            BufferedReader(InputStreamReader(keyboardReader.errorStream)).let {
                it.readLines().forEach {line ->
                    error += line + "\n"
                }
            }

        } catch (e: Exception){
            e.printStackTrace()
            error += e.message
        }

        return null

    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        vOp.enableSet(doBackupCheckbox, true)

        when {
            error.trim() != "" -> onKeyboardError(vOp.getStringFromRes(R.string.error_reading_keyboard_list), error)

            enabledKeyboards.size == 0 -> onKeyboardError(vOp.getStringFromRes(R.string.no_keyboard_enabled),
                    vOp.getStringFromRes(R.string.no_keyboard_enabled_desc))

            enabledKeyboards.size == 1 -> {

                val kPackageName = enabledKeyboards[0].let {
                    if (it.contains("/"))
                        it.split("/")[0]
                    else it
                }

                try {

                    val kAppName = pm.getApplicationLabel(pm.getApplicationInfo(kPackageName, 0)).toString()
                    if (!isKeyboardInAppList(kPackageName)){
                        onKeyboardError("$kAppName ${vOp.getStringFromRes(R.string.selected_keyboard_not_present_in_backup)}",
                                vOp.getStringFromRes(R.string.selected_keyboard_not_present_in_backup_desc))
                    } else {
                        vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)
                        vOp.textSet(menuSelectedStatus, kAppName)
                        onJobCompletion.onComplete(jobCode, true, enabledKeyboards[0])
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                    onKeyboardError(vOp.getStringFromRes(R.string.error_reading_keyboard_list), e.message.toString())
                }

            }

            else -> try {

                val kSelectorView = View.inflate(context, R.layout.keyboard_selector, null)
                val keyboardSelectorDialog by lazy {
                    AlertDialog.Builder(context, R.style.DarkAlert)
                            .setView(kSelectorView)
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                vOp.checkSet(doBackupCheckbox, false)
                            }
                            .setCancelable(false)
                            .create()
                }

                for (enabledKeyboard in enabledKeyboards) {

                    val kPackageName = enabledKeyboard.let {
                        if (it.contains("/"))
                            it.split("/")[0]
                        else it
                    }

                    val kItem = View.inflate(context, R.layout.keyboard_item, null)

                    try {

                        kItem.keyboard_icon.setImageDrawable(pm.getApplicationIcon(kPackageName))
                        vOp.textSet(kItem.keyboard_name, pm.getApplicationLabel(pm.getApplicationInfo(kPackageName, 0)).toString())

                        if (isKeyboardInAppList(kPackageName)) {
                            vOp.visibilitySet(kItem.keyboard_present_in_backup_label, View.VISIBLE)
                        } else vOp.visibilitySet(kItem.keyboard_present_in_backup_label, View.GONE)

                        kItem.setOnClickListener {

                            if (!isKeyboardInAppList(kPackageName)) {
                                AlertDialog.Builder(context)
                                        .setTitle(kItem.keyboard_name.text.toString() + " " + vOp.getStringFromRes(R.string.selected_keyboard_not_present_in_backup))
                                        .setMessage(R.string.selected_keyboard_not_present_in_backup_desc)
                                        .setNegativeButton(R.string.close, null)
                                        .show()
                            } else {
                                vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)
                                vOp.textSet(menuSelectedStatus, kItem.keyboard_name.text.toString())
                                vOp.doSomething { keyboardSelectorDialog.dismiss() }
                                onJobCompletion.onComplete(jobCode, true, enabledKeyboard)
                            }

                        }

                        kSelectorView.keyboard_options_holder.addView(kItem)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onJobCompletion.onComplete(jobCode, false, e.message.toString())
                    }

                }

                keyboardSelectorDialog.show()

                vOp.clickableSet(menuMainItem, true)
                menuMainItem.setOnClickListener {
                    try {
                        keyboardSelectorDialog.show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onKeyboardError(vOp.getStringFromRes(R.string.error_reading_keyboard_list), e.message.toString())
                    }
                }

            } catch (e: Exception){
                e.printStackTrace()
                onKeyboardError(vOp.getStringFromRes(R.string.error_occurred), e.message.toString())
            }
        }

    }

    private fun isKeyboardInAppList(packageName: String): Boolean{
        itemList.forEach{
            if (it.PACKAGE_INFO.packageName == packageName) return true
        }
        return false
    }

    private fun onKeyboardError(title: String, errorMessage: String) {

        vOp.checkSet(doBackupCheckbox, false)
        vOp.textSet(menuSelectedStatus, "")
        vOp.visibilitySet(menuSelectedStatus, View.GONE)

        vOp.doSomething {
            AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_error)
                    .setTitle(title)
                    .setMessage(errorMessage)
                    .setNegativeButton(R.string.close, null)
                    .show()
        }

        onJobCompletion.onComplete(jobCode, false, title)

    }

}