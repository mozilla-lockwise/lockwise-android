/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Handler
import android.util.Log
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

    private val clipDataClearDelay = 60000L
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

    fun apply(manager: ClipboardManager) {
        clipboardManager = manager
    }

    private fun addToClipboard(label: String, str: String) {
        val clip = ClipData.newPlainText(label, str)
        clipboardManager.primaryClip = clip
        timedReplaceDirty("", str)
    }

    private fun timedReplaceDirty(dirty: String, clean: String, delay: Long = clipDataClearDelay){
        Handler().postDelayed({
            replaceDirty(dirty, clean)
        }, delay)
    }

    fun replaceDirty(dirty: String, clean: String) {
        val clipData = clipboardManager.primaryClip.getItemAt(0)
        if (clipData.text == clean) {
            clipboardManager.primaryClip = ClipData.newPlainText("", dirty)
        }
    }
}
