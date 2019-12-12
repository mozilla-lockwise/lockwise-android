# Release Notes

## 3.3.0 (Build 5783)

Stability and UI cleanup.

All changes since version 3.2.0:

- Bump the required Android Components version to 19.0.1 (#1070)
- Update ui tests (#1083)
- Fix interrupts causing the app to go to the item list. (#1075)
- UX cleanup part deux (#1071)
- Introduce Glean SDK (#1085)
- Metadata cleanups for datastore and autofill. (#1102)
- Localization updates (#1110)

## 3.3.0 (Build 5658)

Stability and UI cleanup.

All changes since version 3.2.0:

- Bump the required Android Components version to 19.0.1 (#1070)
- Update ui tests (#1083)
- Fix interrupts causing the app to go to the item list. (#1075)
- UX cleanup part deux (#1071)

## 3.2.0 (Build 5522)

Stability and UI cleanup.

All changes since version 3.1.0:

- Search bar text handles have violet background (#1035)
- Reconcile l10n strings for translation #1020
- Close keyboard when exiting feedback form (#1057)
- Update ---feature.md (#1063)
- The device back button doesn't have the same functionality with the X button in Edit Login Mode (994)
- Remove "name" field from the edit view (#1010)
- Whooops! Wrong URL displayed when accessing Learn More from Lockwise settings (#1048)

## 3.1.0 (Build 5356)

Improve autofill detection and increase target SDK to 28.

All changes since version 3.0.1:

- Hide soft keyboard on unlock with pin (#1024)
- Fixup crash in isButton with Java/Kotlin null dereference (#1039)
- Make autofill detection more robust (#1031)
- Increase target sdk to 28 (#1026)
- Remove color state change on hostname when clicking outside of the cardview (#1030)
- Remove name field from edit view (#1025)

## 3.0.1 (Build 5215)

Improve threading and log in keyboard experience.

All changes since version 3.0.0:

- Soft pan layout when keyboard is opened on webview (#929)
- Update syncCredentials on the correct thread after their generation (#1012, #1013)

## 3.0.0 (Build 5177)

Add capabilities to edit entries, delete entries, and capture new entries from autofill.

All changes since version 2.0.0: 

- Use extra fields to detect duplicates (#997)
- Generate the token URL of the main thread (#1003)
- 980: launch hostname from icon in detail (#1004)
- 948: Prevent duplicates during edit
- Cache access tokens (#945)
- 873: Update faq links to sumo (#951)
- Import l10n. (#964)
- 956: Disable editing hostname (#963)
- 938-uitests-add-error-handling-edit-view (#966)
- 920-add-new-edit-screenshots-tests (#922)
- Add intent filter for launching app from a URL (#967)
- Autofill capture an existing login new to Lockwise (#943)
- 899: UX cleanup for edit (#962)
- 969 refactor screenshotstests use robot (#970)

## 2.0.0 (Build 4820)

Refine telemetry for sync and fix crashes.

All changes since version 1.1.3: 

- Telemetry for sync (#835)
- String updates for localization (#852)
- Update app services, android components, and megazord configuration (#865)
- Update Readme with l10n process (#872)
- Fix obvious NPE sync crash (#884)
- Create infrastructure for feature flags (#882)
- Add telemetry probes to syncIfRequired trigger syncs (#887)
- Import strings from l10n (#893)
- Remove timeout from sync (#889)
- Fail fast if the edge between to routes is not in the nav graph. (#895)
- Add fxalogin to autofill onboarding route. (#901)
- Routing back to itemlist on login/search/feedback forms (#890)
- Add null text value for empty usernames (#902)

## 1.1.3 (Build 4584)

We fixed more bugs to help sync for some users, as well as some UI updates to standardize localized views.

All changes since version 1.1.2:

- Import strings from android-l10n (#809)
- Adjust spinner dropdown size to fit longer words (l10n) (#812)
- Ellipsize text in item list (#811)
- Reset support when syncing (#826)
- Add dot to sentence (#837)
- Telemetry for sync (#835)
- Update metrics docs to reflect telemetry sync changes (#841)

## 1.1.2 (Build 4405)

We fixed more bugs to help stabilize sign in and sync for some users. We also hardened the security of the app even more.

Changes since the last build:

- Import strings from android-l10n (#769)
- Bottom text is cut from the screen (#771)
- Update A-C (1.0.0) and AS (0.32.0) to latest versions (#774)

All changes since version 1.1.1:

- additional changes to translated strings (#730)
- made sure the search icon is visible for all languages (#737)
- added error handling and reporting improvements for troubleshooting (#722)
- changed to use dataprotect for fingerprint interactions (#741)
- explicitly set theme in item list to not show the wrong background on some screens (#743)
- updated android-components and application-services dependencies (#751)
- hide the password when browsing away from entry (#685)
- add a timeout for when sync does not complete (#747)
- Remove registered device from AS device list (#732)
- implement sync interval every 24 hours (#756)
- update ignored gradle dependencies (#755)
- Fix DataStore backend not set error (#738)
- Import strings from android-l10n (#769)
- Bottom text is cut from the screen (#771)
- Update A-C (1.0.0) and AS (0.32.0) to latest versions (#774)

## 1.1.2 (Build 4342)

_Date: 2019-06-24_

We fixed more bugs to help stabilize sign in and sync for some users. We also hardened the security of the app even more.

Changes since the last build:

- Remove registered device from AS device list (#732)
- implement sync interval every 24 hours (#756)
- update ignored gradle dependencies (#755)
- Fix DataStore backend not set error (#738)

All changes since version 1.1.1:

- additional changes to translated strings (#730)
- made sure the search icon is visible for all languages (#737)
- added error handling and reporting improvements for troubleshooting (#722)
- changed to use dataprotect for fingerprint interactions (#741)
- explicitly set theme in item list to not show the wrong background on some screens (#743)
- updated android-components and application-services dependencies (#751)
- hide the password when browsing away from entry (#685)
- add a timeout for when sync does not complete (#747)
- Remove registered device from AS device list (#732)
- implement sync interval every 24 hours (#756)
- update ignored gradle dependencies (#755)
- Fix DataStore backend not set error (#738)

## 1.1.2 (Build 4259)

_Date: 2019-06-11_

We fixed more bugs to help stabilize sign in and sync for some users. We also hardened the security of the app even more.

Changes since version 1.1.1:

- additional changes to translated strings (#730)
- made sure the search icon is visible for all languages (#737)
- added error handling and reporting improvements for troubleshooting (#722)
- changed to use dataprotect for fingerprint interactions (#741) 
- explicitly set theme in item list to not show the wrong background on some screens (#743) 
- updated android-components and application-services dependencies (#751)
- hide the password when browsing away from entry (#685)
- add a timeout for when sync does not complete (#747)

## 1.1.1 (Build 4053)

_Date: 2019-05-30_

We fixed a handful of bugs and improved the overall security and stability of the app including signing in and syncing.

Changes since version 1.1.0:

- added a small delay to navigate to the confirmation screen (#709)
- update application-services and android-components (#712)
- adjust to not duplicate the top fragment and require two back taps (#710)
- close db when going to the background (#713)
- simply unlock when no device security present (#718) 
- disconnect account security bugs (#694)
- check system elapsed time on startup (#715)

## 1.1.0 (Build 3951)

_Date: 2019-05-21_

Firefox Lockbox is now Firefox Lockwise! With the new name also comes a new look!

Changes since the last release:

- Rebrand Lockbox to new Lockwise brand, name, colors, etc. (#653 #669 #677 #676 #679)
- Minify and shrink release APK (#664) 
- Import localized strings: fr, de, it, es (#674 #686 #687 #691)

## 1.0.3 (Build 3819)

_Date 2019-05-10_

Minor update that includes more stability and crash fixes.

New since last release candidate:

- fix string interpolation to show product name on fingerprint dialog (#659)

All changes since last version:

- null fragment if fragment list unavailable (#618)
- fix crash for null className on Android Q (#609)
- remove deprecated calls (#591)
- fix keystore crashes during autofill (#624)
- fix stuck onboarding screen after autofill settings (#642)
- string updates to prepare for localization (#617)
- major dependency updates (#600)
- fix for DumpNode crashes on Q (#623)
- more reliably clear the clipboard (#644)
- secure flag on autofill (#647)
- refactor locked presenters (#643)

## 1.0.3 (Build 3722)

_Date 2019-05-03_

Minor update that includes more stability and crash fixes.

- null fragment if fragment list unavailable (#618)
- fix crash for null className on Android Q (#609)
- remove deprecated calls (#591)

New since last build:

- fix keystore crashes during autofill (#624)
- fix stuck onboarding screen after autofill settings (#642)
- string updates to prepare for localization (#617)
- major dependency updates (#600)
- fix for DumpNode crashes on Q (#623)
- more reliably clear the clipboard (#644)
- secure flag on autofill (#647)
- refactor locked presenters (#643)

## 1.0.3 (Build 3522)

_Date: 2019-04-26_

First Alpha build with crash fixes.

- null fragment if fragment list unavailable (#618)
- fix crash for null className on Android Q (#609)
- remove deprecated calls (#591)

## 1.0.2 (Build 3428)

_Date: 2019-04-19_

Second Alpha/Beta build with a fix for autofill crashes and proper version number in-app.

Additional changes since 3417:

- set version name (number) to 1.0.2 (#605)
- fix for an IllegalStateException autofill crash (#548)

## 1.0.2 (Build 3417)

_Date: 2019-04-19_

First Alpha/Beta build with dependency updates, paving way for more crash reporting and fixes coming soon.

- update constraintlayout dependency (#578) 
- fix race condition in LocketPresenterTest (#573)
- update more dependencies (#572)
- add Sentry to log errors and crashes (#588)
- update UI tests (#590)
- enable R8 and byte-level optimizations (#594)

## 1.0.1 (Build 3320)

_Date: 2019-04-04_

Release Candidate

- improve navigation and routing for stability (#499)
- make finger print auth action flatter (#529)
- fix font in spinner list (#540)
- add nullsafe operations on FingerprintManager to prevent crashes (#546)
- change network connection checks (#518)
- fix error text alignment (#538)
- fix mismatching monster on unlock screen (#553)
- improve autofill behavior after backgrounding (#502)
- add app bar shadow (#566)
- add build number on settings screen (#552)
- add title to sign in screen (#569)
- autolock on device restart (#568)
- merge route presenters (#539) 

## 1.0.1 (Build 3307)

_Date: 2019-04-01_

Third internal test build for post-launch point release.

- improve navigation and routing for stability (#499)
- make finger print auth action flatter (#529)
- fix font in spinner list (#540)
- add nullsafe operations on FingerprintManager to prevent crashes (#546)
- change network connection checks (#518)
- fix error text alignment (#538)
- fix mismatching monster on unlock screen (#553)
- improve autofill behavior after backgrounding (#502)
- add app bar shadow (#566)
- add build number on settings screen (#552)
- add title to sign in screen (#569)
- autolock on device restart (#568)

## 1.0.1 (Build 3264)

_Date: 2019-04-01_

Second internal test build for post-launch point release.

- improve navigation and routing for stability (#499)
- make finger print auth action flatter (#529)
- fix font in spinner list (#540)
- add nullsafe operations on FingerprintManager to prevent crashes (#546)
- change network connection checks (#518)
- fix error text alignment (#538)
- fix mismatching monster on unlock screen (#553)
- improve autofill behavior after backgrounding (#502)
- add app bar shadow (#566)

## 1.0.1 (Build 3190)

_Date: 2019-03-26_

First internal test build for post-launch point release.

- improve navigation and routing for stability (#499)
- make finger print auth action flatter (#529)

## 1.0.0 (Build 3171)

_Date: 2019-03-26_

Introducing Firefox Lockwise for Android!

Minor change since Build 3137:

- add error handler to filtering to prevent crashes on autofill (#526)

## 1.0.0 (Build 3137)

_Date: 2019-03-21_

Minor changes since Release Candidate:

- Fix Adjust integration (#521)
- Add more autofill telemetry actions (#522)
- use blocking operators rather than inline checks (#506)
- only update lock status after updating list (#505)

## 1.0.0 (Build 3024)

_Date: 2019-03-14_

Release Candidate

- moved some dependencies into our codebase (#491)
- fixed crash on entries with no username (#496)
- fixed bug in onboarding screen (#495)
- added telemetry to autofill events (#498)
- added Adjust (#497)

## 1.0 (Build 2955)

_Date: 2019-03-08_

Fourth internal Alpha release on Play Store. Changes include:

- more telemetry events (#439)
- onboarding instructions if no device security set (#438)
- handling automatic locking if no device security set (#456)
- automatically launch biometrics prompt when returning from background (#428)
- improvements to networking warning error (#458)
- added handling for empty username entries (#451)
- autofill infrastructure and UI improvements (#470 #471 #474 #475 #487)

## 1.0 (Build 2599)

_Date: 2019-02-19_

Third internal Alpha release on Play Store. Changes include:

- added autofill support to webpages (#404)
- added launch hint icon next to hostname (#399)
- added support to enable autofill from settings screen (#417)
- added the public suffix list to be less permissive on fill matching (#425)
- added biometric unlock onboarding flow (#396)
- added confirmation screen during onboarding (#432)
- added authentication during autofill when app is locked (#413)
- made entire username and password rows tappable to copy (#400)
- added FAQ links and instructions (#423)
- added onboarding screen to enable autofill (#429)
- improved initialization, autolock, and autofill states (#437, #446)

## 1.0 (Build 2247)

_Date: 2019-01-26_

Second internal Alpha release on Play Store. Changes include:

- updates to android-components dependencies (#358, #377)
- smaller app size due to less redundant dependency inclusion (#353)
- stability and fixes for locking and unlocking (#347)
- initial credential provider / autofill infrastructure (#351)
- fix for support text box overlapping setting toggle (#370)
- added copy notification (toast) styling (#374)
- autofill added to app username/password forms (#372)
- more stability around locking and unlocking and routing (#349)
- improved notifications and handling when offline (#286)
- add placeholder splash screen so no flash of "welcome" screen (#391)
- add "no saved entries" screen so you know why its empty (#397)

## 1.0 (Build 2050)

_Date: 2019-01-08_

First internal Alpha release on Play Store
