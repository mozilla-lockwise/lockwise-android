/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.MemoryLoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncUnlockInfo
import java.util.Date
import java.util.Random
import java.util.UUID
import kotlin.coroutines.experimental.buildSequence

// Fixed-Data Implementation
class FixedDataStoreSupport(
    values: List<ServerPassword>? = null
) : DataStoreSupport {
    var size = getRandomInRange(10, 20)
    private val logins = MemoryLoginsStorage(
        values ?: List(size) { createDummyItem(it) })

    override var encryptionKey: String = "shh-keep-it-secret"

    override var syncConfig: SyncUnlockInfo =
        SyncUnlockInfo(
            kid = "fake-kid",
            fxaAccessToken = "fake-at",
            syncKey = "fake-key",
            tokenserverURL = "https://oauth.example.com/token")

    override fun createLoginsStorage(): LoginsStorage = logins

    companion object {
        val shared = FixedDataStoreSupport()
    }
}

/**
 * Creates a test ServerPassword item
 * Some functionality inspired by FxA 'upload_fake_passwords.py'
 * https://gist.github.com/rfk/916d9ca684f862b1c1030c685a5a4d19
 */
internal fun createDummyItem(idx: Int): ServerPassword {
    val random = Random()
    val id = UUID.randomUUID().toString()
    val pwd = createRandomPassword()
    val host = createHostname()
    val user = createUserId()
    val created = Date().time
    val used = Date(created - 86400000).time
    val changed = Date(used - 86400000).time

    return ServerPassword(
        id = id,
        hostname = host,
        username = user,
        password = pwd,
        formSubmitURL = host,
        timeCreated = created,
        timeLastUsed = used,
        timePasswordChanged = changed,
        timesUsed = random.nextInt(100) + 1
    )
}

internal fun createRandomPassword(): String {
    return UUID.randomUUID().toString().substring(0..20)
}

internal fun createHostname(): String {
    val protocolChoices = listOf(
        "https://www.",
        "http://www.",
        "https://",
        "https://accounts."
    )
    val domainChoices = listOf(
        ".com",
        ".net",
        ".org",
        ".co.uk"
    )
    var protocol = protocolChoices[Random().nextInt(protocolChoices.size)]
    var domain = domainChoices[Random().nextInt(domainChoices.size)]

    var hostnameLength = getRandomInRange(8, 20)
    val hostname =
        buildSequence {
            val r = Random(); while (true) yield(r.nextInt(26))
        }
            .take(hostnameLength)
            .map {
                (it + 97).toChar()
            }
            .joinToString("")

    return protocol + hostname + domain
}

internal fun createUserId(): String {
    var pos = getRandomInRange(1, 27)
    return "fakeTester$pos"
}

internal fun getRandomInRange(lower: Int, upper: Int): Int {
    return (lower..upper).shuffled(random = Random()).first()
}