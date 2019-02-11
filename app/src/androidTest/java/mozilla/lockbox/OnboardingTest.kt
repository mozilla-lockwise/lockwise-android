package mozilla.lockbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import mozilla.lockbox.robots.autofillOnboardingScreen
import mozilla.lockbox.robots.fingerprintOnboardingScreen
import mozilla.lockbox.view.RootActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class OnboardingTest {

    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Test
    fun fingerprintSkipButtonNavigatesToItemList() {
        navigator.gotoFingerprintOnboarding()
        fingerprintOnboardingScreen { tapSkip() }
        navigator.checkAtItemList()
    }

    @Test
    fun fingerprintSuccessNavigatesToItemList() {
        navigator.gotoFingerprintOnboarding()
        fingerprintOnboardingScreen {
            exists()
            // touchFingerprint() doesn't work, see FingerprintDialogTest.kt
            tapSkip()
        }
        navigator.checkAtItemList()
    }

    @Test
    fun autofillSkipButtonNavigatesToItemList() {
        navigator.gotoAutofillOnboarding()
        autofillOnboardingScreen { tapSkip() }
        navigator.checkAtItemList()
    }

    @Test
    fun autofillGoToSettingsNavigatesToSystemSettings() {
        navigator.gotoAutofillOnboarding()
        autofillOnboardingScreen {
            exists()
            touchGoToSettings()
        }
    }
}
