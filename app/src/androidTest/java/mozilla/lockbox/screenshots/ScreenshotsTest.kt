package mozilla.lockbox.screenshots

import android.os.SystemClock.sleep
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo

import kotlinx.coroutines.ExperimentalCoroutinesApi

import mozilla.lockbox.view.RootActivity
import mozilla.lockbox.R

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import br.com.concretesolutions.kappuccino.custom.recyclerView.RecyclerViewInteractions.recyclerView

import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)

open class ScreenshotsTest {

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Test
    fun testCompleteOnboarding() {

        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        onView(withId(R.id.buttonGetStarted))
                .check(matches(isDisplayed()))

        Screengrab.screenshot("get-started")

        onView(withId(R.id.buttonGetStarted)).perform(click())
        Screengrab.screenshot("secure-device-screen")

        onView(withId(android.R.id.button2)).perform(click())
        onView(withId(R.id.skipFxA))
                .check(matches(isDisplayed()))
        sleep(5000)
        Screengrab.screenshot("enter-email-screen")

        onView(withId(R.id.skipFxA)).perform(click())
        onView(withId(R.id.skipButton))
                .check(matches(isDisplayed()))
        Screengrab.screenshot("autofill-onboarding-screen")

        onView(withId(R.id.skipButton)).perform(click())
        Screengrab.screenshot("allset-screen")

        onView(withId(R.id.finishButton)).perform(click())
        onView(withId(R.id.sortButton))
                .check(matches(isDisplayed()))
        Screengrab.screenshot("all-logins-screen")

        selectItem(1)
        onView(withId(R.id.inputHostname))
                .check(matches(isDisplayed()))
        Screengrab.screenshot("item-detail-screen")

        onView(withId(R.id.kebabMenu)).perform(click())
        Screengrab.screenshot("item-menu")

        onView(withText(R.string.delete)).perform(click())
        Screengrab.screenshot("item-delete-disclaimer")

        onView(withText(R.string.cancel)).perform(click())
        /* Edit Menu Not developed yet
        onView(withId(R.id.kebabMenu)).perform(click())
        onView(withText(R.string.edit)).perform(click())
        Screengrab.screenshot("item-edit-menu")
        */

        onView(withId(R.id.inputUsername)).perform(click())
        Screengrab.screenshot("username-copied-screen")

        onView(withId(R.id.inputPassword)).perform(click())
        Screengrab.screenshot("password-copied-screen")
        pressBack()

        onView(withId(R.id.sortButton)).perform(click())
        Screengrab.screenshot("sorting-options-screen")
        pressBack()
/*
        onView(withId(R.id.filterButton)).perform(click());
        Screengrab.screenshot("filter-options-screen");
        pressBack()
        pressBack()
*/
        onView(withId(R.id.appDrawer)).perform(DrawerActions.open())
        Screengrab.screenshot("app-menu-screen")

        onView(withId(R.id.lockNow)).perform(click())
        Screengrab.screenshot("lock-now-screen")
        pressBack()

        tapSettings()
        onView(withId(R.id.settingList))
                .check(matches(isDisplayed()))
        Screengrab.screenshot("settings-menu-screen")
        pressBack()

        tapAccountSetting()
        onView(withId(R.id.disconnectButton))
                .check(matches(isDisplayed()))
        Screengrab.screenshot("settings-account-screen")

        onView(withId(R.id.disconnectButton)).perform(click())
        Screengrab.screenshot("disconnect-account-screen")
        pressBack()
    }
}

fun selectItem(position: Int = 0) = clickListItem(R.id.entriesView, position)

fun openMenu(): ViewInteraction {
    val drawer2 = onView(withId(R.id.appDrawer))
    drawer2.perform(DrawerActions.open())
    return onView(withId(R.id.navView))!!
}

private fun menuOption(item: Int) = openMenu().perform(navigateTo(item))

fun tapSettings() = menuOption(R.id.setting_menu_item)
fun tapAccountSetting() = menuOption(R.id.account_setting_menu_item)

fun clickListItem(listRes: Int, position: Int) {
    recyclerView(listRes) {
        atPosition(position) { click() }
    }
}
