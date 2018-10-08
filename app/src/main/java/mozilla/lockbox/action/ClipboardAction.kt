package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class ClipboardAction : Action {
    data class Clip(val label: String, val str: String) : ClipboardAction()
}