/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.preference.PreferenceManager
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.robolectric.util.ReflectionHelpers
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class, RxSharedPreferences::class, FingerprintStore::class)
class SettingStoreTest : DisposingTest() {
    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    val sharedPreferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val rxSharedPreferences: RxSharedPreferences = Mockito.mock(RxSharedPreferences::class.java)

    @Mock
    val autofillManager: AutofillManager = Mockito.mock(AutofillManager::class.java)

    @Mock
    val fingerprintStore: FingerprintStore = PowerMockito.mock(FingerprintStore::class.java)

    private val autoLockSetting = PublishSubject.create<String>()
    private val autoLockStub = object : Preference<String> {
        override fun asObservable(): Observable<String> {
            return autoLockSetting
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in String> { TODO("not implemented") }
        override fun defaultValue(): String { TODO("not implemented") }
        override fun get(): String { TODO("not implemented") }
        override fun set(value: String) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val deviceWasSecure = PublishSubject.create<Boolean>()
    private val deviceSecureStub = object : Preference<Boolean> {
        override fun asObservable(): Observable<Boolean> {
            return deviceWasSecure
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in Boolean> { TODO("not implemented") }
        override fun defaultValue(): Boolean { TODO("not implemented") }
        override fun get(): Boolean { TODO("not implemented") }
        override fun set(value: Boolean) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val unlockWithFingerprintPendingAuthSetting = PublishSubject.create<Boolean>()
    private val unlockWithFingerprintPendingAuthStub = object : Preference<Boolean> {
        override fun asObservable(): Observable<Boolean> {
            return unlockWithFingerprintPendingAuthSetting
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in Boolean> { TODO("not implemented") }
        override fun defaultValue(): Boolean { TODO("not implemented") }
        override fun get(): Boolean { TODO("not implemented") }
        override fun set(value: Boolean) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val unlockWithFingerprintSetting = PublishSubject.create<Boolean>()
    private val unlockWithFingerprintStub = object : Preference<Boolean> {
        override fun asObservable(): Observable<Boolean> {
            return unlockWithFingerprintSetting
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in Boolean> { TODO("not implemented") }
        override fun defaultValue(): Boolean { TODO("not implemented") }
        override fun get(): Boolean { TODO("not implemented") }
        override fun set(value: Boolean) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val sendUsageDataSetting = PublishSubject.create<Boolean>()
    private val sendUsageDataStub = object : Preference<Boolean> {
        override fun asObservable(): Observable<Boolean> {
            return sendUsageDataSetting
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in Boolean> { TODO("not implemented") }
        override fun defaultValue(): Boolean { TODO("not implemented") }
        override fun get(): Boolean { TODO("not implemented") }
        override fun set(value: Boolean) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val itemListSortSetting = PublishSubject.create<String>()
    private val itemListSortStub = object : Preference<String> {
        override fun asObservable(): Observable<String> {
            return itemListSortSetting
        }

        override fun isSet(): Boolean { TODO("not implemented") }
        override fun key(): String { TODO("not implemented") }
        override fun asConsumer(): Consumer<in String> { TODO("not implemented") }
        override fun defaultValue(): String { TODO("not implemented") }
        override fun get(): String { TODO("not implemented") }
        override fun set(value: String) { TODO("not implemented") }
        override fun delete() { TODO("not implemented") }
    }

    private val dispatcher = Dispatcher()
    var subject = SettingStore(dispatcher, fingerprintStore)
    private val sendUsageDataObserver = TestObserver<Boolean>()
    private val itemListSortOrder = TestObserver<Setting.ItemListSort>()
    private val unlockWithFingerprint = TestObserver<Boolean>()
    private val autoLockTime = TestObserver<Setting.AutoLockTime>()

    private val autofillAvailable = true
    private val isCurrentAutofillProvider = false

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 28)

        whenCalled(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)
        whenCalled(editor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(editor)
        whenCalled(sharedPreferences.edit()).thenReturn(editor)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(sharedPreferences)

        whenCalled(rxSharedPreferences.getString(eq(SettingStore.Keys.AUTO_LOCK_TIME), anyString())).thenReturn(autoLockStub)
        whenCalled(rxSharedPreferences.getBoolean(eq(SettingStore.Keys.DEVICE_SECURITY_PRESENT), anyBoolean())).thenReturn(deviceSecureStub)
        whenCalled(rxSharedPreferences.getBoolean(eq(SettingStore.Keys.SEND_USAGE_DATA), anyBoolean())).thenReturn(sendUsageDataStub)
        whenCalled(rxSharedPreferences.getBoolean(eq(SettingStore.Keys.UNLOCK_WITH_FINGERPRINT), anyBoolean())).thenReturn(unlockWithFingerprintStub)
        whenCalled(rxSharedPreferences.getString(eq(SettingStore.Keys.ITEM_LIST_SORT_ORDER), anyString())).thenReturn(itemListSortStub)
        whenCalled(rxSharedPreferences.getBoolean(SettingStore.Keys.UNLOCK_WITH_FINGERPRINT_PENDING_AUTH)).thenReturn(unlockWithFingerprintPendingAuthStub)

        PowerMockito.mockStatic(RxSharedPreferences::class.java)
        whenCalled(RxSharedPreferences.create(sharedPreferences)).thenReturn(rxSharedPreferences)

        whenCalled(autofillManager.isAutofillSupported).thenReturn(autofillAvailable)
        whenCalled(autofillManager.hasEnabledAutofillServices()).thenReturn(isCurrentAutofillProvider)
        whenCalled(context.getSystemService(AutofillManager::class.java)).thenReturn(autofillManager)

        subject.injectContext(context)
        subject.sendUsageData.subscribe(sendUsageDataObserver)
        subject.unlockWithFingerprint.subscribe(unlockWithFingerprint)
        subject.itemListSortOrder.subscribe(itemListSortOrder)

        clearInvocations(editor)
    }

    @Test
    fun sendUsageDataTest_defaultValue() {
        val defaultValue = Constant.SettingDefault.sendUsageData
        verify(rxSharedPreferences).getBoolean(SettingStore.Keys.SEND_USAGE_DATA, defaultValue)

        sendUsageDataSetting.onNext(defaultValue)

        sendUsageDataObserver.assertValue(defaultValue)
    }

    @Test
    fun sendUsageDataTest_newValue() {
        val newValue = false
        val defaultValue = Constant.SettingDefault.sendUsageData

        verify(rxSharedPreferences).getBoolean(SettingStore.Keys.SEND_USAGE_DATA, defaultValue)

        sendUsageDataSetting.onNext(defaultValue)

        sendUsageDataObserver.assertValue(defaultValue)

        val action = SettingAction.SendUsageData(newValue)
        dispatcher.dispatch(action)

        verify(editor).putBoolean(Mockito.anyString(), Mockito.anyBoolean())
        verify(editor).apply()
    }

    @Test
    fun unlockWithFingerprint_newValue() {
        val newValue = true
        val defaultValue = Constant.SettingDefault.unlockWithFingerprint

        verify(rxSharedPreferences).getBoolean(SettingStore.Keys.UNLOCK_WITH_FINGERPRINT, defaultValue)

        unlockWithFingerprint.onNext(defaultValue)

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
        verify(rxSharedPreferences).getString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, defaultValue.name)

        itemListSortSetting.onNext(defaultValue.name)

        itemListSortOrder.assertValue(defaultValue)
    }

    @Test
    fun itemListSortOrder_newValue() {
        val newValue = Setting.ItemListSort.RECENTLY_USED
        val defaultValue = Constant.SettingDefault.itemListSort
        verify(rxSharedPreferences).getString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, defaultValue.name)

        itemListSortSetting.onNext(defaultValue.name)

        itemListSortOrder.assertValue(defaultValue)

        val action = SettingAction.ItemListSortOrder(newValue)
        dispatcher.dispatch(action)

        verify(editor).putString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, newValue.name)
        verify(editor).apply()
    }

    @Test
    fun `default value for autolock time when device is secure`() {
        clearInvocations(rxSharedPreferences)
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)
        subject.injectContext(context)

        subject.autoLockTime.subscribe(autoLockTime)

        val defaultValue = Constant.SettingDefault.autoLockTime
        verify(rxSharedPreferences).getString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(rxSharedPreferences).getString(SettingStore.Keys.AUTO_LOCK_TIME, defaultValue.name)
        verify(rxSharedPreferences).getBoolean(SettingStore.Keys.DEVICE_SECURITY_PRESENT, fingerprintStore.isDeviceSecure)

        autoLockSetting.onNext(defaultValue.name)
        deviceWasSecure.onNext(true)

        autoLockTime.assertLastValue(defaultValue)
    }

    @Test
    fun `getting new values for autolock time when device is secure`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)

        subject.autoLockTime.subscribe(autoLockTime)
        val newValue = Setting.AutoLockTime.OneHour
        val defaultValue = Setting.AutoLockTime.FiveMinutes

        autoLockSetting.onNext(defaultValue.name)
        deviceWasSecure.onNext(true)

        autoLockTime.assertValue(defaultValue)

        val action = SettingAction.AutoLockTime(newValue)
        dispatcher.dispatch(action)

        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, newValue.name)
        verify(editor).apply()
    }

    @Test
    fun `changing device from secure to insecure`() {
        subject.autoLockTime.subscribe(autoLockTime)
        val defaultValue = Constant.SettingDefault.autoLockTime
        val noSecurityAutoLockTime = Constant.SettingDefault.noSecurityAutoLockTime
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(false)

        autoLockSetting.onNext(defaultValue.name)
        deviceWasSecure.onNext(true)

        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, noSecurityAutoLockTime.name)
        verify(editor).putBoolean(SettingStore.Keys.DEVICE_SECURITY_PRESENT, false)
        verify(editor).apply()

        autoLockSetting.onNext(noSecurityAutoLockTime.name)
        deviceWasSecure.onNext(false)

        autoLockTime.assertValue(noSecurityAutoLockTime)
    }

    @Test
    fun `default value for autolock time when device is not secure`() {
        clearInvocations(rxSharedPreferences)
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(false)
        val defaultValue = Constant.SettingDefault.noSecurityAutoLockTime

        subject.injectContext(context)

        verify(rxSharedPreferences).getString(SettingStore.Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
        verify(rxSharedPreferences).getString(SettingStore.Keys.AUTO_LOCK_TIME, defaultValue.name)
        verify(rxSharedPreferences).getBoolean(SettingStore.Keys.DEVICE_SECURITY_PRESENT, false)

        subject.autoLockTime.subscribe(autoLockTime)

        deviceWasSecure.onNext(false)
        autoLockSetting.onNext(defaultValue.name)

        autoLockTime.assertLastValue(defaultValue)
    }

    @Test
    fun `changing device from insecure to secure`() {
        subject.autoLockTime.subscribe(autoLockTime)
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)
        val defaultValue = Constant.SettingDefault.noSecurityAutoLockTime
        val defaultSecureValue = Constant.SettingDefault.autoLockTime

        deviceWasSecure.onNext(false)
        autoLockSetting.onNext(defaultValue.name)

        verify(editor).putString(SettingStore.Keys.AUTO_LOCK_TIME, defaultSecureValue.name)
        verify(editor).putBoolean(SettingStore.Keys.DEVICE_SECURITY_PRESENT, true)
        verify(editor).apply()

        autoLockSetting.onNext(defaultSecureValue.name)
        deviceWasSecure.onNext(true)

        autoLockTime.assertValue(defaultSecureValue)
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

        dispatcher.dispatch(FingerprintAuthAction.OnSuccess)
        dispatcher.dispatch(FingerprintAuthAction.OnError)
        dispatcher.dispatch(FingerprintAuthAction.OnCancel)

        fingerprintAuthObserver.assertValueSequence(
            listOf(
                FingerprintAuthAction.OnSuccess,
                FingerprintAuthAction.OnError,
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

        clearInvocations(editor)
    }
}