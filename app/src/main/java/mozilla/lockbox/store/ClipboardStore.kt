package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class ClipboardStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    internal val compositeDisposable = CompositeDisposable()
    companion object {
        val shared = ClipboardStore()
    }

    private lateinit var clipboardManager: ClipboardManager

    init {
        dispatcher.register
                .filterByType(ClipboardAction::class.java)
                .subscribe {
                    // unpack the action, including adding new Clips to the Clipboard.
                    when (it) {
                        is ClipboardAction.Clip -> {
                            addToClipboard(it.label, it.str)
                        }
                    }
                }
                .addTo(compositeDisposable)
    }

    fun apply(manager: ClipboardManager) {
        clipboardManager = manager
    }

    fun addToClipboard(label: String, str: String) {

        val clip = ClipData.newPlainText(label, str)
        clipboardManager.primaryClip = clip
    }
}
