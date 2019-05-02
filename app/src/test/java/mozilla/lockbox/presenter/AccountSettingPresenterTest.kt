/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.concept.sync.Avatar
import mozilla.components.concept.sync.Profile
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(SecurePreferences::class)
class AccountSettingPresenterTest {
    class FakeAccountSettingView : AccountSettingView {
        var setDisplayNameArgument: String? = null
        var setAvatarArgument: Avatar? = null

        override fun setDisplayName(text: String) {
            setDisplayNameArgument = text
        }

        override fun setAvatar(avatar: Avatar) {
            setAvatarArgument = avatar
        }

        override val disconnectButtonClicks: Observable<Unit> = PublishSubject.create<Unit>()
    }

    @Mock
    val accountStore = PowerMockito.mock(AccountStore::class.java)

    private val view = FakeAccountSettingView()

    lateinit var subject: AccountSettingPresenter

    private val profileStub = PublishSubject.create<Optional<Profile>>()
    private val dispatcherObserver = TestObserver.create<Action>()

    @Before
    fun setUp() {
        whenCalled(accountStore.profile).thenReturn(profileStub)
        PowerMockito.whenNew(AccountStore::class.java).withAnyArguments().thenReturn(accountStore)
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject = AccountSettingPresenter(view, Dispatcher.shared, accountStore)
        subject.onViewReady()
    }

    @Test
    fun `new profile object with a displayName and email`() {
        val displayName = "TROGDOR"
        val email = "sample@sample.com"
        val avatar = Avatar(url = "www.mozilla.org/pix.png", isDefault = true)
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertEquals(displayName, view.setDisplayNameArgument)
    }

    @Test
    fun `new profile object with a displayName and null email`() {
        val displayName = "TROGDOR"
        val email = null
        val avatar = Avatar(url = "www.mozilla.org/pix.png", isDefault = true)
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertEquals(displayName, view.setDisplayNameArgument)
    }

    @Test
    fun `new profile object with a null displayName and non-null email`() {
        val displayName = null
        val email = "sample@sample.com"
        val avatar = Avatar(url = "www.mozilla.org/pix.png", isDefault = true)
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertEquals(email, view.setDisplayNameArgument)
    }

    @Test
    fun `new profile object with a null displayName and null email`() {
        val displayName = null
        val email = null
        val avatar = Avatar(url = "www.mozilla.org/pix.png", isDefault = true)
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertNull(view.setDisplayNameArgument)
    }

    @Test
    fun `new profile object with an avatar value`() {
        val displayName = "TROGDOR"
        val email = "sample@sample.com"
        val avatar = Avatar(url = "www.mozilla.org/pix.png", isDefault = true)
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertEquals(avatar, view.setAvatarArgument)
    }

    @Test
    fun `new profile object without an avatar value`() {
        val displayName = "TROGDOR"
        val email = "sample@sample.com"
        val avatar = null
        profileStub.onNext(
            Profile("lkjkhjlfdshkjljkafds", email, avatar, displayName).asOptional()
        )

        Assert.assertNull(view.setAvatarArgument)
    }

    @Test
    fun `new, null profile object`() {
        profileStub.onNext(Optional(null))

        Assert.assertNull(view.setDisplayNameArgument)
        Assert.assertNull(view.setAvatarArgument)
    }

    @Test
    fun disconnectButtonClicks() {
        (view.disconnectButtonClicks as Subject).onNext(Unit)

        dispatcherObserver.assertValue(DialogAction.UnlinkDisclaimer)
    }
}
