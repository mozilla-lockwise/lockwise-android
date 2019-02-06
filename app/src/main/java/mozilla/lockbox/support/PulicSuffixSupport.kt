/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *
 */

package mozilla.lockbox.support

import io.reactivex.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asMaybe
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.lockbox.log
import java.net.IDN
import java.net.URI
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class PublicSuffixSupport(
    private val psl: PublicSuffixList,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, e ->
            log.error(
                message = "unexpected error in PublicSuffixList usage",
                throwable = e
            )
        }
    private val coroutineContext: CoroutineContext
        get() = dispatcher + exceptionHandler

    fun fromWebDomain(domain: String?): Observable<PublicSuffix> {
        val fullDomain = domain ?: ""
        return psl.getPublicSuffixPlusOne(fullDomain)
            .asMaybe(coroutineContext)
            .toSingle("")
            .flatMapObservable { Observable.just(PublicSuffix(it, fullDomain)) }
    }

    fun fromOrigin(origin: String): Observable<PublicSuffix> {
        val domain = URI.create(origin).host
        return fromWebDomain(domain)
    }
    fun fromPackageName(pkg: String): Observable<PublicSuffix> {
        val domain = pkg
            .split('.')
            .asReversed()
            .joinToString(".")
        return fromWebDomain(domain)
    }
}

data class PublicSuffix(
    val topDomain: String,
    val fullDomain: String
) {
    fun isEmpty(): Boolean = topDomain.isEmpty()
    fun isNotEmpty(): Boolean = topDomain.isNotEmpty()
    fun matches(expected: PublicSuffix): Boolean
        = expected.topDomain.equals(expected.topDomain, true)
}