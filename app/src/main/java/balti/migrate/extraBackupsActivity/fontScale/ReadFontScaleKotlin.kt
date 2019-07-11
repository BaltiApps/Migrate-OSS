package balti.migrate.extraBackupsActivity.fontScale

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.R
import balti.migrate.extraBackupsActivity.OnJobCompletion
import balti.migrate.extraBackupsActivity.ViewOperations
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadFontScaleKotlin(private val jobCode: Int,
                          private val context: Context,
                          private val menuMainItem: LinearLayout,
                          private val menuSelectedStatus: TextView,
                          private val menuReadProgressBar: ProgressBar,
                          private val doBackupCheckbox: CheckBox
) : AsyncTask<Any, Any, Any>() {


    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""

    private var scaleText = ""
    private var scale = 0.0

    override fun onPreExecute() {
        super.onPreExecute()

        vOp.clickableSet(menuMainItem, false)
        vOp.enableSet(doBackupCheckbox, false)
        vOp.visibilitySet(menuSelectedStatus, View.GONE)
        vOp.visibilitySet(menuReadProgressBar, View.VISIBLE)
    }

    override fun doInBackground(vararg params: Any?): Any? {

        try {
            val dpiReader = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(dpiReader.outputStream)).let {
                it.write("settings get system font_scale\n")
                it.write("exit\n")
                it.flush()
            }

            BufferedReader(InputStreamReader(dpiReader.inputStream)).let {
                it.readLines().forEach {line ->
                    scaleText += line + "\n"
                }
            }

            BufferedReader(InputStreamReader(dpiReader.errorStream)).let {
                it.readLines().forEach {line ->
                    error += line + "\n"
                }
            }

            scale = scaleText.trim().toDouble()

        } catch (e: Exception){
            error += e.message
            e.printStackTrace()
        }

        return null
    }

    override fun onPostExecute(result: Any?) {
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
                onJobCompletion.onComplete(jobCode, true, scale)
            }
            else {
                vOp.checkSet(doBackupCheckbox, false)
                onJobCompletion.onComplete(jobCode, false, error)
            }

            vOp.clickableSet(menuMainItem, error == "")
        }
    }
}