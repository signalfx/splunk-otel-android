package com.splunk.sdk.common.utils

import android.view.View
import androidx.fragment.app.Fragment
import com.splunk.sdk.common.utils.extensions.get
import com.splunk.sdk.common.utils.extensions.getFragmentSpecialEffectsControllerViewTag
import com.splunk.sdk.common.utils.extensions.invoke

// MARK Required Proguard rules

object FragmentTransactionObserver {

    enum class Event {
        START, END
    }

    fun observe(fragment: Fragment, callback: (Event, Fragment) -> Unit) {
        val parent = fragment.view?.parent as? View ?: return
        val tagId = parent.context.getFragmentSpecialEffectsControllerViewTag() ?: return
        val controller = parent.getTag(tagId)

        try {
            if (controller != null) {
                val pendingOperations = controller.get<ArrayList<Any>>("mPendingOperations") ?: return

                for (operation in pendingOperations) {
                    val currentFragment = operation.get<Fragment>("mFragment") ?: continue
                    val listener = Runnable { callback(Event.END, currentFragment) }

                    callback(Event.START, currentFragment)
                    operation.invoke<Unit>("addCompletionListener", listener to Runnable::class)
                }
            }
        } catch (e: NoSuchFieldException) {
            // TODO report
        }
    }
}
