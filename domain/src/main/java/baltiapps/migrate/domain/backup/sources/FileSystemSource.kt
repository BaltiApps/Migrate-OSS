package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.backup.model.DataItem

abstract class FileSystemSource() {
    abstract fun checkPermission(filePath: String = ""): Boolean
    abstract fun createDirectory(dirPath: String): Boolean

    inline fun <T: TextWriter> writeText(
        directory: String,
        fileName: String,
        append: Boolean,
        textWriter: T,
        writerBlock: (fileWriter: T) -> Unit,
    ) {
        try {
            if (!createDirectory(directory)) {
                throw Exception("NOT permitted to write at: $directory")
            }
            textWriter.setup("$directory/$fileName", append)
            writerBlock(textWriter)
        } finally {
            textWriter.close()
        }
    }

    inline fun <T: DBWriter<*>> writeDB(
        directory: String,
        fileName: String,
        dbWriter: T,
        writerBlock: (dbWriter: T) -> Unit,
    ) {
        try {
            if (!createDirectory(directory)) return
            dbWriter.setup("$directory/$fileName")
            writerBlock(dbWriter)
        } finally {
            dbWriter.close()
        }
    }
}

interface TextWriter {
    fun setup(fileLocation: String, append: Boolean)
    fun write(text: String)
    fun writeLine(line: String)
    fun close()
}

interface DBWriter<T: DataItem<*>> {
    fun setup(fileLocation: String)
    fun writeRow(dataItem: T)
    fun close()
}