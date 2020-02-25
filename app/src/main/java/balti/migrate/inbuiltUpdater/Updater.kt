package balti.migrate.inbuiltUpdater

import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.THIS_VERSION
import balti.migrate.utilities.GetUpdateInfo
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_ERROR
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_LAST_TESTED_ANDROID
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_MESSAGE
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_NAME
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_STATUS
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_URL
import balti.migrate.utilities.GetUpdateInfo.Companion.UPDATE_VERSION
import kotlinx.android.synthetic.main.updater_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class Updater: AppCompatActivity() {

    private val commonTools by lazy { CommonToolKotlin(this) }
    private var updateUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.updater_activity)

        CoroutineScope(Main).launch {
            toggleLayout(1)
            val json = GetUpdateInfo().getInfo()
            if (json.has(UPDATE_ERROR)){
                commonTools.showErrorDialog(json.getString(UPDATE_ERROR), getString(R.string.update_check_error), false)
                toggleLayout(0)
            }
            else if (json.has(UPDATE_VERSION) && json.getInt(UPDATE_VERSION) <= THIS_VERSION){
                val ad = AlertDialog.Builder(this@Updater).apply {
                    val m = SpannableString(getString(R.string.no_new_version_desc))
                    Linkify.addLinks(m, Linkify.ALL)
                    setTitle(R.string.you_are_on_latest)
                    setMessage(m)
                    setCancelable(false)
                    setPositiveButton(R.string.close) {_, _ ->
                        finish()
                    }
                }
                        .create()

                ad.show()

                ad.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
            }
            else if (!json.has(UPDATE_URL)) {
                commonTools.showErrorDialog(getString(R.string.no_update_url), getString(R.string.update_check_error), false)
                toggleLayout(0)
            }
            else {
                toggleLayout(2)
                UPDATE_NAME.let { if (json.has(it)) update_name.text = json.getString(it) }
                UPDATE_VERSION.let { if (json.has(it)) update_version.text = json.getInt(it).toString() }
                UPDATE_LAST_TESTED_ANDROID.let { if (json.has(it)) update_last_android_version.text = json.get(it).toString() }
                UPDATE_STATUS.let { if (json.has(it)) update_status.text = json.getString(it) }
                UPDATE_MESSAGE.let { if (json.has(it)) update_info.text = json.getString(it) }
                updateUrl = json.getString(UPDATE_URL)
            }
        }
    }

    private fun toggleLayout(mode: Int){  // mode = 0 -> hide all layouts, else visible only layout with index (mode-1)
        val layouts = arrayOf(update_check_wait_layout, update_download_layout, update_installer_layout)
        if (mode >= 0 && mode < layouts.size)
            for (i in layouts.indices){
                layouts[i].visibility =
                        if (i == (mode-1)) View.VISIBLE
                        else View.GONE
            }
    }
}