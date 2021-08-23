package balti.migrate.extraBackupsActivity.contacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.module.baltitoolbox.functions.Misc.runOnMainThread
import balti.module.baltitoolbox.functions.Misc.runSuspendFunction
import balti.module.baltitoolbox.functions.Misc.showErrorDialog
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_fragment_contacts.*

class ContactsFragment: ParentFragmentForExtras(R.layout.extra_fragment_contacts) {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var selectorLauncher: ActivityResultLauncher<Intent>

    override lateinit var readTask: ParentReaderForExtras

    override fun onCreateFragment() {
        super.onCreateFragment()
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
               startReadTask()
            }
            else {
                Toast.makeText(mActivity, R.string.contacts_access_needed, Toast.LENGTH_SHORT).show()
                contacts_fragment_checkbox.isChecked = false
            }
        }
        selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                updateContacts()
            }
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {

        delegateCheckbox?.setOnCheckedChangeListener { buttonView, isChecked ->

            mActivity?.run {

                if (isChecked) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.not_recommended)
                        .setMessage(getText(R.string.contacts_not_recommended))
                        .setPositiveButton(R.string.dont_backup) { _, _ ->
                            buttonView.isChecked = false
                        }
                        .setNegativeButton(R.string.backup_contacts_anyway) { _, _ ->
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                        .setCancelable(false)
                        .show()
                }
                else deselectExtra(contactsList, listOf(contacts_read_progress, contacts_read_text_status))
            }
        }


    }

    private fun startReadTask(){
        readTask = ReadContactsKotlin(this)

        runSuspendFunction {
            val jobResults = readTask.executeWithResult() as ReaderJobResultHolder
            if (jobResults.success) {
                runOnMainThread {
                    tryIt({
                        updateContacts(jobResults.result as ArrayList<ContactsDataPacketKotlin>)
                    }, true)
                }
            } else {
                delegateCheckbox?.isChecked = false
                showErrorDialog(
                    jobResults.result.toString(),
                    getString(R.string.error_reading_contacts)
                )
            }
        }
    }

    private fun updateContacts(newList: ArrayList<ContactsDataPacketKotlin> = contactsList){

        contactsList.run {
            if (newList != this) {
                clear()
                addAll(newList)
            }
        }

        delegateStatusText?.text = getString(R.string.reading)
        delegateProgressBar?.visibility = View.GONE

        var n = 0
        newList.forEach {
            if (it.selected) n++
        }

        if (contactsList.size > 0){
            delegateStatusText?.text = "$n ${getString(R.string.of)} ${contactsList.size}"
            delegateMainItem?.setOnClickListener {
                //LoadContactsForSelectionKotlin(JOBCODE_LOAD_CONTACTS, this, contactsList).execute()
                selectorLauncher.launch(Intent(mActivity, LoadContactsForSelection::class.java))
            }
        }
        else {
            mActivity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.empty_contacts)
                    .setPositiveButton(R.string.close) {_, _ -> delegateCheckbox?.isChecked = false }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override val viewIdStatusText: Int = R.id.contacts_read_text_status
    override val viewIdProgressBar: Int = R.id.contacts_read_progress
    override val viewIdCheckbox: Int = R.id.contacts_fragment_checkbox

    override fun isChecked(): Boolean? = try {
        delegateCheckbox?.isChecked
    } catch (e: Exception) { null }
}