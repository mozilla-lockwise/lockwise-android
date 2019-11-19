# Firefox Lockwise for Android Metrics Plan

_Last Updated: Feb 4, 2019_

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Analysis](#analysis)
- [Collection](#collection-legacy)
- [List of Implemented Events](#list-of-implemented-events)
- [Mozilla Glean SDK](#mozilla-glean-sdk)
- [Adjust SDK](#adjust-sdk)
- [References](#references)

<!-- /TOC -->

This is the metrics collection plan for the Lockwise Android app. It documents all events that are currently collected through telemetry. It will be updated periodically to reflect all new and planned data collection. A similar document for the Lockwise for iOS app can be found [here](https://github.com/mozilla-lockbox/lockbox-ios/blob/master/docs/metrics.md).

## Analysis

Data collection is done solely for the purpose of product development, improvement and maintenance.

We analyze the data described in this doc *primarily* with the purpose of (dis)confirming the following hypothesis:

`If Firefox users have access to their browser-saved passwords on their mobile device but outside of the mobile browser, then they will use those passwords to log into both websites (via a browser; Firefox or otherwise) and stand-alone apps. We will know this to be true when the most frequent actions taken in the app are revealing, copying, or autofilling of credentials.`

In service to validating the above hypothesis, we plan on answering these specific questions, given the data we collect (see [List of Implemented Events](#list-of-implemented-events)):

*Note that when we refer to retrieval of "credentials", we mean access to usernames, passwords, or both*

* Are users using Lockwise to retrieve credentials?
	* For different intervals of time (e.g. day, week, month), what is:
		* The average rate with which a user retrieves a credential or reveals a password
		* The distribution of above rates across all users
* How often do users access Lockwise credentials via autofill, versus directly through the credential list provided by the app?
* Once downloaded, do users continue to use the app? (i.e., how well are they retained?)
	* We will count a user as retained in a given time interval if they perform one of the following actions:
		* Display the credential list
		* Tap a credential in the credential list
		* Copy a credential to the clipboard
		* Reveal a password
		* Autofill a credential stored in Lockwise into another app
		* Tap the URI associated with a credential (to open it in an app or browser)
	* Since they can be performed automatically, we will **not** count a user as retained if they *only* perform the following actions (in absence of any in the list above):
		* Unlock their credentials
		* Sync their credentials from the Firefox desktop browser
* Does requiring a Firefox Account constitute a roadblock to adoption?
	* What proportion of new Lockwise users are pre-existing Firefox Account users?
	* What proportion of users start the Account sign-in process but never complete it?
* Does adoption of Lockwise lead to adoption of Firefox Mobile browsers (e.g. Focus)?
	* Do users set the default browser in Lockwise to be a Firefox browser?

In addition to answering the above questions that directly concern actions in the app, we will also analyze telemetry emitted from the password manager that exists in the the Firefox desktop browser. These analyses will primarily examine whether users of Lockwise start active curation of their credentials in the desktop browser (Lockwise users will not be able to edit credentials directly from the app).

## Collection (legacy)

*Note: There is currently a new Mozilla mobile telemetry SDK under development, however it will not ship prior to the Lockwise for Android app. Once the new SDK ships we will evaluate whether or not to tear out the old implementation and replace it with the new SDK.*

Data is collected using this library:

https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/README.md

We submit two ping types, both of which are implemented by the component above.

First is the [core ping](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/src/main/java/org/mozilla/telemetry/ping/TelemetryCorePingBuilder.java), which contains information about the Android version, architecture, etc of the device Lockwise has been installed on:

https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html

The second is the [event ping](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/src/main/java/org/mozilla/telemetry/ping/TelemetryEventPingBuilder.java) which allows us to record event telemetry:

https://github.com/mozilla-mobile/focus-android/wiki/Event-Tracking-with-Mozilla%27s-Telemetry-Service

See [this](https://github.com/mozilla-mobile/focus-android/blob/master/app/src/main/java/org/mozilla/focus/telemetry/TelemetryWrapper.kt) for the kotlin source code that Firefox Focus uses to define its telemetry events.

Every event must contain `category`, `method` and `object` fields, and may optionally contain `value` and `extra` fields as well.

Finally, the `appName` metadata sent with each ping should always be `Lockbox`.

See here for more information on event schemas:

https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html#public-js-api

## List of Implemented Events

1. When the app starts up:
	* `category`: action
	* `method`: startup
	* `object`: app
	* `value`: null
	* `extras`: null

2. When locking/unlocking, accessing the datastore (sync credentials, updating the entry list, etc.):
	* `category`: action
	* `method`: lock, unlock, reset, sync_start, sync_end, sync_timeout, sync_error, list_update, list_update_error, update_credentials, touch
	* `object`: datastore
	* `value`: null
	* `extras`: null

3. Events that fire during the setup process:
	* `category`: action
	* `method`: show
	* `object`: login_welcome, login_fxa, login_onboarding_confirmation, login_learn_more
	* `value`: null
	* `extras`: null

4. When the main item list is shown to the user:
	* `category`: action
	* `method`: show
	* `object`: entry_list
	* `value`: null
	* `extras`: null

5. When a user shows the details of an item in the entry list:
	* `category`: action
	* `method`: show
	* `object`: entry_detail
	* `value`: null
	* `extras`: null

6. When a user taps one of the copy buttons available after being shown entry details:
	* `category`: action
	* `method`: tap
	* `object`: entry_copy_username_button, entry_copy_password_button
	* `value`: null
	* `extras`: null

7. When a user taps to reveal a password:
	* `category`: action
	* `method`: tap
	* `object`: reveal_password
	* `value`: null
	* `extras`: null

8. When one of the settings pages is shown to the user:
	* `category`: action
	* `method`: show
	* `object`: settings_list, settings_autolock, settings_account, settings_faq, settings_provide_feedback
	* `value`: null
	* `extras`: null

9. When a user changes something on the settings page:
	* `category`: action
	* `method`: setting_changed
	* `object`: settings_autolock_time, settings_reset, settings_fingerprint, settings_fingerprint_pending_auth, settings_item_list_order,
	* `value`: whatever the value of each of the above was changed to, for example `60` in the case of `settings_autolock_time`
	* `extras`: null

10. When the app enters the background or foreground:
	* `category`: action
	* `method`: background, foreground
	* `object`: app
	* `value`: null
	* `extras`: null

11. When a user taps on the search box to filter the credential list:
	* `category`: action
	* `method`: tap
	* `object`: filter
	* `value`: null
	* `extras`: null

12. When a user taps on a link to open a webpage in their browser:
	* `category`: action
	* `method`: tap
	* `object`: open_in_browser
	* `value`: null
	* `extras`: null

13. When a user interacts with the autofill functionality
	* `category`: action
	* `method`:
		* autofill_error (there was an error when attempting to fill a form),
		* autofill_multiple (the autofill API returned a list possible credentials to fill a form),
		* autofill_single (a credential was selected from the list of possible credentials to fill a form),
		* autofill_filter (the user filtered a returned list of autofill credentials),
		* autofill_locked (the datastore was locked when a form triggered the autofill API),
		* autofill_cancel (the user cancelled form autofill),
		* background (when the autofill service is backgrounded, doesn't necessarily involve user-facing UX),
		* foreground (always when the autofill service is foregrounded, doesn't necessarily involve user-facing UX)
	* `object`: autofill
	* `value`: null
	* `extras`: null

## Mozilla Glean SDK

Lockwise for Android uses the [Glean SDK](https://mozilla.github.io/glean/book/index.html) to collect telemetry. The Glean SDK provides a handful of [pings and metrics out of the box](https://mozilla.github.io/glean/book/user/pings/index.html). The data review for using the Glean SDK is available at [this link](TODO).

Lockwise for Android also uses the following Glean-enabled components of [Android Components](https://github.com/mozilla-mobile/android-components/), which are sending telemetry:

|Name|Description|Collected metrics|Data review|
|---|---|---|---|
|[Firefox accounts](https://github.com/mozilla-mobile/android-components/tree/master/components/service/firefox-accounts)|A library for integrating with Firefox Accounts.| [docs](https://github.com/mozilla-mobile/android-components/blob/master/components/support/sync-telemetry/docs/metrics.md)| [review](TODO) |

## Adjust SDK

The app also includes a version of the [adjust SDK](https://github.com/adjust/android_sdk). Mozilla uses this software to keep track of the number of installations of the Lockwise app, as well the number of new Firefox Accounts registered through the app.

## References

[Glean SDK repository, used to collect and send telemetry](https://github.com/mozilla/glean/)

[Legacy library used to collect and send telemetry on Android](https://github.com/mozilla-mobile/android-components/blob/master/components/service/telemetry/README.md)

[Description of the "Core" ping](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html)

[Description of the "Focus Event" Ping](https://github.com/mozilla-mobile/focus-android/wiki/Event-Tracking-with-Mozilla%27s-Telemetry-Service)

[Description of Event Schemas in General](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html#public-js-api)
