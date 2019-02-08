/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.view.autofill.AutofillManager
import io.reactivex.observers.TestObserver
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.support.Constant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.robolectric.util.ReflectionHelpers
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class)
class SettingStoreTest : DisposingTest() {
    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    val sharedPreferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val autofillManager: AutofillManager = Mockito.mock(AutofillManager::class.java)

    val dispatcher = Dispatcher()
    val subject = SettingStore(dispatcher)
    private val sendUsageDataObserver = TestObserver<Boolean>()
    private val itemListSortOrder = TestObserver<Setting.ItemListSort>()
    private val unlockWithFingerprint = TestObserver<Boolean>()
    private val autoLockTime = TestObserver<Setting.AutoLockTime>()

    val autofillAvailable = true
    val isCurrentAutofillProvider = false

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 28)

        whenCalled(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)
        whenCalled(sharedPreferences.edit()).thenReturn(editor)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(sharedPreferences)

        whenCalled(autofillManager.isAutofillSupported).thenReturn(autofillAvailable)
        whenCalled(autofillManager.hasEnabledAutofillServices()).thenReturn(isCurrentAutofillProvider)
        whenCalled(context.getSystemService(AutofillManager::class.java)).thenReturn(autofillManager)

        subject.injectContext(context)
        subject.sendUsageData.subscribe(sendUsageDataObserver)
        subject.unlockWithFingerprint.subscribe(unlockWithFingerprint)
        subject.itemListSortOrder.subscribe(itemListSortOrder)
        subject.autoLockTime.subscribe(autoLockTime)
    }

    @Test
    fun sendUsageDataTest_defaultValue() {
        val defaultValue = Constant.SettingDefault.sendUsageData
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
    fun unlockWithFingerprint_newValue() {
        val newValue = true
        val defaultValue = false

        unlockWithFingerprint.assertValue(defaultValue)

        val action = SettingAction.UnlockWithFingerprint(newValue)
        dispatcher.dispatch(action)

        Mockito.verify(editor).putBoolean(Mockito.anyString(), Mockito.anyBoolean())
        Mockito.verify(editor).apply()
    }

    @Test
    fun itemListSortOrder_enumRoundtrip() {
        val start = Setting.ItemListSort.RECENTLY_USED
        val end = Setting.ItemListSort.valueOf(start.name)

        assertEquals(start, end)
    }

    @Test
    fun itemListSortOrder_defaultValue() {
        val defaultValue = Constant.SettingDefault.itemListSort
        itemListSortOrder.assertValue(defaultValue)
    }

    @Test
    fun itemListSortOrder_newValue() {
        val newValue = Setting.ItemListSort.RECENTLY_USED
        val defaultValue = Setting.ItemListSort.ALPHABETICALLY

        itemListSortOrder.assertValue(defaultValue)

        val action = SettingAction.ItemListSortOrder(newValue)
        dispatcher.dispatch(action)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, newValue.name)
        verify(editor).apply()
    }

    @Test
    fun autoLockTime_defaultValue() {
        val defaultValue = Constant.SettingDefault.autoLockTime
        autoLockTime.assertValue(defaultValue)
    }

    @Test
    fun autoLockTime_newValue() {
        val newValue = Setting.AutoLockTime.OneHour
        val defaultValue = Setting.AutoLockTime.FiveMinutes

        autoLockTime.assertValue(defaultValue)

        val action = SettingAction.AutoLockTime(newValue)
        dispatcher.dispatch(action)

        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, newValue.name)
        verify(editor).apply()
    }

    @Test
    fun `reset actions when app is credential provider restore default values`() {
        setIsCurrentAutofillProvider(true)
        dispatcher.dispatch(SettingAction.Reset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
        verify(editor).apply()
        verify(autofillManager).disableAutofillServices()
    }

    @Test
    fun `userreset lifecycle actions when app is credential provider restore default values`() {
        setIsCurrentAutofillProvider(true)
        dispatcher.dispatch(LifecycleAction.UserReset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
        verify(editor).apply()
        verify(autofillManager).disableAutofillServices()
    }

    @Test
    fun `reset actions when app is not credential provider restore default values`() {
        setIsCurrentAutofillProvider(false)
        dispatcher.dispatch(SettingAction.Reset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
        verify(editor).apply()
        verify(autofillManager, never()).disableAutofillServices()
    }

    @Test
    fun `userreset lifecycle actions when app is not credential provider restore default values`() {
        setIsCurrentAutofillProvider(false)
        dispatcher.dispatch(LifecycleAction.UserReset)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(editor).putBoolean(SettingStore.Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
        verify(editor).apply()
        verify(autofillManager, never()).disableAutofillServices()
    }

    @Test
    fun test_FingerprintAuthAction() {
        val fingerprintAuthObserver = createTestObserver<FingerprintAuthAction>()
        subject.onEnablingFingerprint.subscribe(fingerprintAuthObserver)

        dispatcher.dispatch(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnAuth))
        dispatcher.dispatch(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnError))
        dispatcher.dispatch(FingerprintAuthAction.OnCancel)

        fingerprintAuthObserver.assertValueSequence(
            listOf(
                FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnAuth),
                FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnError),
                FingerprintAuthAction.OnCancel
            )
        )
    }

    @Test
    fun `autofill available uses system setting`() {
        assertEquals(autofillAvailable, subject.autofillAvailable)
    }

    @Test
    fun `is current autofill provider uses system setting`() {
        assertEquals(isCurrentAutofillProvider, subject.isCurrentAutofillProvider)
        setIsCurrentAutofillProvider(true)
        assertEquals(true, subject.isCurrentAutofillProvider)
    }

    @Test
    fun `disabling autofill setting when app is credential provider disables the system setting for the app`() {
        setIsCurrentAutofillProvider(true)
        dispatcher.dispatch(SettingAction.Autofill(false))
        verify(autofillManager).disableAutofillServices()
    }

    @Test
    fun `disabling autofill setting when app is not credential provider does nothing`() {
        setIsCurrentAutofillProvider(false)
        dispatcher.dispatch(SettingAction.Autofill(false))
        verify(autofillManager, never()).disableAutofillServices()
    }

    private fun setIsCurrentAutofillProvider(value: Boolean) {
        whenCalled(autofillManager.hasEnabledAutofillServices()).thenReturn(value)
        whenCalled(context.getSystemService(AutofillManager::class.java)).thenReturn(autofillManager)

        subject.injectContext(context)
    }
}