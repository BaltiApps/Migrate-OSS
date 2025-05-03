package balti.migrate.common.data.model

import baltiapps.migrate.domain.common.model.GenericFile

class MediaStoreDownloadFile(
    override val path: String,
    override val isValidBackupDirectory: Boolean = false
) : GenericFile {

    companion object {
        const val EXPORT_PATH_PREFIX = "Download/Migrate/"
    }

}