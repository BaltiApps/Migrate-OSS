package balti.migrate.backup.data.sources.contacts

import balti.migrate.backup.data.model.ContactData
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import java.io.File
import java.io.FileWriter

class ContactsWriter: TextWriter<ContactData> {
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

    override fun write(data: ContactData) {
        writeLine(data)
    }

    override fun writeLine(data: ContactData) {
        fileWriter.write("${data.vcfContent}\n")
    }

    override fun close() {
        if (::file.isInitialized) {
            fileWriter.close()
        }
    }
}