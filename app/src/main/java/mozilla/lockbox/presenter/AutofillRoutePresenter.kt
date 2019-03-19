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
) : RoutePresenter(activity) {

    private lateinit var navController: NavController

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
            is RouteAction.LockScreen -> navigateTo(R.id.fragment_locked)
            is RouteAction.ItemList -> showDialogFragment(AutofillFilterFragment(),
                RouteAction.DialogFragment.AutofillSearchDialog
            )
            is RouteAction.DialogFragment.FingerprintDialog ->
                showDialogFragment(FingerprintAuthDialogFragment(), action)
        }
    }

    private fun navigateTo(@IdRes destinationId: Int, args: Bundle? = null) {
        super.navigateToFragment(navController, destinationId, args)
    }

    // could possibly put this in RoutePresenter? slight differences between these versions
    private fun showDialogFragment(dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        val fragmentManager = activity.supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
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