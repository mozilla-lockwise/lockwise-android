/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_account.view.*

class AccountViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer {

    var email: String? = null
        set(value) {
            field = value
            containerView.email.text = value
        }
    var disconnectSummary: String? = null
        set(value) {
            field = value
            containerView.disconnectSummary.text = value
        }
    var buttonTitle: String? = null
        set(value) {
            field = value
            containerView.button.text = value
        }
    var buttonSummary: String? = null
        set(value) {
            field = value
            containerView.buttonSummary.text = value
        }
}