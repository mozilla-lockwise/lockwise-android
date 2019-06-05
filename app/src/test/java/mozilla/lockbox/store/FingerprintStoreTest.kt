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
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_LOCKOUT
import android.os.CancellationSignal
import android.preference.PreferenceManager
import android.util.Base64
import io.reactivex.observers.TestObserver
import mozilla.components.lib.dataprotect.Keystore
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.doReturn
import org.powermock.api.mockito.PowerMockito.verifyStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import javax.crypto.Cipher
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(Keystore::class, FingerprintManager::class)
@Suppress("DEPRECATION")
class FingerprintStoreTest {
    @Mock
    val keystore: Keystore = PowerMockito.mock(Keystore::class.java)

    @Mock
    val cipher: Cipher = PowerMockito.mock(Cipher::class.java)

    @Mock
    val fingerprintManager: FingerprintManager = Mockito.mock(FingerprintManager::class.java)

    @Mock
    val keyguardManager: KeyguardManager = Mockito.mock(KeyguardManager::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    private val dispatcher = Dispatcher()
    val subject = FingerprintStore(dispatcher, keystore)

    @Before
    fun setUp() {
        whenCalled(keystore.createEncryptCipher()).thenReturn(cipher)
    }

    @Test
    fun `fingerprint start sensor action when fingerprints are not available`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(false)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        applyMockContext()

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verifyZeroInteractions(keystore)
    }

    @Test
    fun `fingerprint start sensor action when fingerprints are available`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        applyMockContext()

        val callbackCaptor = ArgumentCaptor.forClass(FingerprintStore.AuthenticationCallback::class.java)

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verify(keystore).createEncryptCipher()
        verify(fingerprintManager).authenticate(
            any(FingerprintManager.CryptoObject::class.java),
            any(CancellationSignal::class.java),
            eq(0),
            callbackCaptor.capture(),
            eq(null)
        )
    }

    @Test
    fun `callbacks with lockout authentication errors`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        applyMockContext()

        val callbackCaptor = ArgumentCaptor.forClass(FingerprintStore.AuthenticationCallback::class.java)

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verify(keystore).createEncryptCipher()
        verify(fingerprintManager).authenticate(
            any(FingerprintManager.CryptoObject::class.java),
            any(CancellationSignal::class.java),
            eq(0),
            callbackCaptor.capture(),
            eq(null)
        )

        val stateObserver = TestObserver.create<FingerprintStore.AuthenticationState>()
        subject.authState.subscribe(stateObserver)

        callbackCaptor.value.onAuthenticationError(FINGERPRINT_ERROR_LOCKOUT, null)

        
    }

    @Test
    fun `callbacks with authentication succeeded`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        applyMockContext()

        val callbackCaptor = ArgumentCaptor.forClass(FingerprintStore.AuthenticationCallback::class.java)

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verify(keystore).createEncryptCipher()
        verify(fingerprintManager).authenticate(
            any(FingerprintManager.CryptoObject::class.java),
            any(CancellationSignal::class.java),
            eq(0),
            callbackCaptor.capture(),
            eq(null)
        )
    }

    @Test
    fun `callbacks with authentication help`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        applyMockContext()

        val callbackCaptor = ArgumentCaptor.forClass(FingerprintStore.AuthenticationCallback::class.java)

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verify(keystore).createEncryptCipher()
        verify(fingerprintManager).authenticate(
            any(FingerprintManager.CryptoObject::class.java),
            any(CancellationSignal::class.java),
            eq(0),
            callbackCaptor.capture(),
            eq(null)
        )
    }

    @Test
    fun `callbacks with authentication failed`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        applyMockContext()

        val callbackCaptor = ArgumentCaptor.forClass(FingerprintStore.AuthenticationCallback::class.java)

        dispatcher.dispatch(FingerprintSensorAction.Start)

        verify(keystore).createEncryptCipher()
        verify(fingerprintManager).authenticate(
            any(FingerprintManager.CryptoObject::class.java),
            any(CancellationSignal::class.java),
            eq(0),
            callbackCaptor.capture(),
            eq(null)
        )
    }

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