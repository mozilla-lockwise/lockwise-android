/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import io.reactivex.observers.TestObserver
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemListSort
import mozilla.lockbox.support.Constant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SettingStoreTest : DisposingTest() {
    @Mock
    val sharedPreferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    val dispatcher = Dispatcher()
    val subject = SettingStore(dispatcher)
    val sendUsageDataObserver = TestObserver<Boolean>()
    val itemListSortOrder = TestObserver<ItemListSort>()

    @Before
    fun setUp() {
        `when`(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)
        `when`(sharedPreferences.edit()).thenReturn(editor)

        subject.apply(sharedPreferences)
        subject.sendUsageData.subscribe(sendUsageDataObserver)
        subject.itemListSortOrder.subscribe(itemListSortOrder)
    }

    @Test
    fun sendUsageDataTest_defaultValue() {
        val defaultValue = Constant.Setting.defaultSendUsageData
        sendUsageDataObserver.assertValue(defaultValue)
    }

    @Test
    fun sendUsageDataTest_newValue() {
        val newValue = false
        val defaultValue = true

        sendUsageDataObserver.assertValue(defaultValue)

        val action = SettingAction.SendUsageData(newValue)
        dispatcher.dispatch(action)

        verify(editor).putBoolean(Mockito.anyString(), Mockito.anyBoolean())
        verify(editor).apply()
    }

    @Test
    fun itemListSortOrder_enumRoundtrip() {
        val start = ItemListSort.RECENTLY_USED
        val end = ItemListSort.valueOf(start.name)

        assertEquals(start, end)
    }

    @Test
    fun itemListSortOrder_defaultValue() {
        val defaultValue = Constant.Setting.defaultItemListSort
        itemListSortOrder.assertValue(defaultValue)
    }

    @Test
    fun itemListSortOrder_newValue() {
        val newValue = ItemListSort.RECENTLY_USED
        val defaultValue = ItemListSort.ALPHABETICALLY

        itemListSortOrder.assertValue(defaultValue)

        val action = SettingAction.ItemListSortOrder(newValue)
        dispatcher.dispatch(action)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, newValue.name)
        verify(editor).apply()
    }

    @Test
    fun `reset actions restore default values`() {
        dispatcher.dispatch(SettingAction.Reset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.Setting.defaultItemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.Setting.defaultSendUsageData)
        verify(editor).apply()
    }

    @Test
    fun `userreset lifecycle actions restore default values`() {
        dispatcher.dispatch(LifecycleAction.UserReset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.Setting.defaultItemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.Setting.defaultSendUsageData)
        verify(editor).apply()
    }
}