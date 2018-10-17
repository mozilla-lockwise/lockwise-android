/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.jakewharton.rxbinding2.support.design.widget.itemSelections
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListAdapter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.ItemListPresenter
import mozilla.lockbox.presenter.ItemListView

class ItemListFragment : CommonFragment(), ItemListView {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = ItemListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = ItemListPresenter(this)
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)
        val navController = requireActivity().findNavController(R.id.fragment_nav_host)

        setupToolbar(view.toolbar, view.appDrawer)
        setupNavigationView(navController, view.navView)
        setupListView(view.entriesView)

        return view
    }

    private fun setupNavigationView(navController: NavController, navView: NavigationView) {
        navView.setupWithNavController(navController)
    }

    private fun setupListView(listView: RecyclerView) {
        val context = requireContext()
        val layoutManager = LinearLayoutManager(context)
        val decoration = DividerItemDecoration(context, layoutManager.orientation)
        context.getDrawable(R.drawable.inset_divider)?.let {
            decoration.setDrawable(it)
            listView.addItemDecoration(decoration)
        }
        listView.layoutManager = layoutManager
        listView.adapter = adapter
    }

    private fun setupToolbar(toolbar: Toolbar, drawerLayout: DrawerLayout) {
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_menu, null)
        toolbar.navigationClicks().subscribe { drawerLayout.openDrawer(GravityCompat.START) }
                .addTo(compositeDisposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    // Protocol implementations
    override val filterClicks: Observable<Unit>
        get() = view!!.filterButton.clicks()

    override val itemSelection: Observable<ItemViewModel>
        get() = adapter.clicks()

    override val menuItemSelections: Observable<Int>
        get() {
            val navView = view!!.navView
            val drawerLayout = view!!.appDrawer
            return navView.itemSelections()
                .doOnNext {
                    drawerLayout.closeDrawer(navView)
                }
                .map { it.itemId }
        }

    override fun updateItems(itemList: List<ItemViewModel>) {
        adapter.updateItems(itemList)
    }
}
