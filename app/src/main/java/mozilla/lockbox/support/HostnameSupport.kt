/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.net.Uri
import com.google.common.net.InternetDomainName
import java.net.URI

class HostnameSupport(idn: InternetDomainName?) {
    companion object {
        /**
         * Creates a `HostnameSupport` from the given Android/Java package name.
         */
        fun fromPackageName(pkgName: String): HostnameSupport {
            val domainName = pkgName.split(".").asReversed().joinToString(".")
            return fromDomainName(domainName)
        }

        /**
         * Creates a `HostnameSupport` from the given URI.
         * This parses the uri string ("https://example.com/login") to extract just the host portion
         * ("example.com").
         */
        fun fromUri(uriStr: String): HostnameSupport {
            val domainName = URI.create(uriStr).host
            return fromDomainName(domainName)
        }

        fun fromDomainName(domainName: String?): HostnameSupport {
            val idn: InternetDomainName? = when (domainName) {
                null, "" -> null
                else -> InternetDomainName.from(domainName)
            }
            return HostnameSupport(idn)
        }
    }

    val fullName: String = idn?.toString() ?: ""
    val topName: String

    init {
        topName = when {
            idn == null -> ""
            !idn.isUnderPublicSuffix -> {
                val parts = idn.parts()
                val first = Math.max(parts.size - 2, 0)
                val last = parts.size
                parts.subList(first, last).joinToString(".")
            }
            else -> idn.topPrivateDomain().toString()
        }
    }

    fun matches(test: HostnameSupport): Boolean {
        return test.fullName == this.topName ||
            test.fullName.endsWith(".${this.topName}")
    }
}