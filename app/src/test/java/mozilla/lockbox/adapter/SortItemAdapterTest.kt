package mozilla.lockbox.adapter

import android.content.Context
import android.graphics.Color
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import mozilla.lockbox.R
import mozilla.lockbox.action.Setting
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class SortItemAdapterTest {
    private lateinit var context: Context
    private val list = listOf(
        Setting.ItemListSort.ALPHABETICALLY,
        Setting.ItemListSort.RECENTLY_USED
    )

    lateinit var subject: SortItemAdapter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        subject = SortItemAdapter(context, android.R.layout.simple_spinner_item, ArrayList(list))
    }

    @Test
    fun `setSelection should update list`() {
        subject = spy(subject)
        subject.setSelection(1)
        verify(subject).notifyDataSetChanged()
    }

    @Test
    fun `get view`() {
        val parent = ListView(context)
        val view = subject.getView(0, null, parent)

        assertThat(view, instanceOf(TextView::class.java))
        val label = spy(view as TextView)
        assertEquals(label.text, context.getString(R.string.all_logins_a_z))
        assertEquals(label.currentTextColor, Color.WHITE)
        assertEquals(label.textSize, 20.0f)
    }

    @Test
    fun `get dropdown view`() {
        val parent = ListView(context)
        val view = subject.getDropDownView(0, null, parent)

        assertThat(view, instanceOf(TextView::class.java))
        val label = spy(view as TextView)
        assertEquals(label.text, context.getString(R.string.sort_menu_az))
        assertEquals(label.currentTextColor, context.resources.getColor(R.color.black_87_percent, null))
        assertEquals(label.textSize, 14.0f)
    }
}