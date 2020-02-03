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
    PASSWORD_CHANGE_NOT_ACCEPTED(R.string.autochange_error_password_change_not_accepted)
}

enum class AutoChangeDestination(
    val jsName: String,
    val finding: AutoChangeState,
    val found: AutoChangeState,
    val notFound: AutoChangeError
) {
    LOGIN("login",
        AutoChangeState.LoginFinding,
        AutoChangeState.LoginFound,
        AutoChangeError.NOT_FOUND_LOGIN
    ),

    PASSWORD_CHANGE("passwordChange",
        AutoChangeState.PasswordChangeFinding,
        AutoChangeState.PasswordChangeFound,
        AutoChangeError.NOT_FOUND_PASSWORD_CHANGE
    ),

    LOGOUT("logout",
        AutoChangeState.LoggingOut,
        AutoChangeState.LoggedOut,
        AutoChangeError.NOT_FOUND_PASSWORD_CHANGE
    )
}

sealed class AutoChangeState(
    @StringRes val message: Int
) {
    object HomepageFinding : AutoChangeState(
        R.string.autochange_homepage_finding
    )
    object HomepageFound : AutoChangeState(
        R.string.autochange_homepage_found
    )
    object LoginFinding : AutoChangeState(
        R.string.autochange_login_finding
    )
    object LoginFound : AutoChangeState(
        R.string.autochange_login_found
    )
    object LoginSuccessful : AutoChangeState(
        R.string.autochange_login_success
    )
    object PasswordChangeFinding : AutoChangeState(
        R.string.autochange_password_change_finding
    )
    object PasswordChangeFound : AutoChangeState(
        R.string.autochange_password_change_found
    )
    object PasswordChangeSuccessful : AutoChangeState(
        R.string.autochange_password_change_success
    )
    object LoggingOut : AutoChangeState(
        R.string.autochange_log_out_finding
    )
    object LoggedOut : AutoChangeState(
        R.string.autochange_log_out_success
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
