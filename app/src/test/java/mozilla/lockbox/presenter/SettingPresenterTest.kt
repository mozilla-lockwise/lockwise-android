/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
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
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.`when` as whenCalled
import org.robolectric.RuntimeEnvironment

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

    class FakeSettingStore : SettingStore() {
        override var sendUsageData: Observable<Boolean> = PublishSubject.create<Boolean>()
        override var itemListSortOrder: Observable<Setting.ItemListSort> = PublishSubject.create<Setting.ItemListSort>()
        val unlockWithFingerprintStub = PublishSubject.create<Boolean>()
        override var unlockWithFingerprint: Observable<Boolean> = unlockWithFingerprintStub
        override var unlockWithFingerprintPendingAuth: Observable<Boolean> = PublishSubject.create<Boolean>()
        override var autoLockTime: Observable<Setting.AutoLockTime> = PublishSubject.create<Setting.AutoLockTime>()

        override val onEnablingFingerprint: Observable<FingerprintAuthAction> = PublishSubject.create()
    }

    private lateinit var testHelper: ListAdapterTestHelper
    private val view = SettingViewFake()
    private val fingerprintStore = mock(FingerprintStore::class.java)
    private val settingStore = FakeSettingStore()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    val subject = SettingPresenter(
        view,
        dispatcher,
        settingStore,
        fingerprintStore
    )

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        testHelper = ListAdapterTestHelper()
    }

    @Test
    fun `update settings when fingerprint available`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        val expectedSettings = testHelper.createAccurateListOfSettings(true)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.security_title),
            SectionedAdapter.Section(3, R.string.support_title)
        )

        subject.onViewReady()

        assertEquals(view.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(view.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(view.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(view.settingItem!![3].title, expectedSettings[3].title)
        assertEquals(view.settingItem!![4].title, expectedSettings[4].title)

        assertEquals(view.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(view.sectionsItem!![1].title, expectedSections[1].title)

        assertEquals(view.sectionsItem!![0].firstPosition, expectedSections[0].firstPosition)
        assertEquals(view.sectionsItem!![1].firstPosition, expectedSections[1].firstPosition)
    }

    @Test
    fun `update settings when fingerprint unavailable`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        val expectedSettings = testHelper.createAccurateListOfSettings(false)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, R.string.security_title),
            SectionedAdapter.Section(2, R.string.support_title)
        )

        subject.onViewReady()

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
        subject.onViewReady()
        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(true)

        dispatcherObserver.assertValueAt(0, SettingAction.UnlockWithFingerprintPendingAuth(true))
        val fingerprintDialogAction = dispatcherObserver.values().last() as RouteAction.DialogFragment.FingerprintDialog
        assertEquals(R.string.enable_fingerprint_dialog_title, fingerprintDialogAction.dialogTitle)
        assertEquals(R.string.enable_fingerprint_dialog_subtitle, fingerprintDialogAction.dialogSubtitle)
    }

    @Test
    fun `dispatch save selection to shared prefs when toggle off`() {
        whenCalled(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        subject.onViewReady()
        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(false)
        settingStore.unlockWithFingerprintStub.onNext(false)
        dispatcherObserver.assertLastValue(SettingAction.UnlockWithFingerprint(false))
    }

    @Test
    fun `handle success enabling fingerprint`() {
        subject.onViewReady()
        (settingStore.onEnablingFingerprint as Subject).onNext(
            FingerprintAuthAction.OnAuthentication(
                FingerprintAuthDialogFragment.AuthCallback.OnAuth
            )
        )
        dispatcherObserver.assertValueSequence(
            listOf<Action>(
                SettingAction.UnlockWithFingerprint(true),
                SettingAction.UnlockWithFingerprintPendingAuth(false)
            )
        )
    }

    @Test
    fun `handle error enabling fingerprint`() {
        subject.onViewReady()
        (settingStore.onEnablingFingerprint as Subject).onNext(
            FingerprintAuthAction.OnAuthentication(
                FingerprintAuthDialogFragment.AuthCallback.OnError
            )
        )
        dispatcherObserver.assertValueSequence(
            listOf<Action>(
                SettingAction.UnlockWithFingerprint(false),
                SettingAction.UnlockWithFingerprintPendingAuth(false)
            )
        )
    }

    @Test
    fun `handle cancel enabling fingerprint`() {
        subject.onViewReady()
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

        subject.onViewReady()

        (view.settingItem!![0] as ToggleSettingConfiguration).toggleObserver.accept(true)

        val dispatchedRouteAction = dispatcherObserver.values().last() as RouteAction.DialogFragment.FingerprintDialog
        Assert.assertEquals(dispatchedRouteAction.dialogSubtitle, R.string.enable_fingerprint_dialog_subtitle)
        Assert.assertEquals(dispatchedRouteAction.dialogTitle, R.string.enable_fingerprint_dialog_title)
    }

    @Test
    fun `sendUsageData updated`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        subject.onViewReady()

        (view.settingItem!![2] as ToggleSettingConfiguration).toggleObserver.accept(false)

        val dispatchedAction = dispatcherObserver.values().last() as SettingAction.SendUsageData
        Assert.assertFalse(dispatchedAction.sendUsageData)
    }

    @Test
    fun `autoLockTime update requested`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        subject.onViewReady()

        (view.settingItem!![0] as TextSettingConfiguration).clickListener.accept(Unit)

        dispatcherObserver.assertLastValue(RouteAction.AutoLockSetting)
    }
}