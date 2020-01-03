/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito

@ExperimentalCoroutinesApi
class AppFilterPresenterTest {
    class ExercisingFake(
        override val view: FilterView,
        override val dataStore: DataStore
    ) : AppFilterPresenter(view = view, dataStore = dataStore) {

        fun exerciseItemSelectionActionMap(original: Observable<ItemViewModel>): Observable<Action> {
            return original.itemSelectionActionMap()
        }

        fun exerciseItemListMap(original: Observable<Pair<CharSequence, List<ItemViewModel>>>): Observable<List<ItemViewModel>> {
            return original.itemListMap()
        }
    }

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)

    private val view = FilterPresenterTest.FakeFilterView()
    lateinit var subject: ExercisingFake
    private val id = "jnewkdiou"
    private val itemViewModel = ItemViewModel("mozilla", "cats@cats.com", id)

    @Before
    fun setUp() {
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)

        subject = ExercisingFake(view, dataStore)
    }

    @Test
    fun `item selection`() {
        val action = subject
            .exerciseItemSelectionActionMap(
                Observable.just(ItemViewModel("mozilla", "cats@cats.com", id))
            )
            .blockingIterable()
            .iterator()

        Assert.assertEquals(RouteAction.DisplayItem(id), action.next())
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

        Assert.assertEquals(list, mappedList.next())
    }
}