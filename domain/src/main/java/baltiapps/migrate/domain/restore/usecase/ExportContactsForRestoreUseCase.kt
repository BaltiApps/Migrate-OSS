package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.runCatchingWithProgress
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Export selected contacts to .vcf file
 */
class ExportContactsForRestoreUseCase(
    private val dataRestore: RestoreDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
        textWriter: TextWriter<String>,
        getContactContent: (item: DataItem<ContactListItem>) -> String,
    ): Flow<Progress> {
        return flow {
            if (!dataRestore.shouldRestoreContacts()) return@flow

            textWriter.setup(
                fileLocation = file.parentPath,
                fileName = file.name,
                append = false,
            )

            val stagedContacts = dataRestore.stagedContacts
            stagedContacts.forEachIndexed { index, item ->
                val progress = Progress(
                    itemId = item._id,
                    progressType = Progress.ProgressType.CONTACTS_EXPORT,
                    percentage = getPercentage(index + 1, stagedContacts.size),
                    logs = "(${index + 1}/${stagedContacts.size}) ${item.logInfo}"
                )
                runCatchingWithProgress(progress) {
                    textWriter.writeLine(getContactContent(item))
                }.run { emit(this) }
            }

            textWriter.close()
        }.flowOn(Dispatchers.IO)
    }
}