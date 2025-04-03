package balti.migrate.common.data.sources.fileSystem

import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import java.io.File
import java.io.FileWriter

class TextWriterImpl: TextWriter<String> {
    private lateinit var file: File
    private lateinit var fileWriter: FileWriter

    override fun setup(fileLocation: String, append: Boolean) {
        File(fileLocation).run {
            if (!append && exists()) {
                delete()
            }
            file = this
            fileWriter = FileWriter(file, true)
        }
    }

    override fun write(data: String) {
        fileWriter.write(data)
    }

    override fun writeLine(data: String) {
        write("${data}\n")
    }

    override fun close() {
        if (::file.isInitialized) {
            fileWriter.close()
        }
    }
}