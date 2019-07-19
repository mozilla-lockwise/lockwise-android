/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

enum class LifecycleAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    /**
     * Emitted when an activity owned by the app comes on screen.
     */
    Foreground(TelemetryEventMethod.foreground, TelemetryEventObject.app),
    /**
     * Emitted when an activity owned by the app is no longer on screen. Important to note: when a system dialog (e.g.
     * the PIN guard) is used, a Background and Foreground events are emitted.
     */
    Background(TelemetryEventMethod.background, TelemetryEventObject.app),

    /**
     * Emitted when the autofill service is connected. This is the beginning of an autofill session.
     */
    AutofillStart(TelemetryEventMethod.foreground, TelemetryEventObject.autofill),

    /**
     * Emitted when the autofill service is disconnected. This is not always the end of the autofill session, as the
     * service is disconnected once authentication has started.
     */
    AutofillEnd(TelemetryEventMethod.background, TelemetryEventObject.autofill),

    Startup(TelemetryEventMethod.startup, TelemetryEventObject.app),
    // TODO: Add a TelemetryEventMethod for upgrading
    Upgrade(TelemetryEventMethod.startup, TelemetryEventObject.app),
    UserReset(TelemetryEventMethod.tap, TelemetryEventObject.settings_reset),
    UseTestData(TelemetryEventMethod.tap, TelemetryEventObject.settings_reset) // only used in Debug.
}
