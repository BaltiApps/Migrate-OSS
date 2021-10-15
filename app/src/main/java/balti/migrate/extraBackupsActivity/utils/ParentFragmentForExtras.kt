package balti.migrate.extraBackupsActivity.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs

abstract class ParentFragmentForExtras(layoutId: Int): Fragment(), LifecycleObserver {

    /**
     * Method to reach views of activity, once activity is fully created.
     * Found from stackoverflow.com
     * https://stackoverflow.com/questions/61306719/onactivitycreated-is-deprecated-how-to-properly-use-lifecycleobserver (last answer)
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onCreated(){
        tryIt { delegateStartBackupButton = activity?.findViewById(R.id.startBackupButton) }
        tryIt { delegateBackupButtonWaiting = activity?.findViewById(R.id.backupButtonWaiting) }
    }

    private val rootView: LinearLayout by lazy { View.inflate(activity, layoutId, null) as LinearLayout }
    var mActivity: Activity? = null

    var delegateMainItem: LinearLayout? = null
    var delegateStatusText: TextView? = null
    var delegateProgressBar: ProgressBar? = null
    var delegateCheckbox: CheckBox? = null
    var delegateSalView: TextView? = null
    var delegateStartBackupButton: Button? = null
    var delegateBackupButtonWaiting: LinearLayout? = null

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView
        onCreateFragment()
        return rootView
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateView(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        delegateMainItem = rootView
        delegateStatusText = getViewOrNull(viewIdStatusText)
        delegateProgressBar = viewIdProgressBar?.let { getViewOrNull(it) }
        delegateCheckbox = getViewOrNull(viewIdCheckbox)
        delegateSalView = viewIdSalView?.let { getViewOrNull(it) }
        mActivity = context as Activity
        lifecycle.addObserver(this)
    }

    override fun onDetach() {
        delegateMainItem = null
        delegateStatusText = null
        delegateProgressBar = null
        delegateCheckbox = null
        delegateSalView = null
        delegateStartBackupButton = null
        delegateBackupButtonWaiting = null
        lifecycle.removeObserver(this)
        super.onDetach()
        mActivity = null
    }

    private fun <T: View> getViewOrNull(viewId: Int): T? {
        return try { rootView.findViewById(viewId) } catch (_: Exception) { null }
    }

    protected fun deselectExtra(dataContainer: ArrayList<*>?, viewsToHide: List<View?>? = null, viewsToShow: List<View?>? = null) {
        dataContainer?.clear()
        delegateMainItem?.isClickable = false
        viewsToHide?.forEach { it?.visibility = View.GONE }
        viewsToShow?.forEach { it?.visibility = View.VISIBLE }    //usually includes Stock Android recommended label
        tryIt { readTask?.cancel() }
    }

    fun showStockWarning(fPositive: () -> Unit, fNegative: () -> Unit) {
        mActivity?.let {
            if (SharedPrefs.getPrefBoolean(CommonToolsKotlin.PREF_SHOW_STOCK_WARNING, true)) {
                AlertDialog.Builder(it)
                        .setTitle(R.string.stock_android_title)
                        .setMessage(R.string.stock_android_desc)
                        .setPositiveButton(R.string.go_ahead) { _, _ ->
                            fPositive()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            fNegative()
                        }
                        .setNeutralButton(R.string.dont_show_stock_warning) { _, _ ->
                            SharedPrefs.putPrefBoolean(CommonToolsKotlin.PREF_SHOW_STOCK_WARNING, false, immediate = true)
                            fPositive()
                        }
                        .setCancelable(false)
                        .show()
            } else {
                fPositive()
            }
        }
    }

    fun checkCheckbox(isChecked: Boolean): Boolean = delegateCheckbox.run {
        return if (this == null) false
        else {
            this.isChecked = isChecked
            true
        }
    }

    /**
     * Callback function to be triggered after [readTask] has completed reading.
     * Used to notify the activity of the fragment if needed.
     */
    internal var onReaderCompleteActivityCallback: (() -> Unit)? = null

    /**
     * Setter function for [onReaderCompleteActivityCallback].
     */
    fun onReaderComplete(f: () -> Unit){
        onReaderCompleteActivityCallback = f
    }

    open fun isChecked(): Boolean? = try {
        delegateCheckbox?.isChecked
    } catch (e: Exception) { null }

    open fun onCreateFragment(){}
    abstract fun onCreateView(savedInstanceState: Bundle?)
    abstract val readTask: ParentReaderForExtras?
    abstract val viewIdStatusText: Int
    open val viewIdProgressBar: Int? = null
    abstract val viewIdCheckbox: Int
    open val viewIdSalView: Int? = null
}