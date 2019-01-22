package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class NetworkAction : Action {
    object CheckConnectivity : NetworkAction()
}