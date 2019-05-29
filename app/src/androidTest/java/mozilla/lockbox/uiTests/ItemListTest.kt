package mozilla.lockbox.uiTests

import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.view.RootActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
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