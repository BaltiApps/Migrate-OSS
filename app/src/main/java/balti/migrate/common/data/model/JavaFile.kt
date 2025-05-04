package balti.migrate.common.data.model

import baltiapps.migrate.domain.common.model.GenericFile
import java.io.File

class JavaFile(val file: File) : GenericFile {
    constructor(path: String): this(File(path))
    constructor(parentFile: JavaFile, child: String) : this(File(parentFile.file, child))

    override val path: String = file.absolutePath
    override val name: String = file.name
}