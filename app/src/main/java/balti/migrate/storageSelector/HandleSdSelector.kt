package balti.migrate.storageSelector

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import balti.filex.FileX
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import kotlinx.android.synthetic.main.storage_selector_sd_cards.view.*

/**
 * To be called from [StorageSelectorActivity]
 */
class HandleSdSelector(context: Context) {

    private val paths by lazy { arrayListOf(CommonToolsKotlin.DEFAULT_INTERNAL_STORAGE_DIR) }
    private val rootView by lazy { View.inflate(context, R.layout.storage_selector_sd_cards, null) }
    private var storagePointer = 0

    init {
        rootView.apply {

            // find all available SD card paths and store them.
            // also create the radio buttons for the corresponding SD card paths
            // NOTE: index (and id) of each SD card related radio button start from 1.
            val allSdCards = getTraditionalSdCardPaths()
            allSdCards.let { SDs ->

                SDs.forEach {
                    val sdFile = FileX.new(it, true)
                    val radioButton = RadioButton(context).apply {
                        this.text = sdFile.name
                        this.id = ++storagePointer
                        paths.add(it)
                    }
                    radio_group_sd_cards.addView(radioButton)
                }

                storagePointer = 0
            }

            storage_show_sd_cards.setOnClickListener {
                it.visibility = View.GONE

                // if available SD cards is not blank, show the radio group containing the options for SD cards.
                // else show message for no SD card
                if (allSdCards.isEmpty()){
                    no_sd_cards.visibility = View.VISIBLE
                    radio_group_sd_cards.visibility = View.GONE
                }
                else {
                    no_sd_cards.visibility = View.GONE
                    radio_group_sd_cards.visibility = View.VISIBLE
                }
            }

            // Listener when any SD card related radio button is selected from the radio group.
            // update storagePointer when SD card related radio button is pressed.
            // also clear radio button for internal storage.
            val onCheckedChangedListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->

                // if radio group is cleared, checkedId = -1. So only check if checkedId > 0.
                // also all id for SD card related buttons start from 1.
                if (checkedId > 0) {
                    storagePointer = checkedId
                    radio_internal_storage.isChecked = false
                }
            }

            radio_group_sd_cards.setOnCheckedChangeListener(onCheckedChangedListener)

            // if internal storage is checked, set storagePointer = 0.
            // also clear SD card radio group.
            radio_internal_storage.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked){
                    storagePointer = 0
                    // listener is being made null to prevent listener from being called even from clearCheck()
                    radio_group_sd_cards.setOnCheckedChangeListener(null)
                    radio_group_sd_cards.clearCheck()
                    radio_group_sd_cards.setOnCheckedChangeListener(onCheckedChangedListener)
                }
            }

            // check previously selected storage and select the appropriate radio button
            val prefStorage = getPrefString(CommonToolsKotlin.PREF_DEFAULT_BACKUP_PATH, CommonToolsKotlin.DEFAULT_INTERNAL_STORAGE_DIR)
            if (prefStorage == CommonToolsKotlin.DEFAULT_INTERNAL_STORAGE_DIR){
                // case for internal storage
                radio_internal_storage.isChecked = true
                storagePointer = 0
            }
            else {
                // if an SD card was previously selected and the SD card is still available
                allSdCards.indexOf(prefStorage).let {
                    if (it == -1){
                        // previously selected SD card not available. Select internal storage.
                        radio_internal_storage.isChecked = true
                        storagePointer = 0
                    }
                    else {
                        // previously selected SD card is available. Select the corresponding radio button.
                        (it + 1).let {
                            radio_group_sd_cards.check(it)
                            storagePointer = it
                            radio_group_sd_cards.visibility = View.VISIBLE
                            storage_show_sd_cards.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    fun getView(): View = rootView

    fun getStoragePath(): String = paths[storagePointer]

    private fun getTraditionalSdCardPaths(): Array<String> {
        val possibleSDCards = arrayListOf<String>()
        val storage = FileX.new("/storage/", true)
        if (storage.exists() && storage.canRead()) {
            storage.listFiles { pathname ->
                (pathname.isDirectory && pathname.canRead()
                        && pathname.absolutePath != Environment.getExternalStorageDirectory().absolutePath)
            }?.let { files ->
                for (f in files) {
                    val sdDir = FileX.new("/mnt/media_rw/" + f.name, true)
                    if (sdDir.exists() && sdDir.isDirectory && sdDir.canWrite())
                        possibleSDCards.add(sdDir.canonicalPath)
                }
            }
        }
        return possibleSDCards.toTypedArray()
    }
}