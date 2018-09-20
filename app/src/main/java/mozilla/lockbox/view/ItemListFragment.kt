/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.view.*
import com.jakewharton.rxbinding2.support.design.widget.itemSelections
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_item_list.*
import kotlinx.android.synthetic.main.fragment_item_list.view.toolbar
import kotlinx.android.synthetic.main.fragment_item_list.view.navView
import kotlinx.android.synthetic.main.fragment_item_list.view.appDrawer
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.presenter.ListEntriesPresenter
import mozilla.lockbox.presenter.ListEntriesProtocol

class ItemListFragment : CommonFragment(), ListEntriesProtocol {
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState:
        Bundle?
    ): View? {
        presenter = ListEntriesPresenter(this)
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        view.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_menu, null)
        view.toolbar.title = getString(R.string.app_name)
        view.toolbar.navigationClicks().subscribe { view.appDrawer.openDrawer(GravityCompat.START) }
                .addTo(compositeDisposable)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawerItemSelections
                .doOnDispose {
                    TODO("not working")
                    view.appDrawer.closeDrawer(view.navView, false)
                }
                .subscribe()
                .addTo(compositeDisposable)
        presenter.onViewReady()
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        presenter.onDestroy()
        super.onDestroyView()
    }

    // Protocol implementations
    override val drawerItemSelections: Observable<MenuItem>
            get() = view!!.navView.itemSelections()
}
