package mozilla.lockbox.adapter

import android.content.Context
import mozilla.lockbox.action.Setting
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class SortItemAdapterTest {
    private val context = Mockito.mock(Context::class.java)
    private val list = listOf<Setting.ItemListSort>(
        Setting.ItemListSort.ALPHABETICALLY,
        Setting.ItemListSort.RECENTLY_USED
    )

    var subject = SortItemAdapter(context, android.R.layout.simple_spinner_item, ArrayList(list))

    @Test
    fun `setSelection should update list`() {
        subject = spy(subject)
        subject.setSelection(1)
        verify(subject).notifyDataSetChanged()
    }
}