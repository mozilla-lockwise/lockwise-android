package mozilla.lockbox.uiTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.robots.autofillOnboardingScreen
import mozilla.lockbox.robots.fingerprintOnboardingScreen
import mozilla.lockbox.robots.fxaLogin
import mozilla.lockbox.robots.onboardingConfirmationScreen
import mozilla.lockbox.robots.disconnectDisclaimer
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.RootActivity
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
open class OnboardingTest {

    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Before
    fun setUp() {
        navigator.gotoAccountSetting()
        disconnectDisclaimer {
            tapDisconnect()
            acceptDisconnect()
        }
    }

    @Test
    fun testFxALogin() {
        // There is a check that the view is correct
        navigator.gotoFxALogin()
    }

    @Test
    fun testOnboardingConfirmation() {
        navigator.gotoOnboardingConfirmation()
        onboardingConfirmationScreen { clickFinish() }
    }

    @Ignore("589-UItests-update (#590)")
    @Test
    fun fingerprintSkipButtonNavigatesToItemList() {
        navigator.gotoFingerprintOnboarding()
        fingerprintOnboardingScreen { tapSkip() }
        navigator.checkAtItemList()
    }

    @Ignore("589-UItests-update (#590)")
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

    @Ignore("589-UItests-update (#590)")
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
