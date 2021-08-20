package balti.migrate.extraBackupsActivity.contacts

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ExtrasParentFragment
import balti.migrate.utilities.CommonToolsKotlin.Companion.CONTACT_PERMISSION
import kotlinx.android.synthetic.main.extra_fragment_contacts.view.*

class ContactsFragment: ExtrasParentFragment(R.layout.extra_fragment_contacts) {

    override fun onCreateView(savedInstanceState: Bundle?) {
        rootView.contacts_fragment_checkbox?.setOnCheckedChangeListener { buttonView, isChecked ->

            mActivity?.run {

                if (isChecked) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.not_recommended)
                        .setMessage(getText(R.string.contacts_not_recommended))
                        .setPositiveButton(R.string.dont_backup) { _, _ ->
                            buttonView.isChecked = false
                        }
                        .setNegativeButton(R.string.backup_contacts_anyway) { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.READ_CONTACTS), CONTACT_PERMISSION
                            )
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    override fun isChecked(): Boolean? = try {
        rootView.contacts_fragment_checkbox?.isChecked
    } catch (e: Exception) { null }
}