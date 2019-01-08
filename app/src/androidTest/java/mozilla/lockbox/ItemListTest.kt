package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.view.RootActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class ItemListTest {
    private val navigator = Navigator()

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Test
    fun openSortMenuOnTap() {
        navigator.gotoItemList(false)
        itemList { tapSortButton() }
        itemList { sortMenuIsDisplayed() }
    }

    @Test
    fun spinnerTitleChangesIfOtherItemIsSelected() {
        navigator.gotoItemList(false)
        itemList { tapSortButton() }
        itemList { selectFirstItemInSortMenu() }
        itemList { spinnerDisplaysFirstItemSelection() }
        itemList { tapSortButton() }
        itemList { selectSecondItemInSortMenu() }
        itemList { spinnerDisplaysSecondItemSelection() }
    }

    @Test
    fun spinnerTitleDoesNotChangeIfSameItemIsSelected() {
        navigator.gotoItemList(false)
        itemList { tapSortButton() }
        itemList { selectFirstItemInSortMenu() }
        itemList { spinnerDisplaysFirstItemSelection() }
        itemList { tapSortButton() }
        itemList { selectFirstItemInSortMenu() }
        itemList { spinnerDisplaysFirstItemSelection() }
    }

    @Test
    fun testPullToRefresh() {
        navigator.gotoItemList(false)
        itemList { pullToRefresh() }
        navigator.checkAtItemList()
    }
}