# Release Notes

## 1.0.0 (Build 3137)

Introducing Firefox Lockbox for Android!

Minor changes since Release Candidate:

- Fix Adjust integration (#521)
- Add more autofill telemetry actions (#522)
- use blocking operators rather than inline checks (#506)
- only update lock status after updating list (#505)

## 1.0.0 (Build 3024)

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
