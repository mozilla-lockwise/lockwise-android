/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import io.reactivex.observers.TestObserver
import kotlinx.android.synthetic.main.list_cell_item.*
import kotlinx.android.synthetic.main.list_cell_no_entries.view.*
import mozilla.lockbox.R
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.view.ItemViewHolder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ItemListAdapterTest {

    val itemObserver = TestObserver.create<ItemViewModel>()
    val noEntriesObserver = TestObserver.create<Unit>()

    lateinit var subject: ItemListAdapter
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
        context = ApplicationProvider.getApplicationContext()
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
    }

    @Test
    fun `on bind view map empty username to placeholder`() {
        setupSubject(ItemListAdapterType.ItemList)
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder

        subject.onBindViewHolder(viewHolder, 3)
        Assert.assertEquals(
            context.resources.getString(R.string.no_username),
            viewHolder.itemSubtitle.text
        )
    }

    @Test
    fun `on bind with populated list`() {
        setupSubject(ItemListAdapterType.ItemList)
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder

        subject.onBindViewHolder(viewHolder, 1)

        Assert.assertEquals(list[1], viewHolder.itemViewModel)
    }

    @Test
    fun `on bind with empty entries list`() {
        setupSubject(ItemListAdapterType.ItemList)
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 2)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun `on bind with empty filtered list`() {
        setupSubject(ItemListAdapterType.Filter)
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun `on bind with empty autofill filtered list with text`() {
        setupSubject(ItemListAdapterType.AutofillFilter)
        subject.displayNoEntries(true)
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun `on bind with empty autofill filtered list without text`() {
        setupSubject(ItemListAdapterType.AutofillFilter)
        subject.updateItems(emptyList())
        subject.displayNoEntries(false)
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun `on bind with click on populated list`() {
        setupSubject(ItemListAdapterType.ItemList)
        val viewHolder = subject.onCreateViewHolder(parent, 0) as ItemViewHolder
        val indexOfItem = 1
        val expectedItemViewModel = list[indexOfItem]

        subject.onBindViewHolder(viewHolder, indexOfItem)

        Assert.assertEquals(expectedItemViewModel, viewHolder.itemViewModel)

        viewHolder.containerView.performClick()

        itemObserver.assertLastValue(expectedItemViewModel)
    }

    @Test
    fun `on bind with click on empty list`() {
        setupSubject(ItemListAdapterType.ItemList)
        subject.updateItems(emptyList())
        val viewHolder = subject.onCreateViewHolder(parent, 2)

        subject.onBindViewHolder(viewHolder, 0)

        viewHolder.containerView.noEntriesLearnMore.performClick()

        noEntriesObserver.assertValueCount(1)
    }

    @Test
    fun getItemCount() {
        setupSubject(ItemListAdapterType.ItemList)
        Assert.assertEquals(4, subject.itemCount)
    }

    @Test
    fun `getItemCount for empty itemlists`() {
        setupSubject(ItemListAdapterType.ItemList)
        subject.updateItems(emptyList())
        Assert.assertEquals(1, subject.itemCount)
    }

    @Test
    fun `getItemCount for empty filtering lists`() {
        setupSubject(ItemListAdapterType.Filter)
        subject.updateItems(emptyList())
        Assert.assertEquals(1, subject.itemCount)
    }

    @Test
    fun `getItemCount for empty autofill filtering lists with no text entered`() {
        setupSubject(ItemListAdapterType.AutofillFilter)
        subject.displayNoEntries(false)
        subject.updateItems(emptyList())
        Assert.assertEquals(0, subject.itemCount)
    }

    @Test
    fun `getItemCount for empty autofill filtering lists with text entered`() {
        setupSubject(ItemListAdapterType.AutofillFilter)
        subject.displayNoEntries(true)
        subject.updateItems(emptyList())
        Assert.assertEquals(1, subject.itemCount)
    }

    @Test
    fun `get view type on populated list`() {
        setupSubject(ItemListAdapterType.ItemList)
        Assert.assertEquals(subject.getItemViewType(0), 0)
    }

    @Test
    fun `get view type on empty filtered list`() {
        setupSubject(ItemListAdapterType.Filter)
        subject.updateItems(emptyList())

        Assert.assertEquals(subject.getItemViewType(0), 1)
    }

    @Test
    fun `get view type on empty list`() {
        setupSubject(ItemListAdapterType.ItemList)
        subject.updateItems(emptyList())

        Assert.assertEquals(subject.getItemViewType(0), 2)
    }

    @Test
    fun `getItemViewType empty autofill filtering list with text entered`() {
        setupSubject(ItemListAdapterType.AutofillFilter)
        subject.updateItems(emptyList())
        subject.displayNoEntries(true)

        Assert.assertEquals(subject.getItemViewType(0), 3)
    }

    private fun setupSubject(type: ItemListAdapterType) {
        subject = ItemListAdapter(type)
        subject.itemClicks.subscribe(itemObserver)
        subject.noEntriesClicks.subscribe(noEntriesObserver)

        subject.updateItems(list)
    }
}