/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.ToastNotificationAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.AlertDialogStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.RootActivity
import java.lang.Exception
import mozilla.lockbox.view.Fragment as SpecializedFragment

@ExperimentalCoroutinesApi
abstract class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher,
    private val routeStore: RouteStore,
    internal val alertDialogStore: AlertDialogStore = AlertDialogStore.shared
) : Presenter() {

    lateinit var navController: NavController

    open val navHostFragmentManager: FragmentManager
        get() {
            val fragmentManager = activity.supportFragmentManager
            val navHost = fragmentManager.fragments.last()
            return navHost.childFragmentManager
        }

    open val currentFragment: Fragment?
        get() {
            return navHostFragmentManager.fragments.lastOrNull()
        }

    class BackPressedCallback(
        val enabled: Boolean = false,
        val dispatcher: Dispatcher
    ) : OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
            dispatcher.dispatch(RouteAction.InternalBack)
        }
    }

    override fun onBackPressed(): Boolean {
        dispatcher.dispatch(RouteAction.InternalBack)
        val fragment = currentFragment as? SpecializedFragment
        return fragment?.onBackPressed() ?: false
    }

    private val onBackPressedDispatcher = activity.onBackPressedDispatcher

    private val callback = BackPressedCallback(false, dispatcher)

    override fun onViewReady() {
        super.onViewReady()
        onBackPressedDispatcher.addCallback(activity, callback)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        onBackPressedDispatcher.addCallback(activity, callback)
    }

    protected abstract fun route(action: RouteAction)

    protected abstract fun findTransitionId(@IdRes src: Int, @IdRes dest: Int): Int?

    fun showDialog(destination: DialogAction) {
        alertDialogStore.showDialog(activity, destination)
    }

    fun showToastNotification(action: ToastNotificationAction) {
        try {
            (activity as RootActivity).showToastNotification(action)
        } catch (e: Exception) {
            log.error("Could not show toast notification.")
        }
    }

    open fun showDialogFragment(dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        try {
            dialogFragment.setTargetFragment(currentFragment, 0)
            dialogFragment.show(navHostFragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }

    fun openWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(browserIntent, null)
    }

    fun openSetting(settingAction: RouteAction.SystemSetting) {
        val settingIntent = Intent(settingAction.setting.intentAction)
        settingIntent.data = settingAction.setting.data
        activity.startActivity(settingIntent, null)
    }

    fun navigateToFragment(@IdRes destinationId: Int, args: Bundle? = null) {
        val src = navController.currentDestination ?: return
        val srcId = src.id
        if (srcId == destinationId) {
            val currentScreenArgs = navHostFragmentManager.fragments.lastOrNull()?.arguments
            if (args hasSameContentOf currentScreenArgs) {
                // No point in navigating if nothing has changed.
                return
            }
        }

        val transition = findTransitionId(srcId, destinationId) ?: destinationId

        val navOptions = if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            if (BuildConfig.DEBUG) {
                val from = activity.resources.getResourceName(srcId)
                val to = activity.resources.getResourceName(destinationId)
                val graphName = activity.resources.getResourceName(navController.graph.id)
                throw IllegalStateException(
                    "Cannot route from $from to $to. " +
                        "This is a developer bug, fixable by adding an action to $graphName.xml and/or ${javaClass.simpleName}"
                )
            }
            null
        } else {
            // Get the transition action out of the graph, before we manually clear the back
            // stack, because it causes IllegalArgumentExceptions.
            src.getAction(transition)?.navOptions?.let { navOptions ->
                if (navOptions.shouldLaunchSingleTop()) {
                    while (navController.popBackStack()) {
                        // NOP
                    }
                    routeStore.clearBackStack()
                }
                navOptions
            }
        }

        try {
            navController.navigate(destinationId, args, navOptions)
        } catch (e: RuntimeException) {
            log.error(e.localizedMessage)
            navController.navigate(destinationId, args)
        }
    }

    fun showUnlockFallback(action: RouteAction.UnlockFallbackDialog) {
        val manager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val appName = activity.applicationContext.getString(R.string.app_name)

        val intent = manager.createConfirmDeviceCredentialIntent(
            activity.getString(R.string.unlock_fallback_title, appName),
            activity.getString(R.string.confirm_pattern)
        )
        try {
            currentFragment?.startActivityForResult(intent, action.requestCode)
        } catch (e: RuntimeException) {
            log.error("Unlock fallback failed: ", e)
        }
    }

    private infix fun Bundle?.hasSameContentOf(another: Bundle?): Boolean {
        if (this == null) {
            return another == null || another.isEmpty
        }

        if (size() != another?.size()) {
            return false
        }

        return keySet().all { key ->
            get(key) == another.get(key)
        }
    }
}