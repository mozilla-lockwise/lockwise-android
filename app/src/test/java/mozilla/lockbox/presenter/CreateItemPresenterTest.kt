/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.ItemDetailStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class CreateItemPresenterTest {

    class FakeCreateView : CreateItemView {
        private val passwordVisibleStub = false
        override var isPasswordVisible: Boolean = passwordVisibleStub

        override val togglePasswordClicks = PublishSubject.create<Unit>()

        val hostnameChangedStub = BehaviorSubject.createDefault("")
        override val hostnameChanged: Observable<String>
            get() = hostnameChangedStub

        val usernameChangedStub = BehaviorSubject.createDefault("")
        override val usernameChanged: Observable<String>
            get() = usernameChangedStub

        val passwordChangedStub = BehaviorSubject.createDefault("")
        override val passwordChanged: Observable<String>
            get() = passwordChangedStub

        override fun closeKeyboard() {
            log.info("close keyboard")
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

        override fun displayHostnameError(@StringRes errorMessage: Int?) {
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

    private val isPasswordVisibleStub = BehaviorSubject.createDefault(false)
    private val unavailableUsernamesStub = BehaviorSubject.createDefault(emptySet<String>())
    private val isDirtyStub = BehaviorSubject.createDefault(false)
    private val isEditingStub = BehaviorSubject.createDefault(true)

    val itemDetailStore: ItemDetailStore by lazy {
        val store = PowerMockito.mock(ItemDetailStore::class.java)!!
        `when`(store.isPasswordVisible).thenReturn(isPasswordVisibleStub)
        `when`(store.unavailableUsernames).thenReturn(unavailableUsernamesStub)
        `when`(store.isDirty).thenReturn(isDirtyStub)
        `when`(store.isEditing).thenReturn(isEditingStub)
        store
    }

    lateinit var subject: CreateItemPresenter

    @Before
    fun setUp() {
        subject = CreateItemPresenter(view, dispatcher, itemDetailStore)
        subject.onViewReady()

        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun testViewReady() {
        val testObserver = TestObserver.create<Action>()
        dispatcher.register.subscribe(testObserver)
        val subject = CreateItemPresenter(view, dispatcher, itemDetailStore)
        subject.onViewReady()

        testObserver.assertValueSequence(
            listOf(
                ItemDetailAction.BeginCreateItemSession,
                ItemDetailAction.EditField(username = ""),
                ItemDetailAction.EditField(password = ""),
                ItemDetailAction.EditField(hostname = "")
            )
        )
    }

    @Test
    fun `tapping on save button when not dirty`() {
        isDirtyStub.onNext(false)
        view.saveEntryClicks.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndCreateItemSession,
                RouteAction.ItemList
            )
        )
    }

    @Test
    fun `tapping on save button when dirty`() {
        isDirtyStub.onNext(true)
        view.saveEntryClicks.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.CreateItemSaveChanges,
                RouteAction.ItemList
            )
        )
    }

    @Test
    fun `tapping on close button when not dirty`() {
        isDirtyStub.onNext(false)
        view.closeEntryClicks.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndCreateItemSession,
                RouteAction.ItemList
            )
        )
    }

    @Test
    fun `tapping on close button when dirty`() {
        isDirtyStub.onNext(true)
        view.closeEntryClicks.onNext(Unit)
        dispatcherObserver.assertValueSequence(
            listOf(
                DialogAction.DiscardChangesCreateDialog
            )
        )
    }

    @Test
    fun `a duplicated username causes an error`() {
        val username = "jane.doe"
        unavailableUsernamesStub.onNext(
            setOf(username)
        )

        view.usernameChangedStub.onNext(username)
        Assert.assertNotNull(view.usernameError)

        view.usernameChangedStub.onNext("jane.appleseed")
        Assert.assertNull(view.usernameError)
    }
}
