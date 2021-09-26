package balti.migrate.extraBackupsActivity.installer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.migrate.utilities.CommonToolsKotlin.Companion.FDROID_POSITION
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOT_SET_POSITION
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PLAY_STORE_POSITION
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.extra_item_selector.view.*

class LoadInstallersForSelection_legacy(private val jobCode: Int,
                                        private val context: Context,
                                        private val itemList: ArrayList<BackupDataPacketKotlin> = ArrayList(0)) : AsyncCoroutineTask(){



    private val selectorView by lazy { View.inflate(context, R.layout.extra_item_selector, null) }
    private val installerSelectorDialog by lazy { AlertDialog.Builder(context)
            .setView(selectorView)
            .setCancelable(false)
            .create()
    }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }
    private val copiedItemList by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    private var fdroidClientPresent = false
    private var fdroidExtensionPresent = false
    private var playStorePresent = false

    private lateinit var adapter: InstallerListAdapter

    init {
        selectorView.eis_ok.setOnClickListener(null)
        selectorView.eis_cancel.setOnClickListener {
            installerSelectorDialog.dismiss()
            onJobCompletion.onComplete(jobCode, false, itemList)
        }
        vOp.textSet(selectorView.eis_no_data, R.string.no_app_selected)
        vOp.textSet(selectorView.eis_title, R.string.installer_selector_label)
        vOp.visibilitySet(selectorView.eis_select_all, View.GONE)
        vOp.visibilitySet(selectorView.eis_store_for_all, View.VISIBLE)
    }

    override suspend fun onPreExecute() {
        super.onPreExecute()

        vOp.doSomething { installerSelectorDialog.show() }
        vOp.visibilitySet(selectorView.eis_top_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_button_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_progressBar, View.VISIBLE)
        vOp.visibilitySet(selectorView.eis_listView, View.INVISIBLE)
        vOp.visibilitySet(selectorView.eis_no_data, View.GONE)

        vOp.doSomething {
            context.packageManager.getInstalledPackages(0).forEach {
                if (it.packageName == "org.fdroid.fdroid") fdroidClientPresent = true
                else if (it.packageName == PACKAGE_NAME_FDROID) fdroidExtensionPresent = true
                else if (it.packageName == PACKAGE_NAME_PLAY_STORE) playStorePresent = true
            }
            if (fdroidClientPresent && !fdroidExtensionPresent)
                vOp.visibilitySet(selectorView.eis_fdroid_extension, View.VISIBLE)
            else vOp.visibilitySet(selectorView.eis_fdroid_extension, View.GONE)
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        for (i in itemList)
            copiedItemList.add(i.copy())

        if (copiedItemList.size > 0) adapter = InstallerListAdapter(context, copiedItemList)
        return null
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        if (copiedItemList.size > 0){
            vOp.doSomething { selectorView.eis_listView.adapter = adapter }
            vOp.visibilitySet(selectorView.eis_top_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_button_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_progressBar, View.GONE)
            vOp.visibilitySet(selectorView.eis_listView, View.VISIBLE)
        }
        else {
            vOp.visibilitySet(selectorView.eis_no_data, View.VISIBLE)
            vOp.doSomething { installerSelectorDialog.setCancelable(true) }
        }

        selectorView.eis_ok.setOnClickListener {
            onJobCompletion.onComplete(jobCode, true, adapter.installerCopiedAppList)
            installerSelectorDialog.dismiss()
        }

        selectorView.eis_store_for_all.setOnClickListener {
            vOp.doSomething {
                val radioGroup = RadioGroup(context).apply {
                    setPadding(20,20,20,20)
                }
                radioGroup.addView(RadioButton(context).apply {
                    setText(R.string.not_set)
                    id = NOT_SET_POSITION
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(20,20,20,20)
                })
                if (playStorePresent)
                    radioGroup.addView(RadioButton(context).apply {
                        setText(R.string.play_store)
                        id = PLAY_STORE_POSITION
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setPadding(20, 20, 20, 20)
                    })
                if (fdroidExtensionPresent)
                    radioGroup.addView(RadioButton(context).apply {
                        setText(R.string.f_droid)
                        id = FDROID_POSITION
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setPadding(20, 20, 20, 20)
                    })

                AlertDialog.Builder(context)
                        .setView(radioGroup)
                        .setTitle(R.string.set_installer_for_all)
                        .setPositiveButton(android.R.string.ok){_, _ ->
                            adapter.checkAll(radioGroup.checkedRadioButtonId)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
        }

        selectorView.eis_clear_all.setOnClickListener {
            adapter.checkAll(NOT_SET_POSITION)
        }
    }

}