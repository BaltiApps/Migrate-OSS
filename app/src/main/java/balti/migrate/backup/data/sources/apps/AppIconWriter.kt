package balti.migrate.backup.data.sources.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import java.io.File
import java.io.FileOutputStream

class AppIconWriter: TextWriter<AppData> {
    private lateinit var file: File

    override fun setup(fileLocation: String, fileName: String, append: Boolean) {
        File(fileLocation, fileName).run {
            if (exists()) { delete() }
            file = this
        }
    }

    override fun write(data: AppData) {
        saveAppIconToFile(data.appIcon)
    }

    override fun writeLine(data: AppData) {
        write(data)
    }

    override fun close() {}

    private fun saveAppIconToFile(drawable: Drawable) {
        val sizePx = 256

        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}