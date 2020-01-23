package mozilla.lockbox.presenter

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
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
open class AutofillRoutePresenter(
    private val activity: AppCompatActivity,
    private val responseBuilder: FillResponseBuilder,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val autofillStore: AutofillStore = AutofillStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val pslSupport: PublicSuffixSupport = PublicSuffixSupport.shared
) : RoutePresenter(activity, dispatcher, routeStore) {

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
                    // see https://github.com/mozilla-lockwise/lockwise-android/issues/464
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

    override fun route(action: RouteAction) {
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
            is RouteAction.UnlockFallbackDialog ->
                showUnlockFallback(action)
        }
    }

    override fun findTransitionId(@IdRes src: Int, @IdRes dest: Int): Int? {
        return when (Pair(src, dest)) {
            R.id.fragment_null to R.id.fragment_filter_backdrop -> R.id.action_to_filter
            R.id.fragment_null to R.id.fragment_locked -> R.id.action_to_locked
            R.id.fragment_locked to R.id.fragment_filter_backdrop -> R.id.action_locked_to_filter
            R.id.fragment_filter_backdrop to R.id.fragment_locked -> R.id.action_filter_to_locked
            else -> null
        }
    }

    override fun showDialogFragment(dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        val fragmentManager = activity.supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
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
            is AutofillAction.Cancel -> cancelAndFinish()
            is AutofillAction.Complete -> finishResponse(listOf(action.login))
            is AutofillAction.CompleteMultiple -> finishResponse(action.logins)
        }
    }

    private fun finishResponse(passwords: List<ServerPassword>) {
        val response = responseBuilder.buildFilteredFillResponse(activity, passwords)

        // In Android O, if we just finish with a single dataset, then this counts as
        // a TYPE_DATASET_SELECTED event; we're counting all those as touch events.
        // However, following O, this was supposed to be a separate event, detect and register this
        // here too.
        if (passwords.size == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val id = passwords.first().id
            dispatcher.dispatch(DataStoreAction.AutofillTouch(id))
        }
        response?.let { setFillResponseAndFinish(it) } ?: cancelAndFinish()
    }

    private fun cancelAndFinish() {
        activity.setResult(Activity.RESULT_CANCELED)
        activity.finish()
    }

    private fun setFillResponseAndFinish(fillResponse: FillResponse) {
        val results = Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
        activity.setResult(Activity.RESULT_OK, results)
        activity.finish()
    }
}