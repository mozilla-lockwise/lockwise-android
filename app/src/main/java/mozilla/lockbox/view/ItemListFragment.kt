/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.design.widget.itemSelections
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.ListEntriesPresenter
import mozilla.lockbox.presenter.ListEntriesProtocol

class ItemListFragment : Fragment(), ListEntriesProtocol {
    private val compositeDisposable = CompositeDisposable()
    private lateinit var presenter: ListEntriesPresenter

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
        compositeDisposable.add(
                view.toolbar.navigationClicks().subscribe { view.appDrawer.openDrawer(GravityCompat.START) }
        )

        compositeDisposable.add(
                view.navView.itemSelections().subscribe { view.appDrawer.closeDrawers() }
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    // Protocol implementations
    override val drawerItemSelections: Observable<MenuItem>
            get() = view!!.navView.itemSelections()
}
