package balti.migrate.simpleActivities

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import balti.migrate.R
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.utilities.CommonToolKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import kotlinx.android.synthetic.main.simple_log_display.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class SimpleLogViewer: AppCompatActivity() {

    private val commonTools by lazy { CommonToolKotlin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_log_display)

        logViewBackButton.setOnClickListener {
            finish()
        }

        logSendButton.setOnClickListener {
            commonTools.reportLogs(false)
        }

        class LoadLogText: AsyncTask<Any, Any, Any>(){

            lateinit var err : String
            lateinit var filePath : String

            override fun onPreExecute() {
                super.onPreExecute()
                log_view_progress_bar.visibility = View.VISIBLE
                logBody.text = ""
                err = ""

                intent?.let {
                    if (!it.hasExtra(SIMPLE_LOG_VIEWER_HEAD))
                        err += getString(R.string.no_header) + "\n"
                    else logViewHeader.text = it.getStringExtra(SIMPLE_LOG_VIEWER_HEAD)

                    if (!it.hasExtra(SIMPLE_LOG_VIEWER_FILEPATH))
                        err += getString(R.string.no_filepath) + "\n"
                    else filePath = it.getStringExtra(SIMPLE_LOG_VIEWER_FILEPATH)
                }

                err = err.trim()
            }

            override fun doInBackground(vararg params: Any?): Any {

                try {
                    intent?.let {

                        if (err != "") return 1

                        BufferedReader(FileReader(File(filePath))).readLines().forEach { it1 ->
                            publishProgress(it1)
                            commonTools.tryIt { Thread.sleep(1) }
                        }

                        return 0
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                    err += e.message.toString() + "\n"
                    return 1
                }

                return 1
            }

            override fun onProgressUpdate(vararg values: Any?) {
                super.onProgressUpdate(*values)
                logBody.append("${values[0]}\n")
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)

                log_view_progress_bar.visibility = View.GONE

                if (err != "") {
                    AlertDialog.Builder(this@SimpleLogViewer)
                            .setTitle(R.string.log_reading_error)
                            .setMessage(err)
                            .setNegativeButton(R.string.close) {_, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                }
            }

        }

        LoadLogText().execute()
    }

}