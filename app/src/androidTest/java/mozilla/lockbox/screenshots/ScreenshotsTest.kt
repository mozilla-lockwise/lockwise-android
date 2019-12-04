package mozilla.lockbox.screenshots

import android.os.SystemClock.sleep
import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack

import kotlinx.coroutines.ExperimentalCoroutinesApi

import mozilla.lockbox.view.RootActivity

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import mozilla.lockbox.robots.accountSettingScreen
import mozilla.lockbox.robots.autofillOnboardingScreen
import mozilla.lockbox.robots.deleteCredentialDisclaimer
import mozilla.lockbox.robots.editCredential
import mozilla.lockbox.robots.fxaLogin
import mozilla.lockbox.robots.itemDetail
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.robots.kebabMenu
import mozilla.lockbox.robots.onboardingConfirmationScreen
import mozilla.lockbox.robots.welcome
import mozilla.lockbox.uiTests.Navigator
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)

open class ScreenshotsTest {
    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Test
    fun testAppFirstView() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        welcome {
            exists()
            Screengrab.screenshot("get-started")
            tapGetStarted()
            Screengrab.screenshot("secure-device-screen")
            tapSkipSecureYourDevice()
            // Need to wait for the FxAscreen to be shown
            sleep(5000)
            Screengrab.screenshot("enter-email-screen")
        }
        fxaLogin {
            tapPlaceholderLogin()
            Screengrab.screenshot("autofill-onboarding-screen")
        }
        autofillOnboardingScreen {
            tapSkip()
            Screengrab.screenshot("allset-screen")
        }
        onboardingConfirmationScreen {
            clickFinish()
        }
    }

    @Test
    fun testItemList() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        navigator.gotoItemList(false)
        Screengrab.screenshot("all-logins-screen")

        itemList {
            tapSortButton()
            Screengrab.screenshot("sorting-options-screen")
            pressBack()

            openMenu()
            Screengrab.screenshot("app-menu-screen")

            tapLockNow()
            Screengrab.screenshot("lock-now-screen")
            pressBack()
        }
    }

    @Test
    fun testAppSettingsMenu() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        navigator.gotoSettings()
        Screengrab.screenshot("settings-menu-screen")
        navigator.gotoAccountSetting()
        Screengrab.screenshot("settings-account-screen")
        accountSettingScreen {
            tapDisconnect()
            Screengrab.screenshot("disconnect-account-screen")
            pressBack()
        }
    }

    @Test
    fun testEditDetailView() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        navigator.gotoItemDetail(1)
        itemDetail {
            tapCopyPass()
            Screengrab.screenshot("password-copied-screen")
            tapCopyUsername()
            Screengrab.screenshot("username-copied-screen")
            tapKebabMenu()
        }
        kebabMenu {
            Screengrab.screenshot("item-menu")
            tapDeleteButton()
            Screengrab.screenshot("item-delete-disclaimer")
        }
        deleteCredentialDisclaimer {
            tapCancelButton()
        }
        itemDetail {
            tapKebabMenu()
        }
        kebabMenu {
            tapEditButton()
            sleep(1000)
            Screengrab.screenshot("item-edit-menu")
        }
        editCredential {
            editPassword("")
            sleep(1000)
            Screengrab.screenshot("error-empty-field")
            saveChanges()
        }
    }
}
