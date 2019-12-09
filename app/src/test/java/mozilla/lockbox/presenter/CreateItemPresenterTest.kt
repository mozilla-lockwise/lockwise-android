/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class CreateItemPresenterTest {

    class FakeCreateView : CreateItemView {
        private val togglePasswordVisibilityStub = PublishSubject.create<Unit>()
        override val togglePasswordVisibility: Observable<Unit>
            get() = togglePasswordVisibilityStub

        private val passwordVisibleStub = false
        override var isPasswordVisible: Boolean = passwordVisibleStub

        override val togglePasswordClicks = PublishSubject.create<Unit>()

        val hostnameClicksStub = PublishSubject.create<String>()
        override val hostnameChanged: Observable<String>
            get() = hostnameClicksStub

        val usernameClicksStub = PublishSubject.create<String>()
        override val usernameChanged: Observable<String>
            get() = usernameClicksStub

        val passwordClicksStub = PublishSubject.create<String>()
        override val passwordChanged: Observable<String>
            get() = passwordClicksStub

        override fun closeKeyboard() {
            log.info("close keyboard")
        }

        val fakeCredential: ServerPassword by lazy {
            ServerPassword(
                "id0",
                "https://www.mozilla.org",
                "dogs@dogs.com",
                "woof",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L
            )
        }

        var itemDetailViewModelStub = ServerPassword(
            id = "",
            hostname = fakeCredential.hostname,
            username = fakeCredential.username,
            password = fakeCredential.password
        )

        override fun mapToServerPassword(): ServerPassword {
            return itemDetailViewModelStub
        }

        var item: ItemDetailViewModel? = null

        @StringRes
        var hostnameError: Int? = null
        @StringRes
        var usernameError: Int? = null
        @StringRes
        var passwordError: Int? = null

        override val closeEntryClicks = PublishSubject.create<Unit>()
        override val saveEntryClicks = PublishSubject.create<Unit>()

        override fun displayHostnameError(errorMessage: Int?) {
            hostnameError = errorMessage
        }

        override fun displayUsernameError(@StringRes errorMessage: Int?) {
            usernameError = errorMessage
        }

        override fun displayPasswordError(@StringRes errorMessage: Int?) {
            passwordError = errorMessage
        }

        var _saveEnabled = true
        override fun setSaveEnabled(enabled: Boolean) {
            _saveEnabled = enabled
        }
    }

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()!!
    val view: FakeCreateView = spy(FakeCreateView())

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!

    val itemDetailStore = PowerMockito.mock(ItemDetailStore::class.java)!!
    private val isPasswordVisibleStub = PublishSubject.create<Boolean>()

    lateinit var subject: CreateItemPresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        Mockito.`when`(itemDetailStore.isPasswordVisible).thenReturn(isPasswordVisibleStub)

        subject = CreateItemPresenter(view, dispatcher, itemDetailStore)
        subject.onViewReady()
    }

    @Test
    fun `tapping on save button`() {
        view.setSaveEnabled(true)
        view.saveEntryClicks.onNext(Unit)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.CreateItemSaveChanges(),
                RouteAction.ItemList
            )
        )
    }

    @Test
    fun `tapping on close button`() {
        view.closeEntryClicks.onNext(Unit)
        dispatcherObserver.assertValue(RouteAction.DiscardCreateItemNoChanges)
    }

    // TODO: https://github.com/mozilla-lockwise/lockwise-android/issues/822
    /* @Test
    fun `sends a list of duplicates to the view model`() {
        setUpTestSubject(fakeCredentialNoUsername)
        view.usernameClicksStub.onNext("")
        view.pwdClicksStub.onNext(fakeCredentialNoUsername.password)

        // now do the test.
        view.usernameClicksStub.onNext(fakeCredential.username ?: "")

        assertNotNull(view.usernameError)
    } */

    // TODO: https://github.com/mozilla-lockwise/lockwise-android/issues/823
    /* @Test
    fun `tapping on close button with no change`() {
        setUpTestSubject(fakeCredential)

        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditing(fakeCredential.id)
            )
        )
    }*/

    // TODO: https://github.com/mozilla-lockwise/lockwise-android/issues/823
    /* @Test
    fun `tapping on close button with change`() {
        setUpTestSubject(fakeCredential)

        view.usernameClicksStub.onNext("all-change")
        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                DialogAction.DiscardChangesCreateDialog
            )
        )
    }*/

    // TODO: https://github.com/mozilla-lockwise/lockwise-android/issues/823
    /* @Test
    fun `discard changes sends you back to the item list`() {
        setUpTestSubject(fakeCredential)
        isEditingStub.onNext(false)

        dispatcherObserver.assertValueSequence(
            listOf(
                RouteAction.ItemList
            )
        )
    }*/
}
