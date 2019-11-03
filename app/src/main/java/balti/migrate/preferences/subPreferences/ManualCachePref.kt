package balti.migrate.preferences.subPreferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_CACHE_DEFAULT
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MANUAL_MIGRATE_CACHE

class ManualCachePref(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        val title by lazy { view.findViewById<TextView>(R.id.pref_et_title) }
        val editText by lazy { view.findViewById<EditText>(R.id.pref_et_editText) }
        val okButton by lazy { view.findViewById<Button>(R.id.pref_et_ok) }

        title.setText(R.string.manual_cache)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            editText.hint = MIGRATE_CACHE_DEFAULT
            editText.setText(AppInstance.sharedPrefs.getString(PREF_MANUAL_MIGRATE_CACHE, ""))

            okButton.setOnClickListener {
                editText.text.toString().let {
                    if (it != "") Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    editor.putString(PREF_MANUAL_MIGRATE_CACHE, it).apply()
                }
            }
        }
    }

}