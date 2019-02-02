package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class OnboardingAction : Action {
    object OnDismiss : OnboardingAction()
}