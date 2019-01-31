/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import android.widget.Spinner
import com.jakewharton.rxbinding2.support.design.widget.itemSelections
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.fragment_item_list.*
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import kotlinx.android.synthetic.main.fragment_warning.view.*
import kotlinx.android.synthetic.main.nav_header.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListAdapter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.ItemListPresenter
import mozilla.lockbox.presenter.ItemListView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.Setting
import mozilla.lockbox.adapter.SortItemAdapter
import mozilla.lockbox.model.AccountViewModel
import mozilla.lockbox.support.showAndRemove

@ExperimentalCoroutinesApi
class ItemListFragment : Fragment(), ItemListView {
    private val compositeDisposable = CompositeDisposable()
    private val adapter = ItemListAdapter()
    private val errorHelper = NetworkErrorHelper()

    private lateinit var spinner: Spinner
    private var _sortItemSelection = PublishSubject.create<Setting.ItemListSort>()
    private lateinit var sortItemsAdapter: SortItemAdapter
    private var userSelection = false

    override val sortItemSelection: Observable<Setting.ItemListSort> = _sortItemSelection

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = ItemListPresenter(this)
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = requireActivity().findNavController(R.id.fragment_nav_host)

        setupToolbar(view.navToolbar, view.appDrawer)
        setupNavigationView(navController, view.navView)
        setupListView(view.entriesView)
        setupSpinner(view)
        view.refreshContainer.setColorSchemeResources(R.color.refresh_blue)
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupSpinner(view: View) {
        val sortList = ArrayList<Setting.ItemListSort>()
        sortList.add(Setting.ItemListSort.ALPHABETICALLY)
        sortList.add(Setting.ItemListSort.RECENTLY_USED)

        spinner = view.sortButton
        sortItemsAdapter = SortItemAdapter(context!!, android.R.layout.simple_spinner_item, sortList)
        spinner.adapter = sortItemsAdapter
        spinner.setPopupBackgroundResource(R.drawable.sort_menu_bg)

        // added because different events can trigger onItemSelectedListener
        spinner.setOnTouchListener { _, _ ->
            userSelection = true
            false
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userSelection) {
                    sortItemsAdapter.setSelection(position)
                    _sortItemSelection.onNext(sortMenuOptions[position])
                }
            }
        }
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
        toolbar.setNavigationContentDescription(R.string.menu_description)
        toolbar.navigationClicks().subscribe { drawerLayout.openDrawer(GravityCompat.START) }
            .addTo(compositeDisposable)
    }

    private fun scrollToTop() {
        entriesView.layoutManager?.scrollToPosition(0)
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

    override val lockNowClick: Observable<Unit>
        get() = view!!.lockNow.clicks()

    private val sortMenuOptions: Array<Setting.ItemListSort>
        get() = Setting.ItemListSort.values()

    override fun updateItems(itemList: List<ItemViewModel>) {
        adapter.updateItems(itemList)
    }

    override fun updateAccountProfile(profile: AccountViewModel) {
        val header = view!!.navView.getHeaderView(0)
        header.menuHeader.displayName.text = profile.displayEmailName ?: resources.getString(R.string.firefox_account)
        header.menuHeader.accountName.text = profile.accountName ?: resources.getString(R.string.app_name)

        var avatarUrl = profile.avatarFromURL
        if (avatarUrl.isNullOrEmpty() || avatarUrl == resources.getString(R.string.default_avatar_url)) {
            avatarUrl = null
        }

        Picasso.get()
            .load(avatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .transform(CropCircleTransformation())
            .resizeDimen(R.dimen.avatar_image_size, R.dimen.avatar_image_size)
            .centerCrop()
            .into(header.menuHeader.profileImage)
    }

    override fun updateItemListSort(sort: Setting.ItemListSort) {
        sortItemsAdapter.setSelection(sortMenuOptions.indexOf(sort))
        spinner.setSelection(sortMenuOptions.indexOf(sort), false)
        scrollToTop()
    }

    override fun loading(isLoading: Boolean) {
        if (isLoading) {
            showAndRemove(view!!.loadingView, view!!.refreshContainer)
        } else {
            showAndRemove(view!!.refreshContainer, view!!.loadingView)
        }
        view!!.filterButton.isClickable = !isLoading
        view!!.filterButton.isEnabled = !isLoading
        view!!.sortButton.isClickable = !isLoading
    }

    override val refreshItemList: Observable<Unit> get() = view!!.refreshContainer.refreshes()

    override val isRefreshing: Boolean get() = view!!.refreshContainer.isRefreshing

    override fun stopRefreshing() {
        view!!.refreshContainer.isRefreshing = false
    }

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(view!!)
        } else {
            errorHelper.hideNetworkError(parent = view!!, child = view!!.refreshContainer.entriesView)
        }
    }

    override val retryNetworkConnectionClicks: Observable<Unit>
        get() = view!!.networkWarning.retryButton.clicks()
}
