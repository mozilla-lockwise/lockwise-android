/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *
 */

package mozilla.lockbox.support

import android.content.Context
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asMaybe
import mozilla.appservices.logins.ServerPassword
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.lockbox.log
import mozilla.lockbox.store.ContextStore
import java.net.URI
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class PublicSuffixSupport(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ContextStore {
    companion object {
        val shared by lazy { PublicSuffixSupport() }
    }

    private lateinit var psl: PublicSuffixList

    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, e ->
            log.error(
                message = "unexpected error in PublicSuffixList usage",
                throwable = e
            )
        }
    private val coroutineContext: CoroutineContext
        get() = dispatcher + exceptionHandler

    override fun injectContext(context: Context) {
        psl = PublicSuffixList(context)
    }

    fun fromWebDomain(domain: String?): Observable<PublicSuffix> {
        val fullDomain = domain ?: ""
        return psl.getPublicSuffixPlusOne(fullDomain)
            .asMaybe(coroutineContext)
            .toSingle("")
            .map { PublicSuffix(it, fullDomain) }
            .toObservable()
    }

    fun fromOrigin(origin: String?): Observable<PublicSuffix> {
        val domain = try {
            URI.create(origin).host
        } catch (e: Exception) {
            ""
        }
        return fromWebDomain(domain)
    }
    fun fromPackageName(pkg: String?): Observable<PublicSuffix> {
        val domain = (pkg ?: "")
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
    fun matches(expected: PublicSuffix): Boolean =
        this.topDomain.equals(expected.topDomain, true)
}

@ExperimentalCoroutinesApi
private fun asyncDomain(pslSupport: PublicSuffixSupport, webDomain: String?, packageName: String) =
    // resolve the (webDomain || packageName) to a 1+publicsuffix =
    when (webDomain) {
        null, "" -> pslSupport.fromPackageName(packageName)
        else -> pslSupport.fromWebDomain(webDomain)
    }

private data class FillablePassword(
    val domain: PublicSuffix,
    val entry: ServerPassword
)

@ExperimentalCoroutinesApi
fun Observable<List<ServerPassword>>.filter(pslSupport: PublicSuffixSupport, webDomain: String?, packageName: String): Observable<List<ServerPassword>> {
    val passwords = this.switchMap { serverPasswords ->
        val parsedPasswords = serverPasswords
            .map { serverPwd ->
                pslSupport.fromOrigin(serverPwd.hostname)
                    .map { FillablePassword(it, serverPwd) }
            }
        val zipper: (Array<Any>) -> List<FillablePassword> = { array ->
            array.filter { it is FillablePassword }
                .map { it as FillablePassword }
        }

        when (serverPasswords.size) {
            0 -> Observable.just(emptyList())
            else -> Observable.zipIterable(
                parsedPasswords,
                zipper,
                false,
                serverPasswords.size
            )
        }
    }

    val expectedDomain = asyncDomain(pslSupport, webDomain, packageName)

    return Observables.combineLatest(expectedDomain, passwords)
        .map { latest ->
            val expected = latest.first
            latest.second
                .filter { fillable ->
                    fillable.domain.isNotEmpty() && fillable.domain.matches(expected)
                }
                .map { fillable -> fillable.entry }
        }
}