package mozilla.lockbox.store

import android.content.ClipboardManager
import android.content.Context
import junit.framework.Assert.assertTrue
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ClipboardStoreTest : DisposingTest() {

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: ClipboardStore

    @Before
    fun setUp() {
        dispatcher = Dispatcher()

        this.dispatcher = dispatcher
        subject = ClipboardStore(dispatcher)
    }

    @Test
    fun testInit() {
        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
    }

    @Test
    fun testClipboard() {
        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        subject.clipboardCopy("label", "my_test_string")

        val clipboardManager:ClipboardManager = RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        assertTrue(clipboardManager.primaryClip.getItemAt(0).text.equals("my_test_string"))
    }
}