/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

class Constant {
    class App {
        companion object {
            const val KEYSTORE_LABEL = "lockbox-keystore"
        }
    }

    class FxA {
        companion object {
            const val clientID = "e7ce535d93522896"
            const val redirectUri = "https://lockbox.firefox.com/fxa/android-redirect.html"
            val scopes = arrayOf(
                "profile",
                "https://identity.mozilla.com/apps/lockbox",
                "https://identity.mozilla.com/apps/oldsync"
            )
        }
    }
}