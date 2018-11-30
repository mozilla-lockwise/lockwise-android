package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

open class FaqAction : Action {
    data class Redirect(val url: String) : FaqAction()
}