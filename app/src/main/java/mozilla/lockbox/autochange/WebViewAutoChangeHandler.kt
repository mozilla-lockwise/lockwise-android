/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import android.content.Context
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.components.service.sync.logins.ServerPassword
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.log
import mozilla.lockbox.support.asOptional
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import mozilla.lockbox.autochange.AutoChangeState as State

typealias PasswordGenerator = (hostname: String, formInfo: FormInfo.PasswordChangeInfo?) -> String?
val uuidPasswordGenerator: PasswordGenerator = { _, _ ->
    UUID.randomUUID().toString()
}

val noopPasswordGenerator: PasswordGenerator = { _, _ -> null }

class WebViewAutoChangeHandler(
    val context: Context,
    val webView: WebView = WebView(context),
    val originalItem: ServerPassword,
    val passwordGenerator: PasswordGenerator = uuidPasswordGenerator
) : AutoChangeHandler {

    private val currentState = PublishSubject.create<State>()

    override val progress: Observable<State> = currentState
        .observeOn(mainThread())
        .doOnDispose {
            finish()
        }

    private lateinit var webViewWrapper: WebViewWrapper

    private var updatedItem: ServerPassword = originalItem

    private val secureToken = UUID.randomUUID().toString()

    private val compositeDisposable = CompositeDisposable()

    override fun invoke(): Observable<ServerPassword> {
        val returnValue = PublishSubject.create<ServerPassword>()

        webViewWrapper = createWebViewWrapper()
        val stateMachine = AutoChangeStateMachine(currentState)
        var searchMachine: AutoChangeSearch? = null


        Observables.combineLatest(currentState, webViewWrapper.events())
            .map { (state: State, message: JS2KotlinMessage) ->
                log.info("From webview: $message")

                if (state is State.PasswordChangeSuccessful) {
                    returnValue.onNext(updatedItem)
                }

                if (message is JS2KotlinMessage.DestinationInformation) {
                    val options = message.options
                    updatedItem = when (options) {
                        is FormInfo.LoginFormInfo -> updateItem(updatedItem, options)
                        is FormInfo.PasswordChangeInfo -> updateItem(updatedItem, options)
                        else -> null
                    } ?: updatedItem
                }

                when (state) {
                    is State.Finding -> {
                        if (searchMachine == null) {
                            searchMachine = AutoChangeSearch(state.destination)
                        }
                    }
                }

                stateMachine.nextCommand(
                    state,
                    message,
                    originalItem,
                    updatedItem
                ).asOptional()
            }
            .filterNotNull()
            .concatMap {
                Observable.just(it).delay(300, TimeUnit.MILLISECONDS)
            }
            .observeOn(mainThread())
            .subscribe { next ->
                when (next) {
                    is NavigationMessage.LoadURL -> {
                        webViewWrapper.loadUrl(next.url)
                    }
                    is NavigationMessage.Done -> {
                        finish()
                        returnValue.onComplete()
                    }
                    is Kotlin2JSMessage -> {
                        webViewWrapper.evalJSCommand(next)
                    }
                }
            }
            .addTo(compositeDisposable)

        if (updatedItem.hostname.startsWith("http://")) {
            updatedItem = updatedItem.hostname.let {
                updatedItem.copy(
                    hostname = it.replace(
                    "http://",
                    "https://"
                    )
                )
            }
        }

        currentState.onNext(State.HomepageFinding)
        webViewWrapper.loadUrl(updatedItem.hostname)

        return returnValue
    }

    private fun createWebViewWrapper(): WebViewWrapper {
        val js2KotlinFFI = WebViewJS2Kotlin(secureToken)
        return WebViewWrapper(
            context,
            webView,
            js2KotlinFFI,
            secureToken,
            "\$username" to (originalItem.username ?: "username")
        ).also { it.setup() }
    }

    private fun finish() {
        webViewWrapper.finish()
        currentState.onComplete()
        compositeDisposable.clear()
    }

    private fun updateItem(item: ServerPassword, formInfo: FormInfo.LoginFormInfo?) = formInfo?.let {
        item.copy(
            hostname = it.hostname ?: item.hostname,
            formSubmitURL = it.formActionOrigin ?: item.formSubmitURL
        )
    }

    private fun updateItem(item: ServerPassword, formInfo: FormInfo.PasswordChangeInfo?) =
        passwordGenerator.invoke(item.hostname, formInfo)?.let {
            item.copy(
                password = it,
                timePasswordChanged = Date().time
            )
        }
}
