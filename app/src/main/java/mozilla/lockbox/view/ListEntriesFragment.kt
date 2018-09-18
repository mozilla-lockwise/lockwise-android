/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_entries_fragment.view.*
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

class ListEntriesFragment : Fragment() {
    private lateinit var appDrawer: DrawerLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState:
            Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.list_entries_fragment, container, false)

        appDrawer = view.appDrawer
        val toolbar = view.toolbar
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_menu, null)
        toolbar.title = getString(R.string.app_name)
        toolbar.setNavigationOnClickListener {
            // TODO: replace with a dispatched action
            appDrawer.openDrawer(GravityCompat.START)
        }
        view.navView.setNavigationItemSelectedListener { _ /* menuItem */ ->
            // TODO: replace with a dispatched action
            appDrawer.closeDrawers()

            true
        }

        return view
    }
}
