package balti.migrate.common.data.sources.fileSystem

import android.content.Context
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.exceptions.UnknownFileTypeException

class FileSystemSourceImpl(
    private val applicationContext: Context,
    private val dbUtils: DBUtils,
) : FileSystemSource() {

    private val javaFileUtils by lazy {
        JavaFileUtils(applicationContext)
    }

    private val mediaStoreDownloadUtils by lazy {
        MediaStoreDownloadUtils(applicationContext, dbUtils)
    }

    override fun createDirectory(directory: GenericFile): Boolean {
        return when(directory) {
            is JavaFile -> {
                directory.file.mkdirs()
                directory.file.canWrite()
            }
            is MediaStoreDownloadFile -> mediaStoreDownloadUtils.createNoMediaFile(directory)
            else -> false
        }
    }

    override fun moveDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (String) -> Boolean,
    ): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                javaFileUtils.transferJavaFileToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                javaFileUtils.transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                mediaStoreDownloadUtils.transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown move - Source type - ${source::class.java} and destination type - ${destination::class.java}"
            )
        }
    }

    override fun copyDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (String) -> Boolean,
    ): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                javaFileUtils.transferJavaFileToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                javaFileUtils.transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                mediaStoreDownloadUtils.transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown copy - Source type - ${source::class.java} and destination type - ${destination::class.java}"
            )
        }
    }

}