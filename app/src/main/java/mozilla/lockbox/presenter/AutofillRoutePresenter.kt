package mozilla.lockbox.presenter

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.AutofillStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment

@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.O)
class AutofillRoutePresenter(
    private val activity: AppCompatActivity,
    private val responseBuilder: FillResponseBuilder,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val autofillStore: AutofillStore = AutofillStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val pslSupport: PublicSuffixSupport = PublicSuffixSupport.shared
) : Presenter() {
    private lateinit var navController: NavController

    private val navHostFragmentManager: FragmentManager
        get() {
            val fragmentManager = activity.supportFragmentManager
            val navHost = fragmentManager.fragments.last()
            return navHost.childFragmentManager
        }

    private val currentFragment: Fragment
        get() {
            return navHostFragmentManager.fragments.last()
        }

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.autofill_fragment_nav_host)
        routeStore.routes
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)

        autofillStore.autofillActions
            .subscribe(this::finishAutofill)
            .addTo(compositeDisposable)

        dataStore.state
            .filter { it == DataStore.State.Unlocked }
            .switchMap {
                if (!IntentBuilder.isSearchRequired(activity.intent)) {
                    // need to account for mismatch between `Unlocked` and the list being "ready"
                    // see https://github.com/mozilla-lockbox/lockbox-android/issues/464
                    responseBuilder.asyncFilter(pslSupport, dataStore.list.take(1))
                } else {
                    // the user has pressed search, so we shouldn't filter the list here.
                    Observable.just(emptyList())
                }
            }
            .map { logins ->
                if (logins.isNotEmpty()) {
                    AutofillAction.CompleteMultiple(logins)
                } else {
                    RouteAction.ItemList
                }
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.LockScreen -> {
                dismissDialogIfPresent()
                navigateToFragment(R.id.fragment_locked)
            }
            is RouteAction.ItemList -> {
                dismissDialogIfPresent()
                navigateToFragment(R.id.fragment_filter_backdrop)
            }
            is RouteAction.DialogFragment.AutofillSearchDialog ->
                showDialogFragment(AutofillFilterFragment(), action)
            is RouteAction.DialogFragment.FingerprintDialog ->
                showDialogFragment(FingerprintAuthDialogFragment(), action)
        }
    }

    private fun navigateToFragment(@IdRes destinationId: Int, args: Bundle? = null) {
        val src = navController.currentDestination ?: return
        val srcId = src.id
        if (srcId == destinationId && args == null) {
            // No point in navigating if nothing has changed.
            return
        }

        val transition = findTransitionId(srcId, destinationId) ?: destinationId

        val navOptions = if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            val from = activity.resources.getResourceName(srcId)
            val to = activity.resources.getResourceName(destinationId)
            val graphName = activity.resources.getResourceName(navController.graph.id)
            log.error(
                "Cannot route from $from to $to. " +
                    "This is a developer bug, fixable by adding an action to $graphName.xml and/or ${javaClass.simpleName}"
            )
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
        } catch (e: IllegalArgumentException) {
            log.error("This appears to be a bug in navController", e)
            navController.navigate(destinationId, args)
        }
    }

    private fun findTransitionId(@IdRes src: Int, @IdRes destination: Int): Int? {
        return when (src to destination) {
            R.id.fragment_null to R.id.fragment_filter_backdrop -> R.id.action_to_filter
            R.id.fragment_null to R.id.fragment_locked -> R.id.action_to_locked
            R.id.fragment_locked to R.id.fragment_filter_backdrop -> R.id.action_locked_to_filter
            R.id.fragment_filter_backdrop to R.id.fragment_locked -> R.id.action_filter_to_locked
            else -> null
        }
    }

    private fun showDialogFragment(dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        try {
            dialogFragment.setTargetFragment(currentFragment, 0)
            dialogFragment.show(navHostFragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }

    private fun dismissDialogIfPresent() {
        (currentFragment as? DialogFragment)?.dismiss()
    }

    private fun finishAutofill(action: AutofillAction) {
        when (action) {
            is AutofillAction.Cancel -> setFillResponseAndFinish()
            is AutofillAction.Complete -> finishResponse(listOf(action.login))
            is AutofillAction.CompleteMultiple -> finishResponse(action.logins)
        }
    }

    private fun finishResponse(passwords: List<ServerPassword>) {
        val response = responseBuilder.buildFilteredFillResponse(activity, passwords)
        setFillResponseAndFinish(response)
    }

    private fun setFillResponseAndFinish(fillResponse: FillResponse? = null) {
        if (fillResponse == null) {
            activity.setResult(Activity.RESULT_CANCELED)
        } else {
            activity.setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse))
        }
        activity.finish()
    }
}