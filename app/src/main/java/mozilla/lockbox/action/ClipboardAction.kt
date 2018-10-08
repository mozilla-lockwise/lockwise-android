package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class ClipboardAction : Action {
    data class CopyUsername(val username: String) : ClipboardAction()
    data class CopyPassword(val password: String) : ClipboardAction()
}