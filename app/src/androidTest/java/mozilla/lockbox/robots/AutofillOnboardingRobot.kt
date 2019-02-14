package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R

class AutofillOnboardingRobot : BaseTestRobot {
    override fun exists() = VisibilityAssertions.displayed { id(R.id.iconAutofill) }

    fun touchGoToSettings() = ClickActions.click { id(R.id.goToSettings) }

    fun tapSkip() = ClickActions.click { id(R.id.skipButton) }
}
fun autofillOnboardingScreen(f: AutofillOnboardingRobot.() -> Unit) = AutofillOnboardingRobot().apply(f)
