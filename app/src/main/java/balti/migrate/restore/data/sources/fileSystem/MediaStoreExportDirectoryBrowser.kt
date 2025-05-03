package balti.migrate.restore.data.sources.fileSystem

import android.content.Context
import android.provider.MediaStore
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.restore.sources.ExportDirectoryBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class MediaStoreExportDirectoryBrowser(
    private val applicationContext: Context,
    private val dbUtils: DBUtils,
): ExportDirectoryBrowser<MediaStoreDownloadFile> {

    override suspend fun getDirectories(root: MediaStoreDownloadFile): List<MediaStoreDownloadFile> {
        val directoryPath = getTrimmedDirectoryPath(root)
        val allFilePaths = getAllPaths(directoryPath)

        /**
         * Possible examples of allFilePaths:
         *
         * sample_text.txt
         * contacts.db
         * 02-May/sms.db
         * 03-May/contacts.db
         * 03-May/call_logs.db
         * dir1/01-May/sample_text2.txt
         * dir1/01-May/sms.db
         * dir1/01-May/my_dir/audio.wav
         * dir2/image.png
         * dir3/
         */

        val validBackupFiles = allFilePaths.filter {
            val name = it.substringAfterLast('/')
            name in listOf(
                BACKUP_FILE_NAME_CONTACTS,
                BACKUP_FILE_NAME_CALL_LOGS,
                BACKUP_FILE_NAME_SMS,
            )
        }

        /**
         * Examples of validBackupFiles:
         *
         * contacts.db
         * 02-May/sms.db
         * 03-May/contacts.db
         * 03-May/call_logs.db
         * dir1/01-May/sms.db
         *
         * We should get three directories:
         *
         * 02-May - isValidBackupDirectory=true
         * 03-May - isValidBackupDirectory=true
         * dir1 - isValidBackupDirectory=false
         */

        val validBackupDirectories = validBackupFiles
            .filter { it.count { char -> char == '/' } == 1 }  // 02-May/sms.db,
                                                               // 03-May/contacts.db,
                                                               // 03-May/call_logs.db
            .map { it.substringBefore('/') }          // 02-May, 03-May, 03-May
            .toSet()                                           // 02-May, 03-May
            .map {
                MediaStoreDownloadFile(
                    path = "$directoryPath/$it",
                    isValidBackupDirectory = true
                )
            }

        val invalidBackupDirectories = validBackupFiles
            .filter { it.count { char -> char == '/' } > 1 }   // dir1/01-May/sms.db
            .map { it.substringBefore('/') }          // dir1
            .toSet()
            .map {
                MediaStoreDownloadFile(
                    path = "$directoryPath/$it",
                    isValidBackupDirectory = false
                )
            }

        return (validBackupDirectories + invalidBackupDirectories).sortedByDescending { it.name }
    }

    override suspend fun getFilesUnder(directory: MediaStoreDownloadFile): List<MediaStoreDownloadFile> {
        val directoryPath = getTrimmedDirectoryPath(directory)
        val allFilePaths = getAllPaths(directoryPath)

        /**
         * Possible examples of allFilePaths:
         *
         * sample_text2.txt
         * sms.db
         * my_dir/audio.wav
         *
         * We need only:
         *
         * sample_text2.txt
         * sms.db
         */

        return allFilePaths
            .filter { !it.contains('/') }
            .map {
                MediaStoreDownloadFile("$directoryPath/$it")
            }
    }

    private suspend fun getAllPaths(directoryPath: String): List<String> {
        return withContext(Dispatchers.IO) {
            val allFilePaths = mutableListOf<String>()
            val resolver = applicationContext.contentResolver

            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val fullPath = dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.DATA)
                    val subPath = fullPath.substringAfter("$directoryPath/").trimEnd('/')
                    Timber.i("Path - $subPath")
                    allFilePaths.add(subPath)
                }
            }
            allFilePaths
        }
    }

    private fun getTrimmedDirectoryPath(directory: MediaStoreDownloadFile): String {
        return directory.path.trimEnd('/')
    }
}