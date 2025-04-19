package balti.migrate.common.data.model

import baltiapps.migrate.domain.common.model.GenericFile
import java.io.File

class JavaFile(val file: File) : GenericFile {
    constructor(path: String): this(File(path))

    override val path: String = file.path
    override val name: String = file.name
    override val exists: Boolean = file.exists()
    override val canRead: Boolean = file.canRead()
    override val canWrite: Boolean = file.canWrite()
    override val isFile: Boolean = file.isFile
}