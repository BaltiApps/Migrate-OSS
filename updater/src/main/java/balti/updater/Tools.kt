package balti.updater

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class Tools(val context: Context? = null) {

    fun tryIt(f: () -> Unit){
        try { f() } catch (e: Exception){}
    }


    fun showErrorDialog(message: String, title: String = "", isCancelable: Boolean = true){
        if (context == null) return
        try {
            AlertDialog.Builder(context)
                    .setMessage(message).apply {

                        if (isCancelable)
                            setNegativeButton(R.string.close, null)
                        else {
                            setCancelable(false)
                            setNegativeButton(R.string.close) { _, _ ->
                                if (context is AppCompatActivity) {
                                    (context as AppCompatActivity).finish()
                                }
                            }
                        }

                        if (title == "")
                            setTitle(R.string.error_occurred)
                        else setTitle(title)

                    }
                    .show()
        } catch (e: Exception){
            e.printStackTrace()
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception){}
        }
    }
}