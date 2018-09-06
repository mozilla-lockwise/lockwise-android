# On Using Keys and Biometrics #

Below are some findings on using a couple of security features in Android:

* `AndroidKeyStore`
* Biometrics
* Device locks

## Using `AndroidKeyStore` ##

The `AndroidKeyStore` is an implementation of the [Java Cryptography Architecture (JCA)](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)'s `KeyStore` service to manage application-specific cryptographic keys.  Such keys can be created or imported with an associated label, then an opaque `Key` class obtained from that label to use for cryptographic operations.  However, they **cannot** be exported.  The remainder here focuses on creating rather than import.

Obtaining this Keystore is done using the static method `Keystore.getIstance()` and specifying the "AndroidKeyStore" Service Provider Interface (SPI) provider.

New keys are created using an instance of `KeyGenerator` (or `KeyPairGenerator`), again specifying "AndroidKeyStore" as the SPI provider.  When creating a new key, several properties can be applied via `KeyGenParameterSpec`, including:

* key size (in bits)
* encryption block modes (e.g., `GCM`)
* encryption padding (e.g., `NONE`) 
* require authentication
* duration until next authentication (in seconds)

Once set, these properties **cannot** be changed without first deleting then re-importing/-creating the key.  If the key creation requests some user authentication is required, it can only be done if the device has a security lock set (e.g., Pattern/PIN/Password); if user authentication is required for *every* use, it can only be done if the user has at least one fingerprint enrolled.

Keys in the AndroidKeyStore are stored on the device until one of the following happens:

* The app deletes its entry from the KeyStore
* The app's data storage (not data cache) is cleared
* The app is uninstalled

Keys with user authentication required are invalided and cannot be used if any of the following happen:

* The device is hard reset
* The security lock is disabled (e.g., changed from Pattern/PIN/Password to Swipe or None)

Further, "authenticate every use" keys are invalided and cannot be used if any of the following happen:

* A new fingerprint is enrolled
* All fingerprints are unenrolled

Existing Key objects (secret, private, public, and even certificates) are obtained using the Keystore instance's typical methods (e.g., `.getSecretKey()`).  Note that all of the methods on the Key object that would export the value (e.g., getEncoded()) either throw exception or return `null`; this is true even for PublicKeys.

the "Java Standard" Cipher/Mac/Signature classes are used in Android as they are in any other Java/Kotlin environment.  If the key requires authentication, a `UserNotAuthenticatedException` is thrown; if the key no longer valid (as above), a `KeyPermanentlyInvalidatedException` is thrown.

## Using KeyguardManager

The KeyguardManager is a system service that originally used to lock/unlock the keyboard, but has since expanded to lock/unlock the user's device.  It can only be obtained from a `Context` (e.g., `Activity`).

The most interesting methods here are those that determine if a "strong" security lock (Pattern/PIN/Password) is configured and if the device is currently locked.  Both are boolean values; it is not possible to determine _which_ method of lock is configured.

In addition, the KeyguardManager can be used to prompt the user to enter their Pattern/PIN/Password by way of `createConfirmDeviceCredentialIntent()`; if no "strong" security lock is configured, this method returns `null`.

The Intent is created with optional title and description, then dispatched via `startActivityForResult()` to trigger the device prompts.  Applications receive either `RESULT_OK` (if successfully unlocked) or `RESULT_CANCELED` (device prompt is dismissed) via the overridden method `onActivityResult()`.  It is important to note that dispatching and monitoring is best done from a long-running Activity, such as the MainActivity, or the result is never received.

## Using FingerprintManager

**NOTE:** This API is marked as **deprecated** as of API 28 (Android Pie) and replaced with `BiometricPrompt`.

The FingerprintManager is a system service used to interact with a device's fingerprint hardware.  This was added in API 23, and is now deprecated as of API 28.  As with `KeyguardManager` it can only be obtained from a `Context` (e.g., `Activity`).  It also requires the `USE_FINGERPRINT` or `USE_BIOMETRIC` (added in API 28) permission in the app's manifest.

There are methods to determine if fingerprint authentication is possible; detecting if hardware exists and there is at least one fingerprint enrolled.

Engaging the fingerprint reader is done by calling `authenticate()`.  Before doing so, the app must provide a `FingerprintManager.CryptoObject` and a `FingerprintManager.AuthenticationCallback`.  This method returns immediately; further interaction happens via the passed-in `AuthenticationCallback`.  An optional `CancellationSignal` can be provided to disengage the fingerprint hardware out-of-band (e.g., from the user clicking a "Cancel" button).

This object only engages the hardware; it does not display anything to the user itself.  The app is responsible for managing a view regarding the fingerprint reading operations.

Once the reader has succeeded or errored, it is no longer valid; a new instance must be obtained.

### About the CryptoObject

The required `CryptoObject` wraps a `Cipher`, `Mac` or `Signature` object, ready and initialized with the desired key. the scanner is engaged regardless of the key's authentication requirements, so even keys without any requirements can be used.

### About the AuthenticationCallback

The required `AuthenticationCallback` is where events from fingerprint reader attempts are dispatched.  Subclasses need only override the event methods they are interested in.

On success `onAuthenticationSuccessful()` is called with the original `CryptoObject` wrapped in a `AuthenticationResult`.  If the key required authentication, it is now useable within this method's bounds (and only now if authentication is required on every use).

A fingerprint read failure is notified via `onAuthenticaitonFailed()`, such as a unrecognized print.  If there is some other temporary failure (e.g., dirty reader), `onAuthenticationHelp()` is called with the relevant status code and a (device locale appropriate) user-directed help message.

Permanent errors are notified via `onAuthenticationError()`, with the relevant status code and (device locale appropriate) user-directed error message.

## Using BiometricPrompt

The BiometricPrompt is a class used to engage a device's biometrics hardware using a system-provided dialog.  This class is introduced in API 28 to replace FingerprintManager.  The intent is to support not only fingerprint readers, but also facial recognition; it also handles hardware variations, such as in-screen fingerprint readers (e.g., display a user prompt that indicates the on-screen boundaries of the reader).

To create a `BiometricPrmpt`, a `BiometricPrompt.Builder` is created (with an appropriate `Context`) and configured.  The app can set a title, subtitle, description, and "cancel" button behavior.  Once created, the app calls `authenticate()` (just like with `FingerprintManager`).

The hardware-engagement behavior is nearly identical to `FingerprintManager`; the app is notified of events via an instance of `BiometricPrompt.AuthenticationCallback` (which has the exact same methods as `FingerprintManager.AuthenticationCallback`), can be canceled via a `CancellationSignal`, and operates on a `CryptoObject`.  The biggest differences are:

* The `CryptoObject` is optional
* The app must explicitly provide an `Executor` where events are dispatched (this can be `Context.mainExecutor`)
* The app no longer manages any view to interact with the user.
