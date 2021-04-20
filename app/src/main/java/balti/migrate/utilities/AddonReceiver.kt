package balti.migrate.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.utilities.constants.MDP_Constants
import balti.migrate.utilities.constants.MDP_Constants.Companion.ACTION_TO_MDP_ENGINE
import balti.migrate.utilities.constants.MDP_Constants.Companion.ACTION_TO_MDP_SETUP

class AddonReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.run {
            getStringExtra(MDP_Constants.EXTRA_OPERATION_TYPE)?.let { opType ->
                context?.let { c ->

                    LocalBroadcastManager.getInstance(c).sendBroadcast(
                            when (opType) {
                                MDP_Constants.EXTRA_OPERATION_BACKUP_PROGRESS -> Intent(ACTION_TO_MDP_ENGINE)
                                MDP_Constants.EXTRA_OPERATION_BACKUP_DONE -> Intent(ACTION_TO_MDP_ENGINE)
                                MDP_Constants.EXTRA_OPERATION_DUMMY_SU -> Intent(ACTION_TO_MDP_SETUP)
                                else -> Intent()
                            }.putExtras(this)
                    )
                }
            }
        }
    }
}