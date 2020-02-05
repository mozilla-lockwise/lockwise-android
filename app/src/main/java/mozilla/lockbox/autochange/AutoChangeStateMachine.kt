/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import io.reactivex.subjects.Subject
import mozilla.components.service.sync.logins.ServerPassword
import mozilla.lockbox.autochange.AutoChangeState as State

class AutoChangeStateMachine(private val nextState: Subject<State>) {

    fun nextCommand(currentState: State, message: FromWebView, originalItem: ServerPassword, updatedItem: ServerPassword): ToWebView? =
        when (currentState) {
            is State.HomepageFinding -> currentState.next(message)
            is State.HomepageFound -> currentState.next(message)

            is State.LoginFinding -> currentState.next(message)
            is State.LoginFound -> currentState.next(message, originalItem)
            is State.LoginSuccessful -> currentState.next(message)

            is State.PasswordChangeFinding -> currentState.next(message)
            is State.PasswordChangeFound -> currentState.next(message, originalItem, updatedItem)
            is State.PasswordChangeSuccessful -> currentState.next(message)

            is State.LoggingOut -> currentState.next(message, updatedItem)
            is State.LoggedOut -> currentState.next()

            is State.Error -> currentState.next()
        } ?: (message as? JS2KotlinMessage.Fail)?.let {
            nextState.onNext(State.Error(it.reason))
            null
        }

    private fun State.HomepageFinding.next(event: FromWebView?): ToWebView? =
        when (event) {
            // we go directly to the Homepage via a loadUrl, so
            // immediately start looking for the login page.
            is JS2KotlinMessage.TapEnd -> {
                nextState.onNext(State.HomepageFound)
                null
            }
            else -> null
        }

    private fun State.HomepageFound.next(event: FromWebView?): ToWebView? =
        when (event) {
            is JS2KotlinMessage.TapEnd -> {
                nextState.onNext(State.LoginFinding)
                null
            }
            else -> null
        }

    private fun State.LoginFinding.next(event: FromWebView?): ToWebView? =
        when (event) {
            is JS2KotlinMessage.TapEnd -> {
                Kotlin2JSMessage.Advance("login")
            }

            is JS2KotlinMessage.Arrived -> {
                nextState.onNext(State.LoginFound)
                null
            }

            is JS2KotlinMessage.Fail -> {
                nextState.onNext(State.Error(event.reason))
                null
            }
            else -> null
        }

    private fun State.LoginFound.next(
        event: FromWebView,
        originalItem: ServerPassword
    ): ToWebView? =
        when (event) {
            is JS2KotlinMessage.Arrived -> {
                Kotlin2JSMessage.ExamineDestination("login")
            }

            is JS2KotlinMessage.DestinationInformation -> {
                Kotlin2JSMessage.FillForm("login", hashMapOf(
                    "username" to (originalItem.username ?: ""),
                    "password" to originalItem.password
                ))
            }

            is JS2KotlinMessage.TapEnd -> {
                // form fill ends in a TapEnd.
                // so we should check that we logged in ok.
                Kotlin2JSMessage.ConfirmSuccess("login", true)
            }

            is JS2KotlinMessage.FormFillSuccess -> {
                nextState.onNext(State.LoginSuccessful)
                null
            }

            is JS2KotlinMessage.Fail -> {
                nextState.onNext(State.Error(event.reason))
                null
            }

            else -> null
        }

    private fun State.LoginSuccessful.next(event: FromWebView?): ToWebView? =
        when (event) {
            is JS2KotlinMessage.FormFillSuccess -> {
                nextState.onNext(State.PasswordChangeFinding)
                null
            }

            else -> null
        }

    private fun State.PasswordChangeFinding.next(event: FromWebView?): ToWebView? =
        when (event) {
            is JS2KotlinMessage.FormFillSuccess, // this is the successful login
            is JS2KotlinMessage.TapEnd -> {
                Kotlin2JSMessage.Advance("passwordChange")
            }

            is JS2KotlinMessage.Arrived -> {
                nextState.onNext(State.PasswordChangeFound)
                null
            }

            is JS2KotlinMessage.Fail -> {
                nextState.onNext(State.Error(event.reason))
                null
            }

            else -> null
        }

    private fun State.PasswordChangeFound.next(
        event: FromWebView,
        originalItem: ServerPassword,
        updatedItem: ServerPassword
    ): ToWebView? =
        when (event) {
            is JS2KotlinMessage.Arrived -> {
                Kotlin2JSMessage.ExamineDestination("passwordChange")
            }

            is JS2KotlinMessage.DestinationInformation -> {
                Kotlin2JSMessage.FillForm("passwordChange",
                    hashMapOf(
                        "password" to originalItem.password,
                        "newPassword" to updatedItem.password
                    )
                )
            }

            is JS2KotlinMessage.TapEnd -> {
                // form fill ends in a TapEnd.
                // so we should check that we logged in ok.
                Kotlin2JSMessage.ConfirmSuccess("passwordChange", false)
            }

            is JS2KotlinMessage.FormFillSuccess -> {
                nextState.onNext(State.PasswordChangeSuccessful)
                null
            }

            is JS2KotlinMessage.Fail -> {
                nextState.onNext(State.Error(event.reason))
                null
            }

            else -> null
        }

    private fun State.PasswordChangeSuccessful.next(event: FromWebView?): ToWebView? =
        when (event) {
            is JS2KotlinMessage.FormFillSuccess -> {
                nextState.onNext(State.LoggingOut)
                null
            }
            else -> null
        }

    private fun State.LoggingOut.next(event: FromWebView?, updatedItem: ServerPassword): ToWebView? =
        when (event) {
            is JS2KotlinMessage.FormFillSuccess -> { // this is successful password change.
                NavigationMessage.LoadURL(updatedItem.hostname)
            }
            is JS2KotlinMessage.TapEnd -> {
                Kotlin2JSMessage.Advance("logout")
            }

            is JS2KotlinMessage.Arrived -> {
                nextState.onNext(State.LoggedOut)
                null
            }

            is JS2KotlinMessage.Fail -> {
                nextState.onNext(State.Error(event.reason))
                null
            }

            else -> null
        }

    private fun State.LoggedOut.next(): ToWebView? =
        NavigationMessage.Done

    private fun State.Error.next(): ToWebView? =
        NavigationMessage.Done


}
