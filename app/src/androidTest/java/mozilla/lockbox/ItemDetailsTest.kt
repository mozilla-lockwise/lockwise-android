package mozilla.lockbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.robots.itemDetail
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.view.RootActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
open class ItemDetailsTest {
    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Test
    fun itemDetailsAreDisplayed() {
        navigator.gotoItemList(false)
        itemList { selectItem() }
        itemDetail { exists() }
    }

    @Test
    fun showToastWhenUsernameCopied() {
        navigator.gotoItemDetail()
        itemDetail { tapCopyUsername() }
        itemDetail { toastIsDisplayed(R.string.toast_username_copied, activityRule) }
    }

    @Test
    fun showToastWhenPassCopied() {
        navigator.gotoItemDetail()
        itemDetail { tapCopyPass() }
        itemDetail { toastIsDisplayed(R.string.toast_password_copied, activityRule) }
    }
}