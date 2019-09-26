/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.LockboxBroadcastReceiver
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.ClipboardSupport
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.SystemTimingSupport
import mozilla.lockbox.support.DeviceSystemTimingSupport

open class ClipboardStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    private val timerSupport: SystemTimingSupport = DeviceSystemTimingSupport()
) : ContextStore {
    internal val compositeDisposable = CompositeDisposable()

    companion object {
        @SuppressLint("StaticFieldLeak")
        val shared = ClipboardStore()
    }

    private val defaultClipboardTimeout = 60000L
    private lateinit var clipboardSupport: ClipboardSupport
    private lateinit var alarmManager: AlarmManager
    private lateinit var context: Context

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
        this.context = context
        this.clipboardSupport = ClipboardSupport(context)
        this.alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun addToClipboard(label: String, string: String) {
        clipboardSupport.paste(label, string)
        timedReplaceDirty(string)
    }

    private fun timedReplaceDirty(dirty: String, delay: Long = defaultClipboardTimeout) {
        val triggerAt = delay + this.timerSupport.systemTimeElapsed
        val intent = Intent(context, LockboxBroadcastReceiver::class.java)
        intent.action = Constant.Key.clearClipboardIntent
        intent.putExtra(Constant.Key.clipboardDirtyExtra, dirty)
        val scheduled = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAt, scheduled)
    }
}
