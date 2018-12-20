package mozilla.lockbox.support

import java.io.FileNotFoundException
import java.io.FileReader

class SimpleFileReader {
    @Throws(FileNotFoundException::class)
    fun readContents(filePath: String): String {
        FileReader(filePath).use {
            return it.readText()
        }
    }
}