package balti.migrate.extraBackupsActivity.wifi

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.WIFI_FILE_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.WIFI_FILE_NOT_FOUND
import balti.migrate.utilities.CommonToolKotlin.Companion.WIFI_FILE_PATH
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadWifiKotlin(private val jobCode: Int,
                     private val context: Context,
                     private val menuMainItem: LinearLayout,
                     private val menuSelectedStatus: TextView,
                     private val menuReadProgressBar: ProgressBar,
                     private val doBackupCheckbox: CheckBox
                     ) : AsyncTask<Any, Any, Any>() {


    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""

    private val command =
                    "if [ -f $WIFI_FILE_PATH ]; then\n" +
                    "   cat $WIFI_FILE_PATH\n" +
                    "else\n" +
                    "   echo \"$WIFI_FILE_NOT_FOUND\"\n" +
                    "fi\n"

    private val contents by lazy { ArrayList<String>(0) }

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
                it.write(command)
                it.write("exit\n")
                it.flush()
            }

            BufferedReader(InputStreamReader(dpiReader.inputStream)).let {
                it.readLines().forEach {line ->
                    contents.add(line)
                }
            }

            BufferedReader(InputStreamReader(dpiReader.errorStream)).let {
                it.readLines().forEach {line ->
                    error += line + "\n"
                }
            }
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

            val tv = TextView(context)
            tv.text = ""
            contents.forEach {
                tv.append("$it\n")
            }
            tv.apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(20, 20, 20, 20)
            }

            if (tv.text.toString().trim() == WIFI_FILE_NOT_FOUND)
                error = "$WIFI_FILE_NOT_FOUND\n\n$error".trim()

            if (error == "") {

                vOp.doSomething {

                    val sc = ScrollView(context)
                    sc.addView(tv)

                    val ad = AlertDialog.Builder(context)
                            .setTitle("${vOp.getStringFromRes(R.string.wifi_label)}\n$WIFI_FILE_NAME")
                            .setView(sc)
                            .setNegativeButton(R.string.close, null)
                            .create()

                    vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)

                    menuMainItem.setOnClickListener {
                        ad.show()
                    }
                }
                onJobCompletion.onComplete(jobCode, true, WifiDataPacket(WIFI_FILE_NAME, contents))
            } else {
                onJobCompletion.onComplete(jobCode, false, error)
            }

            vOp.clickableSet(menuMainItem, error == "")
        }
    }

}