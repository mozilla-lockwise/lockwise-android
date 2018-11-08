/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert.assertEquals
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.adapter.ListAdapterTestHelper
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemListSort
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
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
        override var itemListSortOrder: Observable<ItemListSort> = PublishSubject.create<ItemListSort>()
        override var unlockWithFingerprint: Observable<Boolean> = PublishSubject.create<Boolean>()

        val enablingFingerprint = PublishSubject.create<FingerprintAuthAction>()
        override val onEnablingFingerprint: Observable<FingerprintAuthAction>
            get() = enablingFingerprint
    }

    private lateinit var context: Context
    private lateinit var testHelper: ListAdapterTestHelper
    private val settingView = SettingViewFake()
    private val fingerprintStore = mock(FingerprintStore::class.java)
    private val settingStore = FakeSettingStore()
    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val subject = SettingPresenter(
        settingView,
        RuntimeEnvironment.application.applicationContext,
        dispatcher,
        settingStore,
        fingerprintStore
    )

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        context = RuntimeEnvironment.application
        testHelper = ListAdapterTestHelper(context)
        subject.onViewReady()
    }

    @Test
    fun `update settings when fingerprint available`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        val expectedSettings = testHelper.createAccurateListOfSettings(true)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title)),
            SectionedAdapter.Section(3, context.getString(R.string.support_title))
        )

        subject.onResume()

        assertEquals(settingView.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(settingView.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(settingView.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(settingView.settingItem!![3].title, expectedSettings[3].title)
        assertEquals(settingView.settingItem!![4].title, expectedSettings[4].title)

        assertEquals(settingView.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(settingView.sectionsItem!![1].title, expectedSections[1].title)
    }

    @Test
    fun `update settings when fingerprint unavailable`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        val expectedSettings = testHelper.createAccurateListOfSettings(false)

        val expectedSections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title)),
            SectionedAdapter.Section(3, context.getString(R.string.support_title))
        )

        subject.onResume()

        assertEquals(settingView.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(settingView.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(settingView.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(settingView.settingItem!![3].title, expectedSettings[3].title)

        assertEquals(settingView.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(settingView.sectionsItem!![1].title, expectedSections[1].title)
    }

    @Test
    fun `handle success authentication callback`() {
        settingStore.enablingFingerprint.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthDialogFragment.AuthCallback.OnAuth))
        val last = dispatcherObserver.valueCount() - 1
        dispatcherObserver.assertValueAt(last, SettingAction.UnlockWithFingerprint(true))
    }

    @Test
    fun `handle error authentication callback`() {
        settingStore.enablingFingerprint.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthDialogFragment.AuthCallback.OnError))
        dispatcherObserver.assertLastValue(SettingAction.UnlockWithFingerprint(false))
    }
}