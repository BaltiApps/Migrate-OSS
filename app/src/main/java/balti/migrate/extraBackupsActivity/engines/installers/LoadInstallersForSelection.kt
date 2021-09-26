package balti.migrate.extraBackupsActivity.engines.installers

import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentSelectorActivityForExtras
import balti.migrate.extraBackupsActivity.engines.installers.containers.PackageVsInstaller
import balti.migrate.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_item_selector.*

class LoadInstallersForSelection: ParentSelectorActivityForExtras(R.layout.extra_item_selector) {

    private var copiedData = ArrayList<PackageVsInstaller>(0)
    private var adapter: InstallerListAdapter? = null

    override fun setup() {
        eis_ok.setOnClickListener(null)
        eis_cancel.setOnClickListener {
            sendResult(false)
        }
        eis_no_data.setText(R.string.no_apps)
        eis_title.setText(R.string.installer_selector_label)
        eis_select_all.visibility = View.GONE
        eis_store_for_all.visibility = View.VISIBLE

        eis_top_bar.visibility = View.GONE
        eis_button_bar.visibility = View.GONE
        eis_progressBar.visibility = View.VISIBLE
        eis_listView.visibility = View.INVISIBLE
        eis_no_data.visibility = View.GONE
    }

    override fun backgroundProcessing() {
        writeLog("Copying to temp list.")
        for (item in AppInstance.appInstallers)
            copiedData.add(PackageVsInstaller(item.key, item.value))

        writeLog("Creating adapter. Size: ${copiedData.size}")
        if (copiedData.size > 0){
            tryIt {
                adapter = InstallerListAdapter(this, copiedData)
            }
        }
        writeLog("Adapter created. Is null - ${adapter == null}")
    }

    override fun postProcessing() {
        if (copiedData.size > 0 && adapter != null) {
            writeLog("Showing list for selection.")

            tryIt { eis_listView.adapter = adapter }
            eis_top_bar.visibility = View.VISIBLE
            eis_button_bar.visibility = View.VISIBLE
            eis_progressBar.visibility = View.GONE
            eis_listView.visibility = View.VISIBLE

            eis_store_for_all.setOnClickListener {
                showMassInstallerSelector()
            }

            eis_clear_all.setOnClickListener {
                updateAllCopiedData("")
                adapter?.notifyDataSetChanged()
            }

        }
        else {
            writeLog("No data.")
            eis_no_data.visibility = View.VISIBLE
        }

        eis_ok.setOnClickListener {
            sendResult(true)
        }
    }

    private fun updateAllCopiedData(value: String) {
        for (item in copiedData) item.installerName = value
    }

    /**
     * This will show a dialog box to select a single installer for all apps in one go.
     * Useful if for example, the user wants to make the system believe
     * that all restored apps are from, say, Google Play store.
     */
    private fun showMassInstallerSelector(){

        val radioGroup = RadioGroup(this).apply {
            setPadding(20,20,20,20)
        }

        // Add all available installers as radio buttons
        var radioId = 100
        CommonToolsKotlin.QUALIFIED_PACKAGE_INSTALLERS.forEach {

            val installerPackageName = it.key
            val installerAppName = it.value

            radioGroup.addView(RadioButton(this).apply {
                text = installerAppName
                id = radioId++
                this.tag = installerPackageName
                            // store the package names via tag and id
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(20, 20, 20, 20)
            })

        }

        // Add a "Don't set" button
        radioGroup.addView(RadioButton(this).apply {
            setText(R.string.not_set)
            id = radioId++
            this.tag = ""
                                // store blank package name for this id
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(20, 20, 20, 20)
        })

        // Show the radio group in an AlertDialog
        AlertDialog.Builder(this)
            .setView(radioGroup)
            .setTitle(R.string.set_installer_for_all)
            .setPositiveButton(android.R.string.ok){_, _ ->

                val installerForAll: String =
                    radioGroup.run { findViewById<RadioButton>(checkedRadioButtonId).tag }.toString()

                // validate the found package name.
                if (installerForAll.isBlank()
                    || CommonToolsKotlin.QUALIFIED_PACKAGE_INSTALLERS.containsKey(installerForAll)) {

                    // update the copied data and update the adapter
                    updateAllCopiedData(installerForAll)
                    adapter?.notifyDataSetChanged()
                }


            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }

    override fun successBlock(){
        AppInstance.appInstallers.apply {
            clear()
            copiedData.forEach {
                this[it.packageName] = it.installerName
            }
        }
    }

    override val className = "LoadInstallersForSelection"
}