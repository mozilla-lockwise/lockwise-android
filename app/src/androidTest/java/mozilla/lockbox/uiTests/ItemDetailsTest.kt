package mozilla.lockbox.uiTests

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.robots.itemDetail
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.robots.kebabMenu
import mozilla.lockbox.robots.deleteCredentialDisclaimer
import mozilla.lockbox.robots.editCredential
import mozilla.lockbox.robots.editCredentialDisclaimer
import mozilla.lockbox.view.RootActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

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
        // navigator.back()
    }

    @Test
    fun showToastWhenUsernameCopied() {
        navigator.gotoItemDetail()
        itemDetail { tapCopyUsername() }
        itemDetail { toastIsDisplayed(R.string.toast_username_copied, activityRule) }
        // navigator.back()
    }

    @Test
    fun showToastWhenPassCopied() {
        navigator.gotoItemDetail()
        itemDetail { tapCopyPass() }
        itemDetail { toastIsDisplayed(R.string.toast_password_copied, activityRule) }
    }

    @Test
    fun deleteItem() {
        navigator.gotoItemDetailKebabMenu()
        kebabMenu { tapDeleteButton() }
        // First tap on Cancel delete credential
        deleteCredentialDisclaimer { tapCancelButton() }
        itemDetail { exists() }
        // Now delete the credential
        itemDetail { tapKebabMenu() }
        kebabMenu { tapDeleteButton() }
        deleteCredentialDisclaimer { tapDeleteButton() }
        // Check that Item List is shown after removing the credential
        itemList { exists() }
    }

    @Test
    fun editItem() {
        navigator.gotoItemDetailKebabMenu()
        kebabMenu { tapEditButton() }
        // Edit entry Hostname and Username
        editCredential {
            exists()
            tapOnUserName()
            editUserName("UsernameChanged")
            saveChanges() }
        pressBack()
        // Check that changes in entry are saved
        itemList {
            exists()
            editedCredentialUsernameExists("UsernameChanged")
            openCredential("UsernameChanged")
        }
        // navigator.back()
    }

    @Test
    fun cancelEditCredential() {
        // Tap on Cancel edit credential
        navigator.gotoItemDetailKebabMenu()
        kebabMenu { tapEditButton() }
        editCredential {
            closeEditChanges() }
        navigator.gotoItemDetailKebabMenu()
        // itemDetail { tapKebabMenu() }
        kebabMenu { tapEditButton() }
        editCredential {
            editUserName("foo")
            closeEditChanges()
        }
        editCredentialDisclaimer { tapCancelButton() }
        // User is taken to ItemDetail View
        // Now Tap on Discard edit credential
        itemDetail { exists() }

        editCredential { closeEditChanges() }
        editCredentialDisclaimer { tapDiscardButton() }
        // User is taken to ItemList View
        // No changes are applied
        itemDetail { exists() }
        // navigator.back()
    }

    @Test
    fun editItemInvalidTextFieldInput() {
        // itemDetail { tapKebabMenu() }
        navigator.gotoItemDetailKebabMenu()
        kebabMenu { tapEditButton() }
        editCredential {
            editPassword("")
            assertErrorEmptyPassord()
            editPassword("foo bar baz")
            sleep(1000)
            noErrorEmptyPassword()
        }
        // navigator.back()
    }
}