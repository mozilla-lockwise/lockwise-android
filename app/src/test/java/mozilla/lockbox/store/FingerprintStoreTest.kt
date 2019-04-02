/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.store

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenCalled

@Suppress("DEPRECATION")
class FingerprintStoreTest {
    @Mock
    val fingerprintManager = Mockito.mock(FingerprintManager::class.java)

    @Mock
    val keyguardManager = Mockito.mock(KeyguardManager::class.java)

    @Mock
    val context = Mockito.mock(Context::class.java)

    val subject = FingerprintStore()

    @Test
    fun `isDeviceSecure when the device is fingerprint secure and not PIN or password secure`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        applyMockContext()

        Assert.assertTrue(subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is fingerprint hardware but there are no enrolled fingers and the device is not PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        applyMockContext()

        Assert.assertFalse(subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is no fingerprint hardware and the device is not PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(false)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        applyMockContext()

        Assert.assertFalse(subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is no fingerprint hardware but the device is PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(false)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(true)

        applyMockContext()

        Assert.assertTrue(subject.isDeviceSecure)
    }

    private fun applyMockContext() {
        whenCalled(context.getSystemService(Context.FINGERPRINT_SERVICE)).thenReturn(fingerprintManager)
        whenCalled(context.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager)

        subject.injectContext(context)
    }
}