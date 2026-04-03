package baltiapps.migrate.domain.common.sources.fileSystem

import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion

abstract class FileSystemSource() {
    abstract fun createDirectory(directory: GenericFile): Boolean
    abstract fun moveDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean
    abstract fun copyDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (relativeFilePath: String) -> Boolean = { true },
    ): Boolean

    abstract fun getLocalFilePath(file: GenericFile): String?

    inline fun <T: GenericReader<*, *>> read(
        file: GenericFile,
        reader: T,
        readerBlock: (reader: T) -> Flow<Progress>,
    ): Flow<Progress> {
        val path = getLocalFilePath(file) ?: file.path
        reader.setup(path)
        return readerBlock(reader).onCompletion {
            reader.close()
        }
    }
}

interface TextWriter<T> {
    fun setup(fileLocation: String, fileName: String, append: Boolean)
    fun write(data: T)
    fun writeLine(data: T)
    fun close()
}

interface TextReader<T> {
    fun setup(fileLocation: String, fileName: String)
    fun read(): T
    fun readLines(onFinished: (List<T>) -> Unit): Flow<Progress>
    fun close()
}

interface GenericReader<in T, out U> {
    fun setup(readLocation: String)
    fun read(data: T, onComplete: (result: U) -> Unit = {}): Flow<Progress>
    fun close()
}
