/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import mozilla.lockbox.R
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_onboarding_security_dialog.view.*
import mozilla.lockbox.presenter.OnboardingSecurityDialogPresenter
import mozilla.lockbox.presenter.OnboardingSecurityDialogView

class OnboardingSecurityDialogFragment : DialogFragment(), OnboardingSecurityDialogView {

    private val compositeDisposable = CompositeDisposable()
    private var isEnablingDismissed: Boolean = true

    private val _onSkip = PublishSubject.create<Unit>()
    override val onSkipClick: Observable<Unit> get() = _onSkip

    private val _onSetUp = PublishSubject.create<Unit>()
    override val onSetUpNowClick: Observable<Unit> get() = _onSetUp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.NoTitleDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = OnboardingSecurityDialogPresenter(this)
        return inflater.inflate(R.layout.fragment_onboarding_security_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.skipButton.clicks()
            .subscribe {
                dismiss()
            }
            .addTo(compositeDisposable)

        view.setUpNowButton.clicks()
            .subscribe {
                dismiss()
                isEnablingDismissed = false
            }
            .addTo(compositeDisposable)
    }

    // see FingerprintAuthDialogFragment
    override fun onDestroyView() {
        if (isEnablingDismissed) {
            _onSkip.onNext(Unit)
        } else {
            _onSetUp.onNext(Unit)
        }
        compositeDisposable.clear()

        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }
}