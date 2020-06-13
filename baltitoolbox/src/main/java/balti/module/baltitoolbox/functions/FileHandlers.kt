package balti.module.baltitoolbox.functions

import android.os.Environment
import balti.module.baltitoolbox.R
import balti.module.baltitoolbox.ToolboxHQ
import balti.module.baltitoolbox.functions.FileHandlers.INTERNAL_TYPE.*
import java.io.*

object FileHandlers {

    private val context = ToolboxHQ.context

    private fun copyStream(inputStream: InputStream, outputStream: OutputStream): String{

        var read: Int
        val buffer = ByteArray(4096)

        return try {
            while (true) {
                read = inputStream.read(buffer)
                if (read > 0) outputStream.write(buffer, 0, read)
                else break
            }
            outputStream.close()
            return ""
        } catch (e: IOException){
            e.printStackTrace()
            e.message.toString()
        }
    }

    /**
     * Allows to specify kind of parent to be used in [getInternalFile], [unpackAssetToInternal]
     *
     * @property[INTERNAL_FILES]    Parent should be [android.content.Context.getFilesDir]
     * @property[INTERNAL_CACHE]    Parent should be [android.content.Context.getCacheDir]
     * @property[EXTERNAL_FILES]    Parent should be [android.content.Context.getExternalFilesDir] (null type)
     * @property[EXTERNAL_CACHE]    Parent should be [android.content.Context.getExternalCacheDir]
     */
    enum class INTERNAL_TYPE {
        INTERNAL_FILES,
        INTERNAL_CACHE,
        EXTERNAL_FILES,
        EXTERNAL_CACHE
    }

    /**
     * Get a File object using a specified parent.
     * Parent can be specified using a type from [INTERNAL_TYPE]
     *
     * @param[fileName]        A string for file name
     * @param[internalType]    A type from one of [INTERNAL_TYPE]
     *
     * @return  A File object with parent as specified by [fileName]
     */
    fun getInternalFile(fileName: String, internalType: INTERNAL_TYPE = INTERNAL_TYPE.INTERNAL_FILES): File =
            File(
                    when(internalType){
                        INTERNAL_TYPE.INTERNAL_FILES -> context.filesDir
                        INTERNAL_TYPE.INTERNAL_CACHE -> context.cacheDir
                        INTERNAL_TYPE.EXTERNAL_CACHE -> context.externalCacheDir
                        INTERNAL_TYPE.EXTERNAL_FILES -> context.getExternalFilesDir(null)
                    }, fileName
            )

    /**
     * Extract an asset from the apk
     *
     * @param[assetFileName]    Exact name of the asset
     * @param[destination]      File destination where the asset is to be extracted
     *
     * @return  Error occurred during extract. Empty string if no error.
     */
    fun unpackAsset(assetFileName: String, destination: File): String {

        val assetManager = context.assets
        val unpackFile = destination.let {
            if (it.isDirectory) File(it, assetFileName)
            else it
        }

        unpackFile.run {
            mkdirs()
            if (!canWrite()) return "${context.getString(R.string.cannot_write_on_destination)} ${unpackFile.absolutePath}"
            else if (unpackFile.exists()) unpackFile.delete()
        }

        val inputStream = assetManager.open(assetFileName)
        val outputStream = FileOutputStream(unpackFile)

        return copyStream(inputStream, outputStream)
    }

    /**
     * Similar to [unpackAsset] but extracts an asset only in a 'internal' location of the app.
     * 'internal' meaning either the apps private data directory, private directory on the SD card
     *
     * @param[assetFileName]    Exact name of the asset
     * @param[destinationName]  A string specifying the name of the extracted asset.
     * Difference with that of destination of [unpackAsset] is that [destinationName] only accepts
     * a string to define the final extracted asset file name, not the location of extraction.
     * @param[internalType]     An internal parent type of [INTERNAL_TYPE]
     *
     * @return  A string file path to the extracted asset, if extraction is successful, else a blank string
     */
    fun unpackAssetToInternal(assetFileName: String, destinationName: String = assetFileName, internalType: INTERNAL_TYPE = INTERNAL_FILES): String {
        val d = getInternalFile(destinationName, internalType)
        return unpackAsset(assetFileName, d).let {
            if (it == "") d.absolutePath
            else ""
        }
    }

    fun copyFileStream(sourceFile: File, destination: String): String {

        if (!(sourceFile.exists() && sourceFile.canRead())) return "${context.getString(R.string.source_does_not_exist)} ${sourceFile.absolutePath}"

        val destinationFile = File(destination).let {
            if (it.isDirectory) File(it, sourceFile.name)
            else it
        }

        destinationFile.run {
            mkdirs()
            if (!canWrite()) return "${context.getString(R.string.cannot_write_on_destination)} ${destinationFile.absolutePath}"
            else if (destinationFile.exists()) destinationFile.delete()
        }

        val inputStream = sourceFile.inputStream()
        val outputStream = FileOutputStream(destinationFile)

        return copyStream(inputStream, outputStream)

    }

    fun copyFileStream(sourceFile: File, destinationFile: File): String = copyFileStream(sourceFile, destinationFile.absolutePath)

    fun moveFileStream(sourceFile: File, destination: String): String {
        val r = copyFileStream(sourceFile, destination)
        sourceFile.delete()
        return r
    }

    fun moveFileStream(sourceFile: File, destinationFile: File): String = moveFileStream(sourceFile, destinationFile.absolutePath)

    fun getDirLength(directoryPath: String): Long {
        val file = File(directoryPath)
        return if (file.exists()) {
            if (!file.isDirectory) file.length()
            else {
                var sum = 0L
                file.listFiles()?.let {
                    for (f in it) sum += getDirLength(f.absolutePath)
                }
                sum
            }
        } else 0
    }

    fun getDirLength(directory: File): Long = getDirLength(directory.absolutePath)

    fun dirDelete(path: String) {
        val file = File(path)
        if (file.exists() && file.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
            file.deleteRecursively()
        }
    }

}