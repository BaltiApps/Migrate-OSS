package balti.migrate.backup.data.repository

import balti.migrate.backup.data.model.ContactData
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.sources.FileSystemSource
import baltiapps.migrate.domain.backup.sources.TextWriter

class DataRepositoryImpl(
    private val contactsSource: DataSource<ContactData>,
    private val textWriter: TextWriter<String>,
    private val fileSystemSource: FileSystemSource,
) : DataRepository() {
}