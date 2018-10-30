/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import junit.framework.Assert
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ItemListSort
import mozilla.lockbox.action.SettingsAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private val itemListSortOrderKey = "SortOption"

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class PreferencesStoreTest : DisposingTest() {

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: PublicPreferencesStore

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        dispatcher = Dispatcher()
        subject = PublicPreferencesStore(dispatcher)
        context = RuntimeEnvironment.application
        subject.applyContext(context)

        sharedPrefs = subject.sharedPrefs
    }

    @Test
    fun `item list sort has default`() {
        val testObserver = TestObserver.create<ItemListSort>()

        Assert.assertFalse(sharedPrefs.contains(itemListSortOrderKey))
        subject.itemListSortObservable.subscribe(testObserver)

        testObserver.assertLastValue(ItemListSort.ALPHABETICALLY)
        testObserver.addTo(disposer)
    }

    @Test
    fun `item list sort changes when shared prefs change`() {
        val testObserver = TestObserver.create<ItemListSort>()

        Assert.assertFalse(sharedPrefs.contains(itemListSortOrderKey))
        subject.itemListSortObservable.subscribe(testObserver)

        sharedPrefs.edit()
            .putInt(itemListSortOrderKey, ItemListSort.RECENTLY_USED.sortId)
            .apply()

        testObserver.assertLastValue(ItemListSort.RECENTLY_USED)
        testObserver.addTo(disposer)
    }

    @Test
    fun `item list sort changes from a SettingAction`() {
        val testObserver = TestObserver.create<ItemListSort>()

        Assert.assertFalse(sharedPrefs.contains(itemListSortOrderKey))
        subject.itemListSortObservable.subscribe(testObserver)

        dispatcher.dispatch(SettingsAction.SortAction(ItemListSort.RECENTLY_USED))

        testObserver.assertLastValue(ItemListSort.RECENTLY_USED)
        testObserver.addTo(disposer)
    }
}