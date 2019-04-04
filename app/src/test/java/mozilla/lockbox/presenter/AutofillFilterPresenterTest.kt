/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class AutofillFilterPresenterTest {
    class FakeDataStore : DataStore() {
        val getStub = PublishSubject.create<Optional<ServerPassword>>()
        var getArgument: String? = null

        override fun get(id: String): Observable<Optional<ServerPassword>> {
            getArgument = id
            return getStub
        }
    }

    class ExercisingFake(
        override val view: FilterView,
        override val dataStore: DataStore
    ) : AutofillFilterPresenter(view, dataStore = dataStore) {

        fun exerciseItemSelectionActionMap(original: Observable<ItemViewModel>): Observable<Action> {
            return original.itemSelectionActionMap()
        }

        fun exerciseItemListMap(original: Observable<Pair<CharSequence, List<ItemViewModel>>>): Observable<List<ItemViewModel>> {
            return original.itemListMap()
        }
    }

    private val view = FilterPresenterTest.FakeFilterView()
    private val dataStore = FakeDataStore()
    val subject = ExercisingFake(view, dataStore)
    private val id = "jnewkdiou"
    private val itemViewModel = ItemViewModel("mozilla", "cats@cats.com", id)

    @Test
    fun `item selection with null item`() {
        val action = subject
            .exerciseItemSelectionActionMap(
                Observable.just(ItemViewModel("mozilla", "cats@cats.com", id))
            )
            .blockingIterable()
            .iterator()

        Assert.assertEquals(id, dataStore.getArgument)
        dataStore.getStub.onNext(Optional(null))

        Assert.assertEquals(AutofillAction.Cancel, action.next())
    }

    @Test
    fun `item selection with non-null item`() {
        val action = subject
            .exerciseItemSelectionActionMap(
                Observable.just(itemViewModel)
            )
            .blockingIterable()
            .iterator()

        Assert.assertEquals(id, dataStore.getArgument)
        val serverPassword = ServerPassword(id, "www.mozilla.org", password = "dawgz")
        dataStore.getStub.onNext(serverPassword.asOptional())

        Assert.assertEquals(AutofillAction.Complete(serverPassword), action.next())
    }

    @Test
    fun `item list with filtering text`() {
        val list = listOf(itemViewModel)
        val mappedList = subject
            .exerciseItemListMap(
                Observable.just(Pair("m", list))
            )
            .blockingIterable()
            .iterator()

        Assert.assertEquals(list, mappedList.next())
    }

    @Test
    fun `item list without filtering text`() {
        val list = listOf(itemViewModel)
        val mappedList = subject
            .exerciseItemListMap(
                Observable.just(Pair("", list))
            )
            .blockingIterable()
            .iterator()

        Assert.assertEquals(emptyList<ItemViewModel>(), mappedList.next())
    }
}