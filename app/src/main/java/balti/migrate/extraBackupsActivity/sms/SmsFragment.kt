package balti.migrate.extraBackupsActivity.sms

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import kotlinx.android.synthetic.main.extra_fragment_sms.*

class SmsFragment: ParentFragmentForExtras(R.layout.extra_fragment_sms) {

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
                Toast.makeText(mActivity, R.string.calls_access_needed, Toast.LENGTH_SHORT).show()
                sms_fragment_checkbox.isChecked = false
            }
        }
        selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                updateCalls()
            }
        }
    }

}