package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import mozilla.lockbox.flux.Dispatcher

open class ClipboardStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = ClipboardStore()
    }

    private lateinit var clipboardManager: ClipboardManager

    fun apply(ctx: Context) {
        clipboardManager = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    fun clipboardCopy(label: String, str: String) {

        val clip = ClipData.newPlainText(label, str)
        clipboardManager.primaryClip = clip
    }
}
