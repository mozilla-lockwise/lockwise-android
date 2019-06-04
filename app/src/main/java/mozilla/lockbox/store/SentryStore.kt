/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant

class SentryStore(
    dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {
    private val compositeDisposable = CompositeDisposable()

    companion object {
        val shared = SentryStore()
    }

    init {
        dispatcher.register
            .filterByType(SentryAction::class.java)
            .map { it.error }
            .subscribe { throwable ->
                Sentry.getStoredClient().sendException(throwable)
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        val sentryDsn = Constant.Sentry.dsn
        Sentry.init(sentryDsn, AndroidSentryClientFactory(context))
    }
}