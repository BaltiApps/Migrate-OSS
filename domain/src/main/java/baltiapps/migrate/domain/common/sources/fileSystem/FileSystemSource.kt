package baltiapps.migrate.domain.common.sources.fileSystem

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

abstract class FileSystemSource() {
    abstract fun checkPermission(filePath: String = ""): Boolean
    abstract fun createDirectory(dirPath: String): Boolean

    inline fun <T, V: TextWriter<T>> writeText(
        directory: String,
        fileName: String,
        append: Boolean,
        textWriter: V,
        writerBlock: (fileWriter: V) -> Unit,
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

    inline fun <T: DBReader<*>> readDB(
        file: GenericFile,
        dbReader: T,
        readerBlock: (dbReader: T) -> Flow<Progress>,
    ): Flow<Progress> {
        try {
            dbReader.setup(file.path)
            return readerBlock(dbReader)
        } finally {
            dbReader.close()
        }
    }
}

interface TextWriter<T> {
    fun setup(fileLocation: String, append: Boolean)
    fun write(data: T)
    fun writeLine(data: T)
    fun close()
}

interface DBWriter<T: DataItem<*>> {
    fun setup(fileLocation: String)
    fun writeRow(dataItem: T)
    fun close()
}

interface DBReader<T: DataItem<*>> {
    fun setup(fileLocation: String)
    fun readRows(onFinished: (List<T>) -> Unit): Flow<Progress>
    fun close()
}