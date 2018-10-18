/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class SettingStoreTest {
    val fakeSendUsageData = PublishSubject.create<Boolean>()

    @Mock
    val sendUsageDataPreference = Mockito.mock(Preference::class.java) as Preference<Boolean>

    @Mock
    val sharedPreferences: RxSharedPreferences = Mockito.mock(RxSharedPreferences::class.java)

    val subject = SettingStore()

    val sendUsageDataObserver = TestObserver<Boolean>()

    @Before
    fun setUp() {
        `when`(sendUsageDataPreference.asObservable()).thenReturn(fakeSendUsageData)
        `when`(sharedPreferences.getBoolean("send_usage_data", true)).thenReturn(sendUsageDataPreference)
        subject.apply(sharedPreferences)

        subject.sendUsageData.subscribe(sendUsageDataObserver)
    }

    @Test
    fun sendUsageDataTest() {
        val newValue = false
        fakeSendUsageData.onNext(newValue)

        sendUsageDataObserver.assertValue(newValue)
    }
}