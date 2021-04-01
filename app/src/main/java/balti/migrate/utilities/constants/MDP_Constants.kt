package balti.migrate.utilities.constants

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import balti.migrate.AppInstance

class MDP_Constants {
    companion object{

        val DEBUG_TAG = "MDP_TAG"
        val PENDING_INTENT_ID = 7342
        val DONE_LABEL = "--- DONE! ---"

        val CHANNEL_NOTIFICATION = "MDP Service"
        val NOTIFICATION_ID = 11422

        val FILE_BACKUP_SCRIPT = "mdp_backup_script.sh"
        val FILE_RESTORE_SCRIPT = "mdp_restore_script.sh"                   // helper
        val FILE_BACKUP_PROGRESS_LOG = "mdp_backup.txt"
        val FILE_RESTORE_PROGRESS_LOG = "mdp_restore.txt"                   // helper

        val ACTION_MDP_PROGRESS = "action_mdp_progress"
        val ACTION_REQUEST_DATA_FROM_SERVICE = "action_get_data_from_service"

        val ERROR_SCRIPT_MAKE = "ERROR_SCRIPT_MAKE"
        val ERROR_SCRIPT_RUN = "MDP_RUN"
        val ERROR_SCRIPT_RUN_TRY_CATCH = "ERROR_MDP_RUN_CATCH"

        val EXTRA_ADDON_TYPE = "addon_type"                                 // helper
        val EXTRA_ADDON_TYPE_MDP = "type_mdp"                               // helper

        val EXTRA_WAS_CANCELLED = "mdp_was_cancelled"
        val EXTRA_ERRORS = "mdp_errors"
        val EXTRA_PROGRESS_LOG_URI = "mdp_progress_log_uri"

        val EXTRA_BACKUP_PACKAGES_LIST_FILE_LOCATION = "backup_packages_list_file_location"
        val EXTRA_BACKUP_LOCATION = "backup_location"
        val EXTRA_BUSYBOX_LOCATION = "busybox_location"
        val EXTRA_NUMBER_OF_APPS = "number_of_apps"
        val EXTRA_IGNORE_CACHE = "ignore_cache"

        val EXTRA_TITLE = "title"
        val EXTRA_PACKAGE_NAME = "package_name"
        val EXTRA_TASKLOG = "log"
        val EXTRA_PROGRESS_IN_100 = "progress"

        val EXTRA_SU_GRANTED = "mdp_su_granted"
        val EXTRA_SU_ERROR = "mdp_su_error"

        val EXTRA_OPERATION_TYPE = "operation_type"
        val EXTRA_OPERATION_DUMMY_SU = "operation_dummy_su"
        val EXTRA_OPERATION_STOP = "operation_stop"
        val EXTRA_OPERATION_BACKUP_START = "operation_backup_start"
        val EXTRA_OPERATION_BACKUP_DONE = "operation_backup_done"
        val EXTRA_OPERATION_BACKUP_PROGRESS = "operation_backup_progress"
        val EXTRA_OPERATION_RESTORE_START = "operation_restore_start"       // helper
        val EXTRA_OPERATION_RESTORE_DONE = "operation_restore_done"         // helper
        val EXTRA_OPERATION_RESTORE_PROGRESS = "operation_restore_progress"   // helper

        val EXTRA_WHO_IS_CALLING = "who_is_calling"

        val MASTER_PACKAGE_MIGRATE = "balti.migrate"
        val MASTER_PACKAGE_MIGRATE_INTENT_RECEIVER = "balti.migrate.RECEIVER"
        val MASTER_PACKAGE_MIGRATE_RECEIVER_CLASS = "balti.migrate.utilities.AddonReceiver"

        val MASTER_PACKAGE_HELPER = "balti.migrate.helper"                  // helper
        val MASTER_PACKAGE_HELPER_INTENT_RECEIVER = "balti.migrate.helper.RECEIVER" // helper
        val MASTER_PACKAGE_HELPER_RECEIVER_CLASS = "balti.migrate.helper.utilities.AddonReceiver"   // helper

        val TIMEOUT_WAITING_TO_KILL = 3000L

        // internal actions

        val ACTION_TO_MDP_ENGINE = "action_to_mdp_engine"
        val ACTION_TO_MDP_SETUP = "action_to_mdp_setup"
        val MDP_DELAY = 500L

        val MDP_PACKAGE_NAME = "balti.migrate.dataplugin"
        val MDP_RECEIVER_CLASS = "balti.migrate.dataplugin.DummyActivity"

        fun getMdpIntent(bundle: Bundle, operationType: String): Intent {
            return Intent().apply {
                component = ComponentName(MDP_PACKAGE_NAME, MDP_RECEIVER_CLASS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_WHO_IS_CALLING, AppInstance.appContext.packageName)
                putExtra(EXTRA_OPERATION_TYPE, operationType)
                putExtras(bundle)
            }
        }
    }
}