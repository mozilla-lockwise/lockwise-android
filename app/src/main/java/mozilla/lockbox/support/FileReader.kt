package mozilla.lockbox.support

import java.io.FileReader

class FileReader {
    fun readContents(filePath: String): String {
        val file = FileReader(filePath)
        return file.readText()
    }
}