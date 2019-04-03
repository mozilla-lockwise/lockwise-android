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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

open class FingerprintStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {
    internal val compositeDisposable = CompositeDisposable()
    open var fingerprintManager: FingerprintManager? = null
    open lateinit var keyguardManager: KeyguardManager
    private lateinit var authenticationCallback: AuthenticationCallback

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private var cancellationSignal: CancellationSignal? = null
    private var selfCancelled = false

    private val _state: PublishSubject<AuthenticationState> = PublishSubject.create()
    open val authState: Observable<AuthenticationState> get() = _state

    companion object {
        val shared = FingerprintStore()
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_KEY = "DEFAULT_KEY"
    }

    sealed class AuthenticationState {
        object Succeeded : AuthenticationState()
        data class Failed(val error: String? = null) : AuthenticationState()
        data class Error(val error: String?) : AuthenticationState()
    }

    init {
        dispatcher.register.filterByType(FingerprintSensorAction::class.java)
            .doOnDispose { stopListening() }
            .filter { isFingerprintAuthAvailable }
            .subscribe {
                when (it) {
                    is FingerprintSensorAction.Start -> initFingerprint()
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

    open val isDeviceSecure: Boolean
        get() = isFingerprintAuthAvailable || isKeyguardDeviceSecure

    open val isFingerprintAuthAvailable: Boolean
        get() = (fingerprintManager?.isHardwareDetected ?: false) && (fingerprintManager?.hasEnrolledFingerprints() ?: false)

    open val isKeyguardDeviceSecure get() = keyguardManager.isDeviceSecure

    private fun initFingerprint() {
        setupKeyStoreAndKeyGenerator()
        createKey(DEFAULT_KEY)
        val (defaultCipher: javax.crypto.Cipher, _: javax.crypto.Cipher) = setupCiphers()
        initCipher(defaultCipher, DEFAULT_KEY)

        startListening(FingerprintManager.CryptoObject(defaultCipher))
    }

    private fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
        if (!isFingerprintAuthAvailable) {
            return
        }
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

    private fun setupKeyStoreAndKeyGenerator() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }

        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchProviderException ->
                    throw RuntimeException("Failed to get an instance of KeyGenerator", e)
                else -> throw e
            }
        }
    }

    private fun setupCiphers(): Pair<Cipher, Cipher> {
        val defaultCipher: Cipher
        val cipherNotInvalidated: Cipher
        try {
            val cipherString =
                "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
            defaultCipher = Cipher.getInstance(cipherString)
            cipherNotInvalidated = Cipher.getInstance(cipherString)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException ->
                    throw RuntimeException("Failed to get an instance of Cipher", e)
                else -> throw e
            }
        }
        return Pair(defaultCipher, cipherNotInvalidated)
    }

    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        try {
            keyStore.load(null)
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(keyName, null) as SecretKey)
            return true
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return false
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is InvalidKeyException -> throw RuntimeException("Failed to init Cipher", e)
                else -> throw e
            }
        }
    }

    private fun createKey(keyName: String) {
        try {
            keyStore.load(null)

            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

            keyGenerator.run {
                init(builder.build())
                generateKey()
            }
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is InvalidAlgorithmParameterException,
                is CertificateException,
                is IOException -> throw RuntimeException(e)
                else -> throw e
            }
        }
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