# Accessibility on Lockbox for Android

On Lockbox for Android, we implement two forms of automatic testing, and
encourage developers and testers to use the recommended forms of manual testing
to ensure that our app is as accessible to every possible user.

### Automatic Accessibility

###### Lint Checks

These are fairly simple static analysis checks. You're probably familiar with
seeing warnings pop up when you have small code style issues; in Lockbox for
Android, the build will fail if you do not address these warnings! The most
important one for accessibility purposes is the `contentDescription` warning
on images.

###### Espresso Tests

```
AccessibilityChecks.enable()
```
When adding new Espresso tests, you may notice failures related to things like
tap target size. The automated Espresso accessibility checks occur on every
method call in the [`ViewActions`](https://developer.android.com/reference/android/support/test/espresso/action/ViewActions) class.

### Manual Accessibility Testing

###### AccessibilityScanner

###### TalkBack

###### Changing Text Sizes
