package balti.migrate.extraBackupsActivity.contacts

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ExtrasParentFragment
import balti.migrate.extraBackupsActivity.ExtrasParentReader
import balti.module.baltitoolbox.functions.Misc.runSuspendFunction
import kotlinx.android.synthetic.main.extra_fragment_contacts.*

class ContactsFragment: ExtrasParentFragment(R.layout.extra_fragment_contacts) {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override lateinit var readTask: ExtrasParentReader

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
    }

    private fun startReadTask(){
        readTask = ReadContactsKotlin(this)

        runSuspendFunction {
            val results = readTask.executeWithResult()
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {

        contacts_fragment_checkbox.setOnCheckedChangeListener { buttonView, isChecked ->

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

    override val viewIdStatusText: Int = R.id.contacts_read_text_status
    override val viewIdProgressBar: Int = R.id.contacts_read_progress
    override val viewIdCheckbox: Int = R.id.contacts_fragment_checkbox

    override fun isChecked(): Boolean? = try {
        contacts_fragment_checkbox?.isChecked
    } catch (e: Exception) { null }
}