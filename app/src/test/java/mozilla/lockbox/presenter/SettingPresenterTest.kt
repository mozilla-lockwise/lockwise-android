/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.adapter.ListAdapterTestHelper
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(RobolectricTestRunner::class)
class SettingPresenterTest {
    class SettingViewFake : SettingView {
        var settingItem: List<SettingCellConfiguration>? = null
        var sectionsItem: List<SectionedAdapter.Section>? = null

        override fun updateSettingList(
            settings: List<SettingCellConfiguration>,
            sections: List<SectionedAdapter.Section>
        ) {
            settingItem = settings
            sectionsItem = sections
        }
    }

    private val unlockWithFingerprintStub = PublishSubject.create<Boolean>()
    private val sendUsageDataStub = PublishSubject.create<Boolean>()
    private val itemListSortOrderStub = PublishSubject.create<Setting.ItemListSort>()
    private val unlockWithFingerprintPendingAuthStub = PublishSubject.create<Boolean>()
    private val autoLockTimeStub = PublishSubject.create<Setting.AutoLockTime>()
    private val onEnablingFingerprintStub = PublishSubject.create<FingerprintAuthAction>()

    @Mock
    val settingStore = PowerMockito.mock(SettingStore::class.java)

    private lateinit var testHelper: ListAdapterTestHelper
    private val view = SettingViewFake()
    private val fingerprintStore = mock(FingerprintStore::class.java)
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    lateinit var subject: SettingPresenter

    @Before
    fun setUp() {
        whenCalled(settingStore.autoLockTime).thenReturn(autoLockTimeStub)
        whenCalled(settingStore.unlockWithFingerprint).thenReturn(unlockWithFingerprintStub)
        whenCalled(settingStore.unlockWithFingerprintPendingAuth).thenReturn(unlockWithFingerprintPendingAuthStub)
        whenCalled(settingStore.sendUsageData).thenReturn(sendUsageDataStub)
        whenCalled(settingStore.itemListSortOrder).thenReturn(itemListSortOrderStub)
        whenCalled(settingStore.onEnablingFingerprint).thenReturn(onEnablingFingerprintStub)

        PowerMockito.whenNew(SettingStore::class.java).withAnyArguments().thenReturn(settingStore)

        dispatcher.register.subscribe(dispatcherObserver)
        testHelper = ListAdapterTestHelper()
        subject = SettingPresenter(
            view,
            dispatcher,
            settingStore,
            fingerprintStore
        )

        subject.onViewReady()
    }

    @Test
    fun `update settings when fingerprint available and autofill unavailable`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        whenCalled(settingStore.autofillAvailable).thenReturn(false)
        val expectedSettings = testHelper.createAccurateListOfSettings(true, false)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(2, R.string.support_title)
        )

        subject.onResume()

        assertEquals(view.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(view.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(view.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(view.settingItem!![3].title, expectedSettings[3].title)

        assertEquals(view.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(view.sectionsItem!![1].title, expectedSections[1].title)

        assertEquals(view.sectionsItem!![0].firstPosition, expectedSections[0].firstPosition)
        assertEquals(view.sectionsItem!![1].firstPosition, expectedSections[1].firstPosition)
    }

    @Test
    fun `update settings when fingerprint unavailable and autofill unavailable`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        whenCalled(settingStore.autofillAvailable).thenReturn(false)
        val expectedSettings = testHelper.createAccurateListOfSettings(false, false)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(1, R.string.support_title)
        )

        subject.onResume()

        assertEquals(view.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(view.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(view.settingItem!![2].title, expectedSettings[2].title)

        assertEquals(view.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(view.sectionsItem!![1].title, expectedSections[1].title)

        assertEquals(view.sectionsItem!![0].firstPosition, expectedSections[0].firstPosition)
        assertEquals(view.sectionsItem!![1].firstPosition, expectedSections[1].firstPosition)
    }

    @Test
    fun `update settings when fingerprint available and autofill available`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        whenCalled(settingStore.autofillAvailable).thenReturn(true)
        val expectedSettings = testHelper.createAccurateListOfSettings(true, true)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(3, R.string.support_title)
        )

        subject.onResume()

        assertEquals(view.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(view.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(view.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(view.settingItem!![3].title, expectedSettings[3].title)

        assertEquals(view.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(view.sectionsItem!![1].title, expectedSections[1].title)

        assertEquals(view.sectionsItem!![0].firstPosition, expectedSections[0].firstPosition)
        assertEquals(view.sectionsItem!![1].firstPosition, expectedSections[1].firstPosition)
    }

    @Test
    fun `update settings when fingerprint unavailable and autofill available`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        whenCalled(settingStore.autofillAvailable).thenReturn(true)
        val expectedSettings = testHelper.createAccurateListOfSettings(false, true)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(2, R.string.support_title)
        )

        subject.onResume()

        assertEquals(view.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(view.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(view.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(view.settingItem!![3].title, expectedSettings[3].title)

        assertEquals(view.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(view.sectionsItem!![1].title, expectedSections[1].title)

        assertEquals(view.sectionsItem!![0].firstPosition, expectedSections[0].firstPosition)
        assertEquals(view.sectionsItem!![1].firstPosition, expectedSections[1].firstPosition)
    }

    @Test
    fun `route to fingeprint dialog when toggle on`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        subject.onResume()
        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(true)

        dispatcherObserver.assertValueAt(0, SettingAction.UnlockWithFingerprintPendingAuth(true))
        val fingerprintDialogAction = dispatcherObserver.values().last() as RouteAction.DialogFragment.FingerprintDialog
        assertEquals(R.string.enable_fingerprint_dialog_title, fingerprintDialogAction.dialogTitle)
        assertEquals(R.string.enable_fingerprint_dialog_subtitle, fingerprintDialogAction.dialogSubtitle)
    }

    @Test
    fun `dispatch save selection to shared prefs when toggle off`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        subject.onResume()
        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(false)
        unlockWithFingerprintStub.onNext(false)
        dispatcherObserver.assertLastValue(SettingAction.UnlockWithFingerprint(false))
    }

    @Test
    fun `handle success enabling fingerprint`() {
        subject.onResume()
        (settingStore.onEnablingFingerprint as Subject).onNext(FingerprintAuthAction.OnSuccess)
        dispatcherObserver.assertValueSequence(
            listOf<Action>(
                SettingAction.UnlockWithFingerprint(true),
                SettingAction.UnlockWithFingerprintPendingAuth(false)
            )
        )
    }

    @Test
    fun `handle error enabling fingerprint`() {
        subject.onResume()
        (settingStore.onEnablingFingerprint as Subject).onNext(FingerprintAuthAction.OnError)
        dispatcherObserver.assertValueSequence(
            listOf<Action>(
                SettingAction.UnlockWithFingerprint(false),
                SettingAction.UnlockWithFingerprintPendingAuth(false)
            )
        )
    }

    @Test
    fun `handle cancel enabling fingerprint`() {
        subject.onResume()
        (settingStore.onEnablingFingerprint as Subject).onNext(FingerprintAuthAction.OnCancel)
        dispatcherObserver.assertValueSequence(
            listOf<Action>(
                SettingAction.UnlockWithFingerprint(false),
                SettingAction.UnlockWithFingerprintPendingAuth(false)
            )
        )
    }

    @Test
    fun `autoLock with fingerprint enabling`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)

        subject.onResume()

        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(true)

        val dispatchedRouteAction = dispatcherObserver.values().last() as RouteAction.DialogFragment.FingerprintDialog
        Assert.assertEquals(dispatchedRouteAction.dialogSubtitle, R.string.enable_fingerprint_dialog_subtitle)
        Assert.assertEquals(dispatchedRouteAction.dialogTitle, R.string.enable_fingerprint_dialog_title)
    }

    @Test
    fun `sendUsageData updated`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        subject.onResume()

        (view.settingItem!![1] as ToggleSettingConfiguration).toggleObserver.accept(false)

        val dispatchedAction = dispatcherObserver.values().last() as SettingAction.SendUsageData
        Assert.assertFalse(dispatchedAction.sendUsageData)
    }

    @Test
    fun `learn more button in sendUsageData tapped`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        subject.onResume()

        (view.settingItem!![1] as ToggleSettingConfiguration).buttonObserver!!.accept(Unit)

        dispatcherObserver.assertLastValue(AppWebPageAction.Privacy)
    }

    @Test
    fun `autoLockTime update requested with secure device`() {
        Mockito.`when`(fingerprintStore.isDeviceSecure).thenReturn(true)
        subject.onResume()

        (view.settingItem!![0] as TextSettingConfiguration).clickListener.accept(Unit)

        dispatcherObserver.assertLastValue(RouteAction.AutoLockSetting)
    }

    @Test
    fun `autoLockTime update requested with insecure device`() {
        Mockito.`when`(fingerprintStore.isDeviceSecure).thenReturn(false)
        subject.onResume()

        (view.settingItem!![0] as TextSettingConfiguration).clickListener.accept(Unit)

        dispatcherObserver.assertLastValue(DialogAction.SecurityDisclaimer)
    }

    @Test
    fun `autofill provider enabled`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        whenCalled(settingStore.autofillAvailable).thenReturn(true)
        subject.onResume()

        (view.settingItem!![1] as ToggleSettingConfiguration).toggleObserver.accept(true)

        dispatcherObserver.assertLastValue(RouteAction.SystemSetting(SettingIntent.Autofill))
    }

    @Test
    fun `autofill provider disabled`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        whenCalled(settingStore.autofillAvailable).thenReturn(true)
        subject.onResume()

        (view.settingItem!![1] as ToggleSettingConfiguration).toggleObserver.accept(false)

        dispatcherObserver.assertLastValue(SettingAction.Autofill(false))
    }
}