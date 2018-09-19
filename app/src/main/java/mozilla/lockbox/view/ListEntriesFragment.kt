/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.design.widget.itemSelections
import io.reactivex.Observable
import kotlinx.android.synthetic.main.list_entries_fragment.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.ListEntriesPresenter
import mozilla.lockbox.presenter.ListEntriesProtocol

class ListEntriesFragment : Fragment(), ListEntriesProtocol {
    private val presenter = ListEntriesPresenter(this)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState:
            Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.list_entries_fragment, container, false)

        view.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_menu, null)
        view.toolbar.title = getString(R.string.app_name)
        view.toolbar.setNavigationOnClickListener {
            // TODO: replace with a dispatched action
            view.appDrawer.openDrawer(GravityCompat.START)
        }

        view.navView.setNavigationItemSelectedListener { _ /* menuItem */ ->
            // TODO: replace with a dispatched action
            view.appDrawer.closeDrawers()

            true
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    // Protocol implementations
    override val drawerItemClicks: Observable<MenuItem>
            get() = view!!.navView.itemSelections()
}
