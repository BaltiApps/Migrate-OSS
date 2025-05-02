package baltiapps.migrate.domain.common.sources.fileSystem

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion

abstract class FileSystemSource() {
    abstract fun createDirectory(dirPath: String): Boolean
    abstract fun createDirectory(directory: Directory): Boolean

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
        directory: Directory,
        file: GenericFile,
        dbWriter: T,
        writerBlock: (dbWriter: T) -> Flow<Progress>,
    ): Flow<Progress> {
        if (!createDirectory(directory)) {
            return emptyFlow()
        }
        dbWriter.setup(file)
        return writerBlock(dbWriter).onCompletion {
            dbWriter.close()
        }
    }

    inline fun <T: DBReader<*>> readDB(
        file: GenericFile,
        dbReader: T,
        readerBlock: (dbReader: T) -> Flow<Progress>,
    ): Flow<Progress> {
        dbReader.setup(file)
        return readerBlock(dbReader).apply {
            onCompletion { dbReader.close() }
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
    fun setup(file: GenericFile)
    fun writeRows(dataItems: List<T>): Flow<Progress>
    fun close()
}

interface DBReader<T: DataItem<*>> {
    fun setup(file: GenericFile)
    fun readRows(onFinished: (List<T>) -> Unit): Flow<Progress>
    fun close()
}