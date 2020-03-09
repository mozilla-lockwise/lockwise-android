/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import com.github.satoshun.reactivex.webkit.data.OnPageFinished
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.support.asOptional

data class SearchNode(
    val url: String,
    val linkIndex: Int
)

class AutoChangeSearch(val destination: AutoChangeDestination) {

}

fun WebViewWrapper.search(destination: AutoChangeDestination): Observable<JS2KotlinMessage> {
    val compositeDisposable = CompositeDisposable()

    val returnValue = PublishSubject.create<JS2KotlinMessage>()

    val webView = this
    val pageFinished = webView.rawEvents().ofType(OnPageFinished::class.java)
    val causeOfNavigation = ReplaySubject.createWithSize<ToWebView>(1)

    val navigationEvents = pageFinished
        .flatMap { causeOfNavigation.take(1) }
        .map { cause ->
            when (cause) {
                is NavigationMessage.GoBack -> NavigationEvent.GoneBack
                is NavigationMessage.LoadURL -> NavigationEvent.ReloadedUrl(cause.url)
                is Kotlin2JSMessage.NavigateTo -> NavigationEvent.NavigatedTo(webView.url)
                else -> null
            }.asOptional()
        }
        .filterNotNull()

    val js2KotlinMessages = webView.events()
        .map {
            val hasInfo: FromWebView? = when (it) {
                is JS2KotlinMessage.NodeInformation -> JS2KotlinMessage.HasInfo(it)
                else -> null
            }
            hasInfo.asOptional()
        }
        .filterNotNull()

    val agenda = mutableListOf<SearchNode>()

    val searchMessages = js2KotlinMessages.mergeWith(navigationEvents)

    searchMessages
        .map { message ->
            when (message) {
                is NavigationEvent.ReloadedUrl -> {
                    NavigationMessage.AgendaPop()
                }

                is NavigationEvent.NavigatedTo -> {
                    Kotlin2JSMessage.GetInfo(destination.name)
                }

                is JS2KotlinMessage.HasInfo -> {
                    val info = message.info
                    when {
                        info.isDestination -> NavigationMessage.Done
                        info.numLinks == 0 -> NavigationMessage.GoBack

                        else -> {
                            val url = webView.url
                            (0 until info.numLinks)
                                .map { SearchNode(url, it) }
                                .forEach { agenda.add(it) }

                            NavigationMessage.AgendaPop()
                        }
                    }
                }

                is NavigationEvent.GoneBack -> {
                    if (agenda.lastOrNull()?.url != webView.url && webView.canGoBack()) {
                        NavigationMessage.GoBack
                    } else {
                        NavigationMessage.AgendaPop(reset = true)
                    }
                }

                else -> NavigationMessage.Fail(AutoChangeError.BUG)
            }
        }
        .map { command ->
            when (command) {
                is NavigationMessage.AgendaPop -> {
                    if (agenda.isEmpty()) {
                        returnValue.onNext(JS2KotlinMessage.Fail("", destination.notFound))
                        returnValue.onComplete()
                        null
                    } else if (command.reset && agenda.last().url != webView.url) {
                        // If there's an error it's going to be here.
                        NavigationMessage.LoadURL(
                            agenda.last().url
                        )
                    } else {
                        val node = agenda.removeAt(agenda.lastIndex)
                        Kotlin2JSMessage.NavigateTo(destination.jsName, node.linkIndex)
                    }
                }

                is NavigationMessage.Done -> {
                    returnValue.onNext(JS2KotlinMessage.Arrived(destination.jsName))
                    returnValue.onComplete()
                    null
                }

                is NavigationMessage.Fail -> {
                    returnValue.onNext(JS2KotlinMessage.Fail("", command.error))
                    returnValue.onComplete()
                    null
                }

                else -> command
            }.asOptional()
        }
        .filterNotNull()
        .observeOn(mainThread())
        .subscribe { toWebView ->
            if (toWebView is Kotlin2JSMessage.GetInfo) {
                webView.evalJSCommand(toWebView)
            } else {
                causeOfNavigation.onNext(toWebView).also {
                    when (toWebView) {
                        is NavigationMessage.LoadURL -> webView.loadUrl(toWebView.url)
                        is NavigationMessage.GoBack -> webView.goBack()

                        is Kotlin2JSMessage.NavigateTo -> webView.evalJSCommand(toWebView)
                    }
                }
            }
        }
        .addTo(compositeDisposable)

    return returnValue
        .doOnComplete {
            compositeDisposable.clear()
        }
}
