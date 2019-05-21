/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_onboarding_confirmation.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.OnboardingConfirmationPresenter
import mozilla.lockbox.presenter.OnboardingConfirmationView

class OnboardingConfirmationFragment : Fragment(), OnboardingConfirmationView {
    private val _linkClicked = PublishSubject.create<Unit>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = OnboardingConfirmationPresenter(this)

        return inflater.inflate(R.layout.fragment_onboarding_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val linkColor = resources.getColor(R.color.violet_70, null)
        val securityLink = getString(R.string.security_link)
        val securityText = String.format(getString(R.string.security_description), securityLink)
        val spannableSecurityText = SpannableString(securityText)

        val securityLinkStart = securityText.indexOf(securityLink)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                _linkClicked.onNext(Unit)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = linkColor
                ds.isUnderlineText = false
            }
        }

        spannableSecurityText.setSpan(
            clickableSpan,
            securityLinkStart,
            securityLinkStart + securityLink.length,
            SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableSecurityText.setSpan(
            StyleSpan(Typeface.BOLD),
            securityLinkStart,
            securityLinkStart + securityLink.length,
            SPAN_EXCLUSIVE_EXCLUSIVE
        )

        view.encryptionText.text = spannableSecurityText
        view.encryptionText.movementMethod = LinkMovementMethod.getInstance()
        val appName = getString(R.string.app_name)
        view.encryptionText.contentDescription = getString(R.string.security_content_description, appName)
    }

    override val finishClicks: Observable<Unit>
        get() = view!!.finishButton.clicks()

    override val encryptionClicks: Observable<Unit>
        get() = _linkClicked
}