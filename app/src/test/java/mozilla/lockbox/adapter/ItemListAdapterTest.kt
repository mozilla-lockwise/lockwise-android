/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.view.ItemViewHolder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ItemListAdapterTest {

    val subject = ItemListAdapter()
    private lateinit var context: Context
    private lateinit var parent: RecyclerView

    private val list = listOf<ItemViewModel>(
            ItemViewModel("mozilla.org", "example@example.com", "", Date().time),
            ItemViewModel("cats.org", "cats@cats.com", "", Date().time),
            ItemViewModel("dogs.org", "woof@woof.com", "", Date().time)
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
        subject.updateItems(list)
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
        val viewHolder = subject.onCreateViewHolder(parent, 1)

        subject.onBindViewHolder(viewHolder, 0)
    }

    @Test
    fun getItemCount() {
        Assert.assertEquals(3, subject.itemCount)
    }

    @Test
    fun getItemViewType_populatedList() {
        Assert.assertEquals(subject.getItemViewType(0), 0)
    }

    @Test
    fun getItemViewType_emptyList() {
        subject.updateItems(emptyList())

        Assert.assertEquals(subject.getItemViewType(0), 1)
    }
}