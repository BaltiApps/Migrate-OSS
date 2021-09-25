package balti.migrate.extraBackupsActivity.fontScale

import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadFontScaleKotlin(fragment: FontScaleFragment): ParentReaderForExtras(fragment) {

    private var error = ""
    private var fontScaleText = ""
    private var scale = 0.0

    override val className: String = "ReadFontScaleKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        mainItem?.isClickable = false
        doBackupCheckBox?.isEnabled = false
        readStatusText?.visibility = View.GONE
        readProgressBar?.visibility = View.VISIBLE
    }

    override suspend fun backgroundProcessing(): ReaderJobResultHolder {

        writeLog("Starting reading")

        try {
            val dpiReader = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(dpiReader.outputStream)).let {

                writeLog("Writing to input buffer")

                heavyTask {
                    it.write("settings get system font_scale\n")
                    it.write("exit\n")
                    it.flush()
                }
            }

            BufferedReader(InputStreamReader(dpiReader.inputStream)).let {

                writeLog("Reading result buffer")

                it.readLines().forEach {line ->
                    writeLog("FONT_SCALE_RESULT: \'$line\'")
                    fontScaleText += line + "\n"
                }
            }

            BufferedReader(InputStreamReader(dpiReader.errorStream)).let {

                writeLog("Reading error buffer")

                it.readLines().forEach {line ->
                    writeLog("FONT_SCALE_ERROR: \'$line\'")
                    error += line + "\n"
                }
            }

            if (fontScaleText != "null") scale = fontScaleText.trim().toDouble()
            if (scale < 0)
                error = "${getStringFromRes(R.string.weird_font_scale)} ($scale)\n\n$error".trim()

        } catch (e: Exception){
            error += e.message
            e.printStackTrace()
        }

        return if (error == "") {
            writeLog("Read success. Read - $fontScaleText")
            ReaderJobResultHolder(true, scale)
        } else {
            writeLog("Read fail. Error - $error")
            ReaderJobResultHolder(false, error)
        }

    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        writeLog("Post execute")

        doBackupCheckBox?.isEnabled = true
        readProgressBar?.visibility = View.GONE

        if (error == "") {
            mainItem?.isClickable = true
            readStatusText?.visibility = View.VISIBLE
        }

        writeLog("Post execute complete")
    }

}