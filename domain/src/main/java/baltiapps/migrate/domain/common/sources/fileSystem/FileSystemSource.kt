package baltiapps.migrate.domain.common.sources.fileSystem

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion

abstract class FileSystemSource() {
    abstract fun createDirectory(directory: GenericFile): Boolean

    inline fun <T: DBWriter<*>> writeDB(
        file: GenericFile,
        dbWriter: T,
        writerBlock: (dbWriter: T) -> Flow<Progress>,
    ): Flow<Progress> {
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