/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.observers.TestObserver
import kotlinx.android.synthetic.main.list_cell_item.*
import kotlinx.android.synthetic.main.list_cell_no_entries.view.*
import kotlinx.android.synthetic.main.list_cell_no_matching.view.*
import kotlinx.android.synthetic.main.list_cell_setting_toggle.*
import mozilla.lockbox.R
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.view.ItemViewHolder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ItemListAdapterTest {

    val itemObserver = TestObserver.create<ItemViewModel>()
    val noEntriesObserver = TestObserver.create<Unit>()
    val noMatchingObserver = TestObserver.create<Unit>()

    val subject = ItemListAdapter()
    private lateinit var context: Context
    private lateinit var parent: RecyclerView

    private val list = listOf<ItemViewModel>(
        ItemViewModel("mozilla.org", "example@example.com", ""),
        ItemViewModel("cats.org", "cats@cats.com", ""),
        ItemViewModel("dogs.org", "woof@woof.com", ""),
        ItemViewModel("morecats.org", "", "")
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
        subject.itemClicks.subscribe(itemObserver)
        subject.noEntriesClicks.subscribe(noEntriesObserver)
        subject.noMatchingEntriesClicks.subscribe(noMatchingObserver)

        subject.updateItems(list)
    }

    @Test
    fun onBindViewHolder_mapEmptyUsernameToPlaceholder() {
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder

        subject.onBindViewHolder(viewHolder, 3)
        Assert.assertEquals(context.resources.getString(R.string.no_username), viewHolder.itemSubtitle.text)
    }

    @Test
    fun onBindViewHolder_populatedList() {
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder

        subject.onBindViewHolder(viewHolder, 1)

        Assert.assertEquals(list[1], viewHolder.itemViewModel)
    }

    @Test
    fun onBindViewHolder_emptyList() {
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 2)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun onBindViewHolder_emptyFilteringList() {
        subject.updateItems(emptyList(), true)
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun onBindViewHolder_populatedList_clicks() {
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder
        val indexOfItem = 1
        val expectedItemViewModel = list[indexOfItem]

        subject.onBindViewHolder(viewHolder, indexOfItem)

        Assert.assertEquals(expectedItemViewModel, viewHolder.itemViewModel)

        viewHolder.containerView.performClick()

        itemObserver.assertLastValue(expectedItemViewModel)
    }

    @Test
    fun onBindViewHolder_emptyList_clicks() {
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 2)

        subject.onBindViewHolder(viewHolder, 0)

        viewHolder.containerView.noEntriesLearnMore.performClick()

        noEntriesObserver.assertValueCount(1)
    }

    @Test
    fun onBindViewHolder_emptyFilteringList_clicks() {
        subject.updateItems(emptyList(), true)
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)

        viewHolder.containerView.noMatchingLearnMore.performClick()

        noMatchingObserver.assertValueCount(1)
    }

    @Test
    fun getItemCount() {
        Assert.assertEquals(4, subject.itemCount)
    }

    @Test
    fun getItemViewType_populatedList() {
        Assert.assertEquals(subject.getItemViewType(0), 0)
    }

    @Test
    fun getItemViewType_emptyFilteringList() {
        subject.updateItems(emptyList(), true)

        Assert.assertEquals(subject.getItemViewType(0), 1)
    }

    @Test
    fun getItemViewType_emptyList() {
        subject.updateItems(emptyList())

        Assert.assertEquals(subject.getItemViewType(0), 2)
    }
}