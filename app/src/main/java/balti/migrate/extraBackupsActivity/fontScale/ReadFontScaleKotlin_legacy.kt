package balti.migrate.extraBackupsActivity.fontScale

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

class ReadFontScaleKotlin_legacy(private val jobCode: Int,
                                 private val context: Context,
                                 private val menuMainItem: LinearLayout,
                                 private val menuSelectedStatus: TextView,
                                 private val menuReadProgressBar: ProgressBar,
                                 private val doBackupCheckbox: CheckBox
) : AsyncCoroutineTask() {


    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""

    private var scaleText = ""
    private var scale = 0.0

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
                    it.write("settings get system font_scale\n")
                    it.write("exit\n")
                    it.flush()
                }

                BufferedReader(InputStreamReader(dpiReader.inputStream)).let {
                    it.readLines().forEach { line ->
                        scaleText += line + "\n"
                    }
                }

                scaleText = scaleText.trim()

                BufferedReader(InputStreamReader(dpiReader.errorStream)).let {
                    it.readLines().forEach { line ->
                        error += line + "\n"
                    }
                }

                if (scaleText != "null") scale = scaleText.trim().toDouble()
            }
        } catch (e: Exception){
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

            if (scale < 0)
                error = "${vOp.getStringFromRes(R.string.weird_font_scale)} ($scale)\n\n$error".trim()

            if (error == "") {

                vOp.doSomething {

                    vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)

                    menuMainItem.setOnClickListener {
                        AlertDialog.Builder(context)
                                .setTitle(R.string.fontScale_label)
                                .setMessage(scale.toString())
                                .setNegativeButton(R.string.close, null)
                                .show()

                    }
                }
                onJobCompletion.onComplete(jobCode, true, if (scaleText != "null") scale else null)
            } else {
                onJobCompletion.onComplete(jobCode, false, error)
            }

            vOp.clickableSet(menuMainItem, error == "")
        }
    }
}