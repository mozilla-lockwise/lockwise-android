/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.appservices.logins.MemoryLoginsStorage
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.components.service.sync.logins.AsyncLoginsStorageAdapter
import java.util.Date
import java.util.Random
import java.util.UUID

// Fixed-Data Implementation
class FixedDataStoreSupport(
    values: List<ServerPassword>? = null
) : DataStoreSupport {
    var size = getRandomInRange(40, 50)
    private val logins = MemoryLoginsStorage(
        values ?: List(size) { createDummyItem(it) } + listOf(createDummyIPItem())
    )

    override var encryptionKey: String = "shh-keep-it-secret"

    override var syncConfig: SyncUnlockInfo? =
        SyncUnlockInfo(
            kid = "fake-kid",
            fxaAccessToken = "fake-at",
            syncKey = "fake-key",
            tokenserverURL = "https://oauth.example.com/token")

    override fun createLoginsStorage(): AsyncLoginsStorage = AsyncLoginsStorageAdapter(logins)

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

internal fun createDummyIPItem(): ServerPassword {
    val random = Random()
    val id = UUID.randomUUID().toString()
    val pwd = createRandomPassword()
    val host = "http://10.250.7.80"
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
        "https://accounts.",
        "https://mobile."
    )
    val hostnameChoices = listOf(
        "google.com",
        "twitter.com",
        "facebook.com",
        "tumblr.com",
        "firefox.com",
        "yahoo.co.uk",
        "baidu.cn",
        "yandex.ru"
    )
    val prng = Random()
    val protocol = protocolChoices[prng.nextInt(protocolChoices.size)]
    val hostname = hostnameChoices[prng.nextInt(hostnameChoices.size)]

    return protocol + hostname
}

internal fun createUserId(): String? {
    val pos = getRandomInRange(1, 27)
    return if (pos % 7 == 0 || pos % 27 == 0) {
        null
    } else {
        "fakeTester$pos"
    }
}

internal fun getRandomInRange(lower: Int, upper: Int): Int {
    return (lower..upper).shuffled(random = Random()).first()
}