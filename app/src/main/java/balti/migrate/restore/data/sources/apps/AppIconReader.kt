package balti.migrate.restore.data.sources.apps

import android.graphics.drawable.Drawable
import balti.migrate.common.data.model.AppIcon
import baltiapps.migrate.domain.common.model.DrawableAsset
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.TextReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File

class AppIconReader(): TextReader<DrawableAsset> {
    private lateinit var file: File

    override fun setup(
        fileLocation: String,
        fileName: String,
    ) {
        file = File(fileLocation, fileName)
    }

    override fun read(): DrawableAsset {
        val inputStream = file.inputStream()
        val drawable = Drawable.createFromStream(inputStream, null)

        return if (drawable != null) {
            AppIcon(drawable)
        } else {
            AppIcon()
        }
    }

    override fun readLines(onFinished: (List<DrawableAsset>) -> Unit): Flow<Progress> {
        onFinished(listOf(read()))
        return emptyFlow()
    }

    override fun close() {}
}