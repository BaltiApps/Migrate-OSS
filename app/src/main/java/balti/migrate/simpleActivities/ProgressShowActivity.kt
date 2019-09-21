package balti.migrate.simpleActivities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import balti.migrate.R
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_CANCEL
import kotlinx.android.synthetic.main.backup_progress_layout.*

class ProgressShowActivity: AppCompatActivity() {

    private val commonTools by lazy { CommonToolKotlin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.backup_progress_layout)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        progressActionButton.setOnClickListener {
            commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_CANCEL))
        }
    }

}