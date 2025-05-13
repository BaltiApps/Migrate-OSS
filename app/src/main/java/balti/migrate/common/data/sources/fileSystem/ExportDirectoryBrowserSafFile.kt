package balti.migrate.common.data.sources.fileSystem

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import balti.migrate.common.data.model.SafFile
import baltiapps.migrate.domain.common.sources.fileSystem.ExportDirectoryBrowser
import baltiapps.migrate.domain.common.utils.BackupFilesUtils

class ExportDirectoryBrowserSafFile(
    private val applicationContext: Context,
): ExportDirectoryBrowser<SafFile> {
    override suspend fun getDirectories(root: SafFile): List<SafFile> {
        val documentFile =
            DocumentFile.fromTreeUri(applicationContext, root.uriToLocation)
                ?: return emptyList()

        return documentFile.listFiles()
            .filter { it.isDirectory }
            .map { child ->
                val filesInChildren = child.listFiles()
                val isValidBackupDirectory = filesInChildren.any {
                    BackupFilesUtils.shouldImportFile(it.name ?: "")
                }
                SafFile(
                    uriToLocation = child.uri,
                    name = child.name ?: "",
                    isValidBackupDirectory = isValidBackupDirectory,
                    parent = root,
                )
            }
    }

    override suspend fun getFilesUnder(directory: SafFile): List<SafFile> {
        val documentFile =
            DocumentFile.fromTreeUri(applicationContext, directory.uriToLocation)
                ?: return emptyList()

        return documentFile.listFiles().map {
            SafFile(
                uriToLocation = it.uri,
                name = it.name ?: "",
            )
        }
    }

}