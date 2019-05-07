# Test Plan

_Test plan for [Firefox Lockwise for Android][1]_

## Overview

Firefox Lockwise for Android is a new mobile app developed with a pre-defined set of P1 "must have" requirements and a target release at the beginning of February 2019 in the Google Play Store.

Mozilla Product Integrity provides embedded QA to work with the team throughout the bi-weekly sprints allowing for ongoing testing and feedback, issue triage, and continuous test plan development and end-to-end regression testing in order to accommodate a quick release schedule.

### Ownership

* Product Integrity: Andrei Bodea, Bogdan Surd
* Product Manager: Sandy Sage
* Engineering Manager: Devin Reams
* Engineering Leads: Sasha Heinen, James Hugman, Matt Miller, Elise Richards

### Entry Criteria

* PI has access to all product documentation, designs, code
* The Android app code is available on GitHub and builds:
  - locally via Android Studio (Branch and Release)
  - on device via bitrise.io (Branch and Release)
  - on device via Play Store (Release)

### Exit Criteria

* All test suites against P1 "must have" features have performed
* All bugs related to the P1 "must have" features have been triaged
* All bugs resolved fixed have been verified

## Test Matrix

- Devices to be tested:
  - **TBD**
- Major operating system versions to be tested:
  - Minimum: API 23
  - Target: API 28

## Test Suites

- Documented in TestRail: [https://testrail.stage.mozaws.net/index.php?/suites/view/3060&group_by=cases:section_id&group_order=asc][2] (internal Mozilla tool)
- Performed twice-weekly
- Covers all [P1 "must have" Requirements][3] (internal Mozilla document)
  - 01 Sign in to Sync
  - 02 Onboarding
  - 03 Access saved entries
  - 04 No entries support
  - 05 Biometrics to lock/unlock
  - 06 View entry
  - 07 Copy / paste retrieval
  - 08 View password
  - 09 Autofill
  - 10 Account management
  - 11 Browser setting
  - 12 Support
  - 13 Telemetry
  
## Accessibility

There are a number of best practices and accessibility features available to Android developers and we intend to build and test for.

We've [documented the tools and techniques available to test accessibility][7] and ask that every pull request consider these before merging so as to not accumulate "debt" during development but also test all of the following at certain intervals in the project:

## 1. TalkBack support

The reading of on-screen interface items and objects. Testing for:

- All on-screen navigation and button titles are read
- All on-screen alerts and popovers (interrupting the interface) are read
- All labels and text elements on screen are read
- Interaction elements like input boxes and filters have meaningful instructions
- Links to open websites are clearly indicated and read
- Swiping left/right to select next/previous elements works usefully
- Interface scrolling is available where needed

## 2. User interface design

The interface is legible and tappable by designing and testing for:

- adequate color contrasts (e.g.: no light text on light background)
- button and input sizes (e.g.: large enough to tap into easily and consistently)
- text sizes follow system adjustment (i.e. respect "Larger Text" setting)
  
### Out of Scope

1. Internal metrics/analytics review and testing (see [metrics.md][5])
2. Internal security review (performed separately)

---

[1]: https://github.com/mozilla-lockwise/lockwise-android
[2]: https://testrail.stage.mozaws.net/index.php?/suites/view/3060&group_by=cases:section_id&group_order=asc
[3]: https://docs.google.com/document/d/1FfyD7A0qB-WGT2dx3pA5CzS764DVQyfXLs9RVbEpw0s/edit#heading=h.nz3yfasvpfpu
[4]: https://github.com/mozilla-lockwise/lockwise-android/issues/202
[5]: /metrics.md 
[6]: https://github.com/mozilla-lockbox/lockbox-ios/issues/51
[7]: https://github.com/mozilla-lockwise/lockwise-android/blob/master/docs/accessibility.md
