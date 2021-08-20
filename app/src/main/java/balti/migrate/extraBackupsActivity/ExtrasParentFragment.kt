package balti.migrate.extraBackupsActivity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class ExtrasParentFragment(layoutId: Int): Fragment() {

    val rootView: View by lazy { View.inflate(activity, layoutId, null) }
    var mActivity: Activity? = null

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        onCreateFragment()
        return rootView
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateView(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as Activity
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    open fun onCreateFragment(){}
    abstract fun onCreateView(savedInstanceState: Bundle?)
    abstract fun isChecked(): Boolean?
}