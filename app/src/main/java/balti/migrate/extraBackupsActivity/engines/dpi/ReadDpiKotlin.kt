package balti.migrate.extraBackupsActivity.engines.dpi

import android.view.View
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadDpiKotlin(fragment: DpiFragment): ParentReaderForExtras(fragment) {

    private var error = ""
    private var dpiText = ""

    override val className: String = "ReadDpiKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        mainItem?.isClickable = false
        doBackupCheckBox?.isEnabled = false
        readStatusText?.visibility = View.GONE
        readProgressBar?.visibility = View.VISIBLE
    }

    override suspend fun doInBackground(arg: Any?): Any {

        writeLog("Starting reading")

        try {
            val dpiReader = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(dpiReader.outputStream)).let {

                writeLog("Writing to input buffer")

                heavyTask {
                    it.write("wm density\n")
                    it.write("exit\n")
                    it.flush()
                }
            }

            BufferedReader(InputStreamReader(dpiReader.inputStream)).let {

                writeLog("Reading result buffer")

                it.readLines().forEach {line ->
                    writeLog("DPI_RESULT: \'$line\'")
                    dpiText += line + "\n"
                }
            }

            BufferedReader(InputStreamReader(dpiReader.errorStream)).let {

                writeLog("Reading error buffer")

                it.readLines().forEach {line ->
                    writeLog("DPI_ERROR: \'$line\'")
                    error += line + "\n"
                }
            }
        } catch (e: Exception){
            error += e.message
            e.printStackTrace()
        }

        doOnMainThreadParallel {
            doBackupCheckBox?.isEnabled = true
            readProgressBar?.visibility = View.GONE
        }

        return if (error == "") {
            doOnMainThreadParallel {
                mainItem?.isClickable = true
                readStatusText?.visibility = View.VISIBLE
            }
            writeLog("Read success. Read - $dpiText")
            ReaderJobResultHolder(true, dpiText)
        } else {
            writeLog("Read fail. Error - $error")
            ReaderJobResultHolder(false, error)
        }
    }
}