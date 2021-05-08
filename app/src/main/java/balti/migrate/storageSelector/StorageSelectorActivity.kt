package balti.migrate.storageSelector

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R

class StorageSelectorActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_selector)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)
    }

}