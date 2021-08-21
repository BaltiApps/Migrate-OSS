package balti.migrate.extraBackupsActivity

import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.AppInstance
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

abstract class ExtrasParentReader(private val fragment: ExtrasParentFragment): AsyncCoroutineTask() {

    val context by lazy { AppInstance.appContext }

    val mainItem: LinearLayout? get() = fragment.delegateMainItem
    val readStatusText: TextView? get() = fragment.delegateStatusText
    val readProgressBar: ProgressBar? get() = fragment.delegateProgressBar
    val doBackupCheckBox: CheckBox? get() = fragment.delegateCheckbox

}