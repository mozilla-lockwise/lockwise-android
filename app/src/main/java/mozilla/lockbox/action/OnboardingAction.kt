/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment

sealed class OnboardingAction : Action {
    object ShowOnboarding : OnboardingAction()
    object OnDismiss : OnboardingAction()
    data class OnAuthentication(val authCallback: OnboardingFingerprintAuthFragment.AuthCallback) : OnboardingAction()
}