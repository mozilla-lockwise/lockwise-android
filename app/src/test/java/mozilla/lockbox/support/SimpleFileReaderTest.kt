package mozilla.lockbox.support

import org.junit.Assert
import org.junit.Test
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths

class SimpleFileReaderTest {

    @Test(expected = FileNotFoundException::class)
    fun `when there is no file`() {
        SimpleFileReader().readContents("/nada")
    }

    @Test
    fun `when there is a file`() {
        val filename = "cats.txt"
        val contents = "meow"
        Files.write(Paths.get(filename), contents.toByteArray())

        Assert.assertEquals(contents, SimpleFileReader().readContents(filename))

        Files.delete(Paths.get(filename))
    }
}