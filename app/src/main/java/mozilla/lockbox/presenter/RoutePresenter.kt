/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log

@ExperimentalCoroutinesApi
open class RoutePresenter(
    private val activity: AppCompatActivity
) : Presenter() {

    fun navigateToFragment(navController: NavController, @IdRes destinationId: Int, transition: Int,  args: Bundle? = null) {
        val src = navController.currentDestination ?: return
        val srcId = src.id

        if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            val from = activity.resources.getResourceName(srcId)
            val to = activity.resources.getResourceName(destinationId)
            log.error(
                "Cannot route from $from to $to. " +
                    "This is a developer bug, fixable by adding an action to graph_main.xml"
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

    fun getSourceId(navController: NavController, @IdRes destinationId: Int) : Int? {
        val src = navController.currentDestination ?: return null
        val srcId = src.id
        if (srcId == destinationId) {
            // No point in navigating if nothing has changed.
            return null
        }
        return destinationId
    }
}