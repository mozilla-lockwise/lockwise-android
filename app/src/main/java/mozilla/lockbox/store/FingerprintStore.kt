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
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_CANCELED
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_LOCKOUT
import android.os.CancellationSignal
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.components.lib.dataprotect.Keystore
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant

open class FingerprintStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    private val keystore: Keystore = Keystore(Constant.App.keystoreLabel)
) : ContextStore {
    internal val compositeDisposable = CompositeDisposable()
    open var fingerprintManager: FingerprintManager? = null
    open lateinit var keyguardManager: KeyguardManager
    private lateinit var authenticationCallback: AuthenticationCallback

    private var cancellationSignal: CancellationSignal? = null
    private var selfCancelled = false

    private val _state: PublishSubject<AuthenticationState> = PublishSubject.create()
    open val authState: Observable<AuthenticationState> get() = _state

    sealed class AuthenticationState {
        object Succeeded : AuthenticationState()
        data class Failed(val error: String? = null) : AuthenticationState()
        data class Error(val error: String?) : AuthenticationState()
    }

    companion object {
        val shared by lazy { FingerprintStore() }
    }

    open val isDeviceSecure: Boolean
        get() = isFingerprintAuthAvailable || isKeyguardDeviceSecure

    open val isFingerprintAuthAvailable: Boolean
        get() = (fingerprintManager?.isHardwareDetected ?: false) && (fingerprintManager?.hasEnrolledFingerprints()
            ?: false)

    open val isKeyguardDeviceSecure get() = keyguardManager.isDeviceSecure

    init {
        dispatcher.register
            .filterByType(FingerprintSensorAction::class.java)
            .doOnDispose { stopListening() }
            .filter { isFingerprintAuthAvailable }
            .subscribe {
                when (it) {
                    is FingerprintSensorAction.Start -> startListening()
                    is FingerprintSensorAction.Stop -> stopListening()
                }
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        authenticationCallback = AuthenticationCallback(context)
    }

    private fun startListening() {
        if (!isFingerprintAuthAvailable) {
            return
        }

        val cipher = keystore.createEncryptCipher()
        val cryptoObject = FingerprintManager.CryptoObject(cipher)
        cancellationSignal = CancellationSignal()
        selfCancelled = false
        fingerprintManager?.authenticate(cryptoObject, cancellationSignal, 0, authenticationCallback, null)
    }

    private fun stopListening() {
        cancellationSignal?.also {
            selfCancelled = true
            it.cancel()
        }
        cancellationSignal = null
    }

    inner class AuthenticationCallback(val context: Context) : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (!selfCancelled && errorCode != FINGERPRINT_ERROR_CANCELED) {
                if (errorCode == FINGERPRINT_ERROR_LOCKOUT) {
                    _state.onNext(AuthenticationState.Error(context.getString(R.string.fingerprint_error_lockout)))
                } else {
                    _state.onNext(AuthenticationState.Error(errString.toString()))
                }
            }
        }

        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            _state.onNext(AuthenticationState.Succeeded)
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            _state.onNext(AuthenticationState.Failed(helpString.toString()))
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            _state.onNext(AuthenticationState.Failed(context.getString(R.string.fingerprint_not_recognized)))
        }
    }
}