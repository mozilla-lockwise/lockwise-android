package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R

class FingerprintOnboardingRobot : BaseTestRobot {
    override fun exists() = VisibilityAssertions.displayed { id(R.id.iconFingerprint) }

    fun touchFingerprint(finger: String = "1") {
        Runtime.getRuntime().exec("adb -e emu finger $finger")
    }

    fun tapSkip() = ClickActions.click { id(R.id.skipButton) }
}
fun fingerprintOnboardingScreen(f: FingerprintOnboardingRobot.() -> Unit) = FingerprintOnboardingRobot().apply(f)
