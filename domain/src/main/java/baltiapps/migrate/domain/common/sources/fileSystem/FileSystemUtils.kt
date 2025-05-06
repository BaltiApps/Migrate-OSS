package baltiapps.migrate.domain.common.sources.fileSystem

class FileSystemUtils {
    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}