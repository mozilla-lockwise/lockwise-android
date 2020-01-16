/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import mozilla.appservices.logins.ServerPassword

open class ServerPasswordTestHelper {

    private val username = "dogs@dogs.com"
    val item1 = ServerPassword(
        "fdsfda",
        "https://www.mozilla.org",
        username,
        "woof",
        timesUsed = 0,
        timeCreated = 0L,
        timeLastUsed = 1L,
        timePasswordChanged = 0L
    )

    val item2 = ServerPassword(
        "ghfdhg",
        "https://www.cats.org",
        username,
        "meow",
        timesUsed = 0,
        timeCreated = 0L,
        timeLastUsed = 2L,
        timePasswordChanged = 0L
    )
    val item3 = ServerPassword(
        "ioupiouiuy",
        "www.dogs.org",
        username = "",
        password = "baaaaa",
        timesUsed = 0,
        timeCreated = 0L,
        timeLastUsed = 3L,
        timePasswordChanged = 0L
    )
}
