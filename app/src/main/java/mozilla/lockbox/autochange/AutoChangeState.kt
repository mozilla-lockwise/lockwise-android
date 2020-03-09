/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import androidx.annotation.StringRes
import mozilla.lockbox.R

enum class AutoChangeError(
    @StringRes val message: Int
) {
    BAD_CREDENTIALS(R.string.autochange_error_bad_credentials),
    BLOCKED_BY_CAPTCHA(R.string.autochange_error_captcha),
    BLOCKED_BY_2FA(R.string.autochange_error_2fa),
    BLOCKED_BY_TOS(R.string.autochange_error_tos_blocking),
    NOT_FOUND_LOGIN(R.string.autochange_error_login_not_found),
    NOT_FOUND_LOGOUT(R.string.autochange_error_logout_not_found),
    NOT_FOUND_PASSWORD_CHANGE(R.string.autochange_error_password_change_not_found),
    PASSWORD_CHANGE_NOT_ACCEPTED(R.string.autochange_error_password_change_not_accepted),

    BUG(R.string.autochange_error_internal_error)
}

enum class AutoChangeDestination(
    val jsName: String,
    @StringRes val finding: Int,
    @StringRes val found: Int,
    val notFound: AutoChangeError
) {
    LOGIN("login",
        R.string.autochange_login_finding,
        R.string.autochange_login_found,
        AutoChangeError.NOT_FOUND_LOGIN
    ),

    PASSWORD_CHANGE("passwordChange",
        R.string.autochange_password_change_finding,
        R.string.autochange_password_change_found,
        AutoChangeError.NOT_FOUND_PASSWORD_CHANGE
    ),

    LOGOUT("logout",
        R.string.autochange_log_out_finding,
        R.string.autochange_log_out_success,
        AutoChangeError.NOT_FOUND_PASSWORD_CHANGE
    )
}

sealed class AutoChangeState(
    @StringRes val message: Int
) {
    open class Finding(val destination: AutoChangeDestination) : AutoChangeState(destination.finding)
    open class Found(val destination: AutoChangeDestination) : AutoChangeState(destination.found)

    object HomepageFinding : AutoChangeState(
        R.string.autochange_homepage_finding
    )
    object HomepageFound : AutoChangeState(
        R.string.autochange_homepage_found
    )
    object LoginFinding : Finding(
        AutoChangeDestination.LOGIN
    )
    object LoginFound : Found(
        AutoChangeDestination.LOGIN
    )
    object LoginSuccessful : AutoChangeState(
        R.string.autochange_login_success
    )
    object PasswordChangeFinding : Finding(
        AutoChangeDestination.PASSWORD_CHANGE
    )
    object PasswordChangeFound : Found(
        AutoChangeDestination.PASSWORD_CHANGE
    )
    object PasswordChangeSuccessful : AutoChangeState(
        R.string.autochange_password_change_success
    )
    object LoggingOut : Finding(
        AutoChangeDestination.LOGOUT
    )
    object LoggedOut : Found(
        AutoChangeDestination.LOGOUT
    )

    data class Error(val error: AutoChangeError) : AutoChangeState(error.message)

    companion object {
        val happyPathSteps = listOf(
            HomepageFinding,
            HomepageFound,
            LoginFinding,
            LoginFound,
            LoginSuccessful,
            PasswordChangeFinding,
            PasswordChangeFound,
            PasswordChangeSuccessful,
            LoggingOut,
            LoggedOut
        )
    }
}
