package balti.migrate.preferences.subPreferences

import android.annotation.SuppressLint
import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import balti.migrate.AppInstance.Companion.MAX_EFFECTIVE_ZIP_SIZE
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MAX_BACKUP_SIZE
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefLong
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefLong

class ZipMaxSizePref(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    @SuppressLint("SetTextI18n")
    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        val text by lazy { view.findViewById<TextView>(R.id.pref_max_zip_value_text) }
        val seekBar by lazy { view.findViewById<SeekBar>(R.id.pref_max_zip_seekbar) }

        fun getGb(bytes: Long): Float = (bytes / 1024.0 / 1024.0 / 1024.0).toFloat()

        val maxEffective = MAX_EFFECTIVE_ZIP_SIZE
        val parts = getGb(maxEffective).let {
            when {
                it > 3.5 -> 4
                it > 2.5 -> 3
                it > 1.5 -> 2
                else -> 1
            }
        }

        text.text = "${"%.2f".format(getGb(MAX_WORKING_SIZE))} GB"

        seekBar.apply {
            max = parts
            progress = getPrefLong(PREF_MAX_BACKUP_SIZE, maxEffective).let {
                (it / (maxEffective / (parts+1.0))).toInt() -1
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val selectedSize = (maxEffective * ((progress+1.0) / (parts+1.0))).toLong()
                    text.text = "${"%.2f".format(getGb(selectedSize))} GB"
                    putPrefLong(PREF_MAX_BACKUP_SIZE, selectedSize)
                    MAX_WORKING_SIZE = selectedSize

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })
        }
    }

}