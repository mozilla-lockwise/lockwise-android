# Accessibility on Lockwise for Android

On Lockwise for Android, we implement two forms of automatic testing and
encourage developers and testers to use the [recommended forms of manual testing](/test-plan.md#accessibility)
to ensure that our application is accessible to every possible user.

## Automatic Accessibility

### Lint Checks

These are fairly simple static analysis checks. You're probably familiar with warning pop-ups when you have small code style issues; in Lockwise for Android, the build will fail if you do not address these warnings!

The most important accessibility warning is the `contentDescription` warning on images. This warning will appear when the `contentDescription` attribute-- the text description of the image --has not been set.

### Espresso Tests

`AccessibilityChecks.enable()` -- This should be added in setup methods for your Espresso tests.

When adding new Espresso tests, you may notice failures related to things like
tap target size. The automated Espresso accessibility checks occur on every
method call in the [`ViewActions`](https://developer.android.com/reference/androidx.test/espresso/action/ViewActions) class.

## Manual Accessibility Testing

### AccessibilityScanner

The [Android Accessibility Scanner](https://support.google.com/accessibility/android/answer/6376570?hl=en) is a tool used to scan your app's screen to identify potential areas of improvement.

If you are using an emulator, download the Accessibility Scanner [APK package](https://apkpure.com/accessibility-scanner/com.google.android.apps.accessibility.auditor). Once the APK is downloaded, drag and drop the file into your active emulator and follow onscreen instructions to use the Scanner on your app.

If you are using an Android device, download the Android Accessibility Scanner from the Google Play Store on your phone.

### TalkBack

TalkBack is a screen reader that is built into Android devices. This tool can be used to test your application by closing your eyes while attempting to navigate.

Instructions on enabling TalkBack on your device: <https://support.google.com/accessibility/android/answer/6283677?hl=en&ref_topic=3529932>

### Changing Text

A common accessibility problem arises with users who have changed their font size. Test your app by [increasing your device's font size](https://support.google.com/accessibility/android/answer/6006972?hl=en&ref_topic=9079043).

In addition to font size, be aware of text contrast, [color inversion](https://support.google.com/accessibility/android/answer/6151800?hl=en&ref_topic=9079043), colorblindness, and color correction.
