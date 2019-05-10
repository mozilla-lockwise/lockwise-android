package mozilla.lockbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.robots.autofillOnboardingScreen
import mozilla.lockbox.robots.fingerprintOnboardingScreen
import mozilla.lockbox.robots.fxaLogin
import mozilla.lockbox.robots.onboardingConfirmationScreen
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.RootActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
open class OnboardingTest {

    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @BeforeTest
    fun setUp() {
        navigator.disconnectAccount()
    }

    @Test
    fun testOnboardingConfirmation() {
        navigator.gotoOnboardingConfirmation()
        onboardingConfirmationScreen { clickFinish() }
    }

    @Ignore
    @Test
    fun fingerprintSkipButtonNavigatesToItemList() {
        navigator.gotoFingerprintOnboarding()
        fingerprintOnboardingScreen { tapSkip() }
        navigator.checkAtItemList()
    }

    @Ignore
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
        navigator.checkAtOnboardingConfirmation()
        onboardingConfirmationScreen { clickFinish() }
    }

    @Ignore
    @Test
    fun autofillGoToSettingsNavigatesToSystemSettings() {
        navigator.gotoAutofillOnboarding()
        autofillOnboardingScreen {
            exists()
            touchGoToSettings()
        }
    }

    @Test
    fun fxaLoginToOnboardingToItemList() {
        navigator.gotoFxALogin()
        fxaLogin { tapPlaceholderLogin() }
        if (FingerprintStore.shared.isFingerprintAuthAvailable) {
            navigator.checkAtFingerprintOnboarding()
            fingerprintOnboardingScreen { tapSkip() }
        }
        if (SettingStore.shared.autofillAvailable) {
            navigator.checkAtAutofillOnboarding()
            autofillOnboardingScreen { tapSkip() }
        }
        navigator.checkAtOnboardingConfirmation()
        onboardingConfirmationScreen { clickFinish() }
        navigator.checkAtItemList()
    }
}
