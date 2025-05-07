package balti.migrate.common.data.model

import android.net.Uri
import baltiapps.migrate.domain.common.model.GenericFile

data class SafFile(
    val uriToLocation: Uri,
    override val name: String,
    override val path: String = "",
    override val isValidBackupDirectory: Boolean = false,
) : GenericFile {
}