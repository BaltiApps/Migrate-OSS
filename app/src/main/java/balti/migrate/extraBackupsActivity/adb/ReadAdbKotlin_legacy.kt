package balti.migrate.extraBackupsActivity.adb

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadAdbKotlin_legacy(private val jobCode: Int,
                           private val context: Context,
                           private val menuMainItem: LinearLayout,
                           private val menuSelectedStatus: TextView,
                           private val menuReadProgressBar: ProgressBar,
                           private val doBackupCheckbox: CheckBox
) : AsyncCoroutineTask() {


    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""

    private var adbText = ""
    private var adbState = 0

    override suspend fun onPreExecute() {
        super.onPreExecute()

        vOp.clickableSet(menuMainItem, false)
        vOp.enableSet(doBackupCheckbox, false)
        vOp.visibilitySet(menuSelectedStatus, View.GONE)
        vOp.visibilitySet(menuReadProgressBar, View.VISIBLE)
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        try {
            heavyTask {
                val dpiReader = Runtime.getRuntime().exec("su")
                BufferedWriter(OutputStreamWriter(dpiReader.outputStream)).let {
                    it.write("settings get global adb_enabled\n")
                    it.write("exit\n")
                    it.flush()
                }

                BufferedReader(InputStreamReader(dpiReader.inputStream)).let {
                    it.readLines().forEach { line ->
                        adbText += line + "\n"
                    }
                }

                adbText = adbText.trim()

                BufferedReader(InputStreamReader(dpiReader.errorStream)).let {
                    it.readLines().forEach { line ->
                        error += line + "\n"
                    }
                }
                adbState = adbText.trim().toInt()
            }
        } catch (e: Exception) {
            error += adbText + "\n\n"
            error += e.message
            e.printStackTrace()
        }

        return null
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        vOp.doSomething {

            vOp.enableSet(doBackupCheckbox, true)
            vOp.visibilitySet(menuReadProgressBar, View.GONE)

            if (adbState != 0 && adbState != 1)
                error = "${vOp.getStringFromRes(R.string.adb_unknown)} ($adbState)\n\n$error".trim()

            if (error == "") {

                vOp.doSomething {

                    vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)

                    menuMainItem.setOnClickListener {
                        AlertDialog.Builder(context)
                                .setTitle(R.string.adb_label)
                                .setNegativeButton(R.string.close, null)
                                .apply {
                                    when (adbState) {
                                        0 -> setMessage("${vOp.getStringFromRes(R.string.adb_disabled)} ($adbState)")
                                        1 -> setMessage("${vOp.getStringFromRes(R.string.adb_enabled)} ($adbState)")
                                    }
                                }
                                .show()

                    }
                }
                onJobCompletion.onComplete(jobCode, true, if (adbText != "null") adbState else null)
            } else {
                onJobCompletion.onComplete(jobCode, false, error)
            }

            vOp.clickableSet(menuMainItem, error == "")
        }
    }
}