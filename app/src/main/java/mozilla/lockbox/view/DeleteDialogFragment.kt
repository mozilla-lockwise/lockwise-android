/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.presenter.DeleteDialogPresenter
import mozilla.lockbox.presenter.DeleteDialogView
import mozilla.lockbox.presenter.FingerprintDialogPresenter
import mozilla.lockbox.presenter.FingerprintDialogView
import mozilla.lockbox.support.Constant

class DeleteDialogFragment : DialogFragment(), DeleteDialogView {
    private val compositeDisposable = CompositeDisposable()
    private val _dismiss = PublishSubject.create<Unit>()
//    override val onDismiss: Observable<Unit> get() = _dismiss
    private var titleId: Int? = null
    private var subtitleId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NoTitleDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = DeleteDialogPresenter(this)
        return inflater.inflate(R.layout.fragment_delete_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appLabel = getString(R.string.app_label)
        titleId?.let {
            val title = getString(it)
            if (title.contains("%1\$s")) {
                view.dialogTitle.text = getString(it, appLabel)
            } else {
                view.dialogTitle.text = title
            }
        }
        subtitleId?.let {
            val subtitle = getString(it)
            if (subtitle.contains("%1\$s")) {
                view.dialogSubtitle.text = getString(it, appLabel)
            } else {
                view.dialogSubtitle.text = subtitle
            }
            view.dialogSubtitle.visibility = View.VISIBLE
        } ?: run {
            view.dialogSubtitle.visibility = View.GONE
        }

        view.cancel.clicks()
            .subscribe { dismiss() }
            .addTo(compositeDisposable)
    }

    override fun setupDialog(@StringRes titleId: Int, @StringRes subtitleId: Int?) {
        this.titleId = titleId
        this.subtitleId = subtitleId
    }

}