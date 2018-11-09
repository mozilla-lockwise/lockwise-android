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
    Foreground(TelemetryEventMethod.foreground, TelemetryEventObject.app),
    Background(TelemetryEventMethod.background, TelemetryEventObject.app),
    Startup(TelemetryEventMethod.startup, TelemetryEventObject.app),
    // TODO: Add a TelemetryEventMethod for upgrading
    Upgrade(TelemetryEventMethod.startup, TelemetryEventObject.app),
    UserReset(TelemetryEventMethod.tap, TelemetryEventObject.settings_reset)
}
