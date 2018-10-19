package mozilla.lockbox.store

import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.action.AuthenticationAction
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
) {
    internal val compositeDisposable = CompositeDisposable()
    private lateinit var fingerprintManager: FingerprintManager

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private var cancellationSignal: CancellationSignal? = null
    private var selfCancelled = false

    val authState: ReplaySubject<AuthenticationState> = ReplaySubject.create()

    companion object {
        val shared = FingerprintStore()
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_KEY = "DEFAULT_KEY"
    }

    sealed class AuthenticationState {
        object Succeeded : AuthenticationState()
        object Failed : AuthenticationState()
        data class Error(val error: String?) : AuthenticationState()
    }

    init {
        dispatcher.register.filterByType(AuthenticationAction::class.java)
            .subscribe {
                when (it) {
                    is AuthenticationAction.StartListening -> initFingerprint()
                    is AuthenticationAction.StopListening -> stopListening()
                }
            }
            .addTo(compositeDisposable)
    }

    fun apply(manager: FingerprintManager) {
        fingerprintManager = manager
    }

    fun isFingerprintAuthAvailable(): Boolean {
        return fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
    }

    private fun initFingerprint() {
        setupKeyStoreAndKeyGenerator()
        createKey(DEFAULT_KEY)
        val (defaultCipher: javax.crypto.Cipher, cipherNotInvalidated: javax.crypto.Cipher) = setupCiphers()
        initCipher(defaultCipher, DEFAULT_KEY)

        startListening(FingerprintManager.CryptoObject(defaultCipher))
    }

    private fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
        if (!isFingerprintAuthAvailable()) return
        cancellationSignal = CancellationSignal()
        selfCancelled = false
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, AuthenticationCallback(), null)
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

    inner class AuthenticationCallback : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (!selfCancelled) {
                authState.onNext(AuthenticationState.Error(errString.toString()))
            }
        }

        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            authState.onNext(AuthenticationState.Succeeded)
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            authState.onNext(AuthenticationState.Error(helpString.toString()))
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            authState.onNext(AuthenticationState.Failed)
        }
    }
}