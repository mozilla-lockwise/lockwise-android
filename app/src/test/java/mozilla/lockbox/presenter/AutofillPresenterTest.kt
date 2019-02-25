/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.service.autofill.FillResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.ParsedStructure
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.extensions.assertLastValueMatches
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.asOptional
import org.junit.Before
import org.junit.Test

internal class AutofillPresenterTest : DisposingTest() {
    class FakeLockedStore(dispatcher: Dispatcher) : LockedStore(dispatcher) {
        internal val _onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = _onAuth
    }

    class FakeAutofillView : AutofillView {
        override fun showAuthDialog() {
            TODO("not implemented")
        }

        override fun unlockFallback() {
            TODO("not implemented")
        }

        internal val _onFinish = PublishSubject.create<Optional<FillResponse>>()
        override fun setFillResponseAndFinish(fillResponse: FillResponse?) {
            _onFinish.onNext(fillResponse.asOptional())
        }

        internal val _onUnlockConfirm = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean>
            get() = _onUnlockConfirm

        override val context: Context
            get() = TODO("not implemented")
    }

    private val dispatcher = Dispatcher()
    private val dataStore = DataStore(dispatcher)
    private val lockedStore = FakeLockedStore(dispatcher)
    private val fingerprintStore = FingerprintStore(dispatcher)
    private val settingStore = SettingStore(dispatcher)
    private val pslSupport = PublicSuffixSupport()
    private val view = FakeAutofillView()
    private val responseBuilder = FillResponseBuilder(ParsedStructure(packageName = "mozilla.lockbox.testing"))

    private lateinit var presenter: AutofillPresenter

    @Before
    fun setUp() {
        presenter = AutofillPresenter(
            view,
            responseBuilder,
            dispatcher = dispatcher,
            fingerprintStore = fingerprintStore,
            settingStore = settingStore,
            lockedStore = lockedStore,
            dataStore = dataStore,
            pslSupport = pslSupport
        )
        presenter.onViewReady()
    }

    // TODO: Test the other paths

    @Test
    fun `tells view to finish with null on unlockConfirmed(false)`() {
        val obsFinish = createTestObserver<Optional<FillResponse>>()
        view._onFinish.subscribe(obsFinish)

        view._onUnlockConfirm.onNext(false)
        obsFinish.assertLastValueMatches { it.value == null }
    }

    @Test
    fun `tells view to finish with null on fingerprint auth canceled`() {
        val obsFinish = createTestObserver<Optional<FillResponse>>()
        view._onFinish.subscribe(obsFinish)

        lockedStore._onAuth.onNext(FingerprintAuthAction.OnCancel)
        obsFinish.assertLastValueMatches { it .value == null }
    }
}