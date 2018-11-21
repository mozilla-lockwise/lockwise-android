/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class ClipboardStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {
    internal val compositeDisposable = CompositeDisposable()
    companion object {
        val shared = ClipboardStore()
    }

    private val defaultClipboardTimeout = 60000L
    private lateinit var clipboardManager: ClipboardManager

    init {
        dispatcher.register
                .filterByType(ClipboardAction::class.java)
                .subscribe {
                    // unpack the action, including adding new Clips to the Clipboard.
                    when (it) {
                        is ClipboardAction.CopyUsername -> {
                            addToClipboard("username", it.username)
                        }
                        is ClipboardAction.CopyPassword -> {
                            addToClipboard("password", it.password)
                        }
                    }
                }
                .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        this.clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private fun addToClipboard(label: String, string: String) {
        val clip = ClipData.newPlainText(label, string)
        clipboardManager.primaryClip = clip
        timedReplaceDirty(string)
    }

    private fun timedReplaceDirty(dirty: String, clean: String = "", delay: Long = defaultClipboardTimeout) {
        Handler().postDelayed({
            replaceDirty(dirty, clean)
        }, delay)
    }

    fun replaceDirty(dirty: String, clean: String = "") {
        val clipData = clipboardManager.primaryClip.getItemAt(0)
        if (clipData.text == dirty) {
            clipboardManager.primaryClip = ClipData.newPlainText("", clean)
        }
    }
}
