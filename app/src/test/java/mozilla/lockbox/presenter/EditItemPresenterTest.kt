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
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class EditItemPresenterTest {

    class FakeEditItemView : EditItemView {
        private val passwordVisibleStub = false
        override var isPasswordVisible: Boolean = passwordVisibleStub

        private val togglePwdClicksStub = PublishSubject.create<Unit>()
        override val togglePasswordClicks: Observable<Unit>
            get() = togglePwdClicksStub

        val hostnameChangedStub = BehaviorSubject.createDefault("")
        override val hostnameChanged: Observable<String>
            get() = hostnameChangedStub

        val usernameChangedStub = BehaviorSubject.createDefault("")
        override val usernameChanged: Observable<String>
            get() = usernameChangedStub

        val passwordChangedStub = BehaviorSubject.createDefault("")
        override val passwordChanged: Observable<String>
            get() = passwordChangedStub

        private val hostnameFocusStub = BehaviorSubject.createDefault(false)
        private val usernameFocusStub = BehaviorSubject.createDefault(false)
        private val passwordFocusStub = BehaviorSubject.createDefault(false)

        override val hostnameFocus: Observable<Boolean>
            get() = hostnameFocusStub
        override val usernameFocus: Observable<Boolean>
            get() = usernameFocusStub
        override val passwordFocus: Observable<Boolean>
            get() = passwordFocusStub

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

        var _saveEnabled = true
        val closeEntryClicksStub = PublishSubject.create<Unit>()

        override val closeEntryClicks: Observable<Unit>
            get() = closeEntryClicksStub
        val saveEntryClicksStub = PublishSubject.create<Unit>()

        override val saveEntryClicks: Observable<Unit>
            get() = saveEntryClicksStub

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }

        override fun displayUsernameError(@StringRes errorMessage: Int?) {
            usernameError = errorMessage
        }

        override fun displayHostnameError(errorMessage: Int?) {
            hostnameError = errorMessage
        }

        override fun displayPasswordError(@StringRes errorMessage: Int?) {
            passwordError = errorMessage
        }

        override fun setSaveEnabled(enabled: Boolean) {
            _saveEnabled = enabled
        }
    }

    private val isPasswordVisibleStub = BehaviorSubject.createDefault(false)
    private val unavailableUsernamesStub = BehaviorSubject.createDefault(emptySet<String>())
    private val isDirtyStub = BehaviorSubject.createDefault(false)
    private val isEditingStub = BehaviorSubject.createDefault(true)
    private val originalItemStub = PublishSubject.create<Optional<ServerPassword>>()

    val itemDetailStore: ItemDetailStore by lazy {
        val store = PowerMockito.mock(ItemDetailStore::class.java)!!
        Mockito.`when`(store.isPasswordVisible).thenReturn(isPasswordVisibleStub)
        Mockito.`when`(store.unavailableUsernames).thenReturn(unavailableUsernamesStub)
        Mockito.`when`(store.isDirty).thenReturn(isDirtyStub)
        Mockito.`when`(store.isEditing).thenReturn(isEditingStub)
        Mockito.`when`(store.originalItem).thenReturn(originalItemStub)
        store
    }

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()!!

    val view: FakeEditItemView = FakeEditItemView()

    private val theItemId = "dummy"
    private val theItem: ServerPassword by lazy {
        ServerPassword(
            theItemId,
            hostname = "https://www.mozilla.org",
            username = "dogs@dogs.com",
            password = "woof"
        )
    }

    lateinit var subject: EditItemPresenter

    @Before
    fun setUp() {
        subject = EditItemPresenter(view, theItemId, dispatcher, itemDetailStore)
        subject.onViewReady()
        isEditingStub.onNext(true)

        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun testViewReady() {
        val testObserver = TestObserver.create<Action>()
        dispatcher.register.subscribe(testObserver)
        val subject = EditItemPresenter(view, "requested-id", dispatcher, itemDetailStore)
        subject.onViewReady()

        testObserver.assertValueSequence(
            listOf(
                ItemDetailAction.BeginEditItemSession("requested-id"),
                ItemDetailAction.EditField(username = ""),
                ItemDetailAction.EditField(password = "")
            )
        )
    }

    @Test
    fun `sends a detail view model to view`() {
        originalItemStub.onNext(theItem.asOptional())

        // test the results that the view gets.
        val obs = view.item ?: return fail("Expected an item")
        assertEquals(theItem.hostname, obs.hostname)
        assertEquals(theItem.username, obs.username)
        assertEquals(theItem.password, obs.password)
        assertEquals(theItem.id, obs.id)
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

    @Test
    fun `tapping on close button with no change`() {
        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditItemSession
            )
        )
    }

    @Test
    fun `tapping on close button when not dirty`() {
        isDirtyStub.onNext(false)
        view.closeEntryClicksStub.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditItemSession,
                RouteAction.ItemList,
                RouteAction.DisplayItem(theItemId)
            )
        )
    }

    @Test
    fun `tapping on close button when dirty`() {
        isDirtyStub.onNext(true)
        view.closeEntryClicksStub.onNext(Unit)
        dispatcherObserver.assertValueSequence(
            listOf(
                DialogAction.DiscardChangesDialog(theItemId)
            )
        )
    }

    @Test
    fun `tapping on save button when not dirty`() {
        isDirtyStub.onNext(false)
        view.saveEntryClicksStub.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditItemSession,
                RouteAction.ItemList,
                RouteAction.DisplayItem(theItemId)
            )
        )
    }

    @Test
    fun `tapping on save button when dirty`() {
        isDirtyStub.onNext(true)
        view.saveEntryClicksStub.onNext(Unit)
        isEditingStub.onNext(false)
        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EditItemSaveChanges,
                RouteAction.ItemList,
                RouteAction.DisplayItem(theItemId)
            )
        )
    }

    @Test
    fun `stopping editing sends you back to the item detail`() {
        isEditingStub.onNext(false)

        dispatcherObserver.assertValueSequence(
            listOf(
                RouteAction.ItemList,
                RouteAction.DisplayItem(theItemId)
            )
        )
    }
}
