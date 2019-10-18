/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import io.reactivex.Observable
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.model.ItemViewModel

fun ServerPassword.toViewModel(): ItemViewModel {
    val username = this.username ?: ""
    val hostname = titleFromHostname(this.hostname)
    return ItemViewModel(hostname, username, this.id)
}

fun ServerPassword.toDetailViewModel(): ItemDetailViewModel {
    val username = this.username ?: ""
    return ItemDetailViewModel(id, titleFromHostname(hostname), hostname, username, password)
}

private fun titleFromHostname(hostname: String): String {
    return hostname
            .replace(Regex("^http://"), "")
            .replace(Regex("^https://"), "")
            .replace(Regex("^www\\d*\\."), "")
}

fun Observable<List<ServerPassword>>.filter(
    username: String? = null,
    password: String? = null,
    hostname: String? = null,
    httpRealm: String? = null,
    formSubmitURL: String? = null
): Observable<List<ServerPassword>> =
    this.map { items ->
        items.filter(
            username = username,
            password = password,
            hostname = hostname,
            httpRealm = httpRealm,
            formSubmitURL = formSubmitURL
        )
    }

fun Collection<ServerPassword>.filter(
    username: String? = null,
    password: String? = null,
    hostname: String? = null,
    httpRealm: String? = null,
    formSubmitURL: String? = null
) =
    this.filter {
        it.matches(
            username = username,
            password = password,
            hostname = hostname,
            httpRealm = httpRealm,
            formSubmitURL = formSubmitURL
        )
    }

fun ServerPassword.matches(
    username: String? = null,
    password: String? = null,
    hostname: String? = null,
    httpRealm: String? = null,
    formSubmitURL: String? = null
): Boolean {
    fun queryMatch(query: String?, candidate: String?) =
        when (query) {
            null -> true
            "" -> true
            candidate -> true
            else -> false
        }

    return when {
        queryMatch(username, this.username).not() -> false
        queryMatch(hostname, this.hostname).not() -> false
        queryMatch(httpRealm, this.httpRealm).not() -> false
        queryMatch(password, this.password).not() -> false
        queryMatch(formSubmitURL, this.formSubmitURL).not() -> false
        else -> true
    }
}
