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
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Before
import org.junit.Test

internal class AuthPresenterTest : DisposingTest() {
    class FakeLockedStore : LockedStore() {
        internal val _onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = _onAuth
    }

    class FakeAuthView : AuthView {
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

    private val lockedStore = FakeLockedStore()
    private val view = FakeAuthView()
    private val responseBuilder = FillResponseBuilder(ParsedStructure(packageName = "mozilla.lockbox.testing"))

    private lateinit var presenter: AuthPresenter

    @Before
    fun setUp() {
        presenter = AuthPresenter(view, responseBuilder, lockedStore = lockedStore)
        presenter.onViewReady()
    }

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