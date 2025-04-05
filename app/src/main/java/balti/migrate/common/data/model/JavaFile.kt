package balti.migrate.common.data.model

import baltiapps.migrate.domain.common.model.GenericFile
import java.io.File

class JavaFile : GenericFile {
    private lateinit var file: File
    private constructor()
    constructor(file: File): this() {
        this.file = file
    }
    constructor(path: String): this() {
        this.file = File(path)
    }

    override val path: String = file.path
    override val name: String = file.name
    override val exists: Boolean = file.exists()
    override val canRead: Boolean = file.canRead()
    override val canWrite: Boolean = file.canWrite()
    override val isFile: Boolean = file.isFile
}