/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.log
import mozilla.lockbox.view.DialogFragment

typealias TransitionIdFunction = ((from: Int, to: Int) -> Int?)

class RoutePresenterSupport {
    fun navigateToFragment(
        navController: NavController,
        activity: AppCompatActivity,
        @IdRes destinationId: Int,
        findTransitionId: TransitionIdFunction,
        args: Bundle? = null
    ) {
        val src = navController.currentDestination ?: return
        val srcId = src.id
        if (srcId == destinationId && args == null) {
            // No point in navigating if nothing has changed.
            return
        }

        val transition = findTransitionId(srcId, destinationId) ?: destinationId

        if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            val from = activity.resources.getResourceName(srcId)
            val to = activity.resources.getResourceName(destinationId)
            log.error(
                "Cannot route from $from to $to. " +
                    "This is a developer bug, fixable by adding an action to graph_autofill.xml"
            )
        } else {
            val clearBackStack = src.getAction(transition)?.navOptions?.shouldLaunchSingleTop() ?: false
            if (clearBackStack) {
                while (navController.popBackStack()) {
                    // NOP
                }
            }
        }

        try {
            navController.navigate(transition, args)
        } catch (e: IllegalArgumentException) {
            log.error("This appears to be a bug in navController", e)
            navController.navigate(destinationId, args)
        }
    }

    fun showDialogFragment(activity: AppCompatActivity, dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        val fragmentManager = activity.supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }
}