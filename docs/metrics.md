# Firefox Lockbox for Android Metrics Plan

_Last Updated: Sept 26, 2018_

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Analysis](#analysis)
- [Collection](#collection)
- [List of Proposed Events](#list-of-proposed-events)
- [Adjust SDK](#adjust-sdk)
- [References](#references)

<!-- /TOC -->

This is the metrics collection plan for the Lockbox android app. It documents all events that are planned to be collected through telemetry. It will be updated periodically to reflect all new and planned data collection. A similar document for the lockbox for iOS app can be found [here](https://github.com/mozilla-lockbox/lockbox-ios/blob/master/docs/metrics.md).

## Analysis

Data collection is done solely for the purpose of product development, improvement and maintenance.

We will analyze the data described in this doc *primarily* with the purpose of (dis)confirming the following hypothesis:

`If Firefox users have access to their browser-saved passwords on their mobile device but outside of the mobile browser, then they will use those passwords to log into both websites (via a browser; Firefox or otherwise) and stand-alone apps. We will know this to be true when the most frequent actions taken in the app are revealing, copying, or autofilling of credentials.`

In service to validating the above hypothesis, we plan on answering these specific questions, given the data we plan to collect (see [List of Proposed Events](#list-of-proposed-events)):

*Note that when we refer to retrieval of "credentials", we mean access to usernames, passwords, or both*

* Are users using Lockbox to retrieve credentials?
	* For different intervals of time (e.g. day, week, month), what is:
		* The average rate with which a user retrieves a credential or reveals a password
		* The distribution of above rates across all users
* How often do users access Lockbox credentials via autofill, versus directly through the credential list provided by the app?
* Once downloaded, do users continue to use the app? (i.e., how well are they retained?)
	* We will count a user as retained in a given time interval if they perform one of the following actions:
		* Display the credential list
		* Tap a credential in the credential list
		* Copy a credential to the clipboard
		* Reveal a password
		* Autofill a credential stored in Lockbox into another app
		* Tap the URI associated with a credential (to open it in an app or browser)
	* Since they can be performed automatically, we will **not** count a user as retained if they *only* perform the following actions (in absence of any in the list above):
		* Unlock their credentials
		* Sync their credentials from the Firefox desktop browser
* Does requiring a Firefox Account constitute a roadblock to adoption?
	* What proportion of new Lockbox users are pre-existing Firefox Account users?
	* What proportion of users start the Account sign-in process but never complete it?
* Does adoption of Lockbox lead to adoption of Firefox Mobile browsers (e.g. Focus)?
	* Do users set the default browser in Lockbox to be a Firefox browser?

In addition to answering the above questions that directly concern actions in the app, we will also be analyzing telemetry emitted from the password manager that exists in the the Firefox desktop browser. These analyses will primarily examine whether users of Lockbox start active curation of their credentials in the desktop browser (Lockbox users will not be able to edit credentials directly from the app).

## Collection

Data will be collected using this library:

https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/README.md

We plan to submit two ping types, both of which are implemented by the component above.

First is the [core ping](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/src/main/java/org/mozilla/telemetry/ping/TelemetryCorePingBuilder.java), which contains information about the android version, architecture, etc of the device lockbox has been installed on:

https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html

The second is the [event ping](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/src/main/java/org/mozilla/telemetry/ping/TelemetryEventPingBuilder.java) which allows us to record event telemetry:

https://github.com/mozilla-mobile/focus-android/wiki/Event-Tracking-with-Mozilla%27s-Telemetry-Service

TODO: link to the lockbox for android source code at the point where the pings and event values are defined (when that code exists).

See [this](https://github.com/mozilla-mobile/focus-android/blob/master/app/src/main/java/org/mozilla/focus/telemetry/TelemetryWrapper.kt) for the kotlin source code that Firefox Focus uses to define its telemetry events.

Every event must contain `category`, `method` and `object` fields, and may optionally contain `value` and `extra` fields as well.

Events related to specific credentials should have an opaque `item_id` in the extra field where possible.


Finally, the `appName` metadata sent with each ping should always be 'Lockbox'.

See here for more information on event schemas:

https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html#public-js-api

## List of Proposed Events

1. When the app starts up:
	* `category`: action
	* `method`: startup
	* `object`: app
	* `value`: null
	* `extras`: null

2. When locking/unlocking the app:
	* `category`: action
	* `method`: lock, unlock
	* `object`: app
	* `value`: pin, biometrics
	* `extras`: null

3. Events that fire during the setup process:
	* `category`: action
	* `method`: show
	* `object`: login_welcome, login_fxa, login_learn_more, biometrics_setup, autofill_setup
	* `value`: null
	* `extras`: null

4. When the main item list is shown to the user:
	* `category`: action
	* `method`: show
	* `object`: entryList
	* `value`: null
	* `extras`: null

5. When a user shows the details of an item in the entry list:
	* `category`: action
	* `method`: show
	* `object`: entryDetail
	* `value`: null
	* `extras`: ["itemid" : itemid]

6. When a user taps one of the copy buttons available after being shown entry details:
	* `category`: action
	* `method`: tap
	* `object`: entryCopyUsernameButton, entryCopyPasswordButton
	* `value`: null
	* `extras`: ["itemid" : itemid]

7. When a user shows details from an item, is the password shown?:
	* `category`: action
	* `method`: tap
	* `object`: reveal_password
	* `value`: true or false, whether the pw is displayed
	* `extras`: null

8. When one of the settings pages is shown to the user:
	* `category`: action
	* `method`: show
	* `object`: settings_list, settings_autolock, settings_preferred_browser, settings_account, settings_faq, settings_provide_feedback, settings_autofill
	* `value`: null
	* `extras`: null

9. When a user changes something on the settings page:
	* `category`: action
	* `method`: settingsChanged
	* `object`: settings_biometric_login, settings_autolock_time, settings_reset, settings_visual_lock, settings_preferred_browser, settings_record_usage_data
	* `value`: whatever the value of each of the above was changed to, or null for settings_reset
	* `extras`: null

10. When the app enters the background or foreground:
	* `category`: action
	* `method`: background, foreground
	* `object`: app
	* `value`: null
	* `extras`: null

11. Events related to autofill:
	* `category`: action
	* `method`: autofill_locked, autofill_unlocked, login_selected, autofill_clear
	* `object`: autofill
	* `value`: null
	* `extras`: null

## Adjust SDK

The app also includes a version of the [adjust sdk](https://github.com/adjust/android_sdk). Mozilla uses this software to keep track of the number of installations of the lockbox app, as well the number of new Firefox Accounts registered through the app.

## References

[Library used to collect and send telemetry on android](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/README.md)

[Description of the "Core" ping](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html)

[Description of the "Focus Event" Ping](https://github.com/mozilla-mobile/focus-android/wiki/Event-Tracking-with-Mozilla%27s-Telemetry-Service)

[Description of Event Schemas in General](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html#public-js-api)
