package mozilla.lockbox.presenter

import android.content.Context
import android.service.autofill.FillResponse
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.PublicSuffixSupport

interface AuthView {
    fun showAuthDialog()
    fun unlockFallback()
    fun setFillResponseAndFinish(fillResponse: FillResponse?)
    val unlockConfirmed: Observable<Boolean>
    val context: Context
}

@ExperimentalCoroutinesApi
class AuthPresenter(
    private val view: AuthView,
    private val responseBuilder: FillResponseBuilder,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared,
    private val pslSupport: PublicSuffixSupport = PublicSuffixSupport.shared
) : Presenter() {

    override fun onViewReady() {
        Observables.combineLatest(settingStore.unlockWithFingerprint, dataStore.state)
            .filter { pair -> pair.second == DataStore.State.Locked }
            .subscribe { pair ->
                if (fingerprintStore.isFingerprintAuthAvailable && pair.first) {
                    view.showAuthDialog()
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)

        lockedStore.onAuthentication
            .subscribe {
                if (it is FingerprintAuthAction.OnAuthentication) {
                    when (it.authCallback) {
                        is FingerprintAuthCallback.OnAuth -> unlock()
                        is FingerprintAuthCallback.OnError -> view.unlockFallback()
                    }
                }
            }
            .addTo(compositeDisposable)

        view.unlockConfirmed
            .filter { it }
            .subscribe {
                unlock()
            }
            .addTo(compositeDisposable)

        dataStore.state
            .filter { it == DataStore.State.Unlocked }
            .switchMap { responseBuilder.asyncFilter(pslSupport, dataStore.list) }
            .subscribe { passwords ->
                val response =
                    responseBuilder.buildFilteredFillResponse(view.context, passwords)
                    ?: responseBuilder.buildFallbackFillResponse(view.context)
                // This should send off to the searchable list
                // https://github.com/mozilla-lockbox/lockbox-android/issues/421

                view.setFillResponseAndFinish(response)
                dispatcher.dispatch(DataStoreAction.Lock)
            }
            .addTo(compositeDisposable)
    }

    private fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            view.unlockFallback()
        } else {
            dispatcher.dispatch(DataStoreAction.Unlock)
        }
    }
}