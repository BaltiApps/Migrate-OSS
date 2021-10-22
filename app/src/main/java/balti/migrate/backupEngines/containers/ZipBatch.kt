package balti.migrate.backupEngines.containers

import balti.filex.Quad
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE_DEFAULT

class ZipBatch {

    /**
     * Space needed (in bytes) in /system to restore this zip.
     * Usually applicable for system apps backup.
     */
    var batchSystemSize : Long = 0
    private set

    /**
     * Space needed (in bytes) in /data to restore this zip.
     * Usually this includes a major chunk of the backup data,
     * and is unpacked under [MIGRATE_CACHE_DEFAULT] = /data/local/tmp/migrate_cache.
     */
    var batchDataSize : Long = 0
    private set

    val zipFullSize : Long get() = batchDataSize + batchSystemSize

    var zipName = ""
    private set

    val appPackets = ArrayList<ZipAppPacket>(0)
    val extraPackets = ArrayList<ZipExtraPacket>(0)

    /**
     * List of files included in the zip.
     * Triple consists of:
     * - String - Path of the file (from the root location) in the backup.
     *   Name of the file can be found using `substring` from `lastIndexOf` of '/'
     * - Long - Size of the file in bytes.
     * - Boolean - True if is directory.
     * - Boolean - True if the file is to be restored in /system
     */
    val zipFiles = ArrayList<Quad<String, Long, Boolean, Boolean>>(0)

    /**
     * Go over all the entries in [zipFiles]
     * and add the space needed for [batchSystemSize] and [batchDataSize]
     */
    private fun refreshTotal(){
        batchSystemSize = 0
        batchDataSize = 0
        zipFiles.forEach {
            if (it.fourth) batchSystemSize += it.second
            else batchDataSize += it.second
        }
    }

    /**
     * Add files related to a [ZipAppPacket].
     * Files are added as such:
     * - Metadata file: (".json" file name, size, directory = false, system = false)
     * - Icon file:     (".png" file name, size, directory = false, system = false)
     * - Permission:    (".perm" file name, size, directory = false, system = false)
     * - .sh file:     (".sh" file name, size, directory = false, system = false)
     * - Data file:     (".tar.gz" file name, size, directory = false, system = false)
     * - App directory: (".app" directory name, size = 0, directory = true, system = false)
     * - Each entry from `appApkFiles`:
     *   (".app" directory name / ".apk" file name, size, directory = false, system = true if system app)
     */
    fun addZipAppPacket(zipAppPacket: ZipAppPacket){

        zipAppPacket.refreshTotal()

        val isSystemApp = zipAppPacket.appPacket_z.isSystem
        var appDirName = ""

        zipAppPacket.appFileNames.indices.forEach { i ->
            val itemName = zipAppPacket.appFileNames[i]
            val isDirectory =
                if (itemName.endsWith(".app")) {
                    appDirName = itemName
                    true
                } else false

            /**
             * Store 0 for ".app" app directory. Actual values will be taken from `appApkFiles`.
             */
            val itemSize: Long =
                if (isDirectory) 0 else zipAppPacket.fileSizes.getOrNull(i) ?: 0

            /**
             * Everything in the appFileNames - data "tar.gz",
             * permission "perm" file, icon file, metadata "json" file
             * will be restored under [MIGRATE_CACHE_DEFAULT].
             *
             * However for any system apps, there will be a corresponding .sh file.
             * This also gets unpacked in /tmp in TWRP recovery, hence its not a system file.
             *
             * For the ".app" directory, its size is 0, so it doesn't matter
             * if its marked as under /system or under /data.
             */

            zipFiles.add(Quad(itemName, itemSize, isDirectory, false))
        }

        /**
         * Add the base/split apks and their size.
         */
        if (appDirName.isNotBlank()){
            zipAppPacket.appApkFiles.forEach {

                /**
                 * 3rd argument is always false as they are apk files and not directories.
                 * 4th argument depends if the app is system app.
                 */
                zipFiles.add(Quad("${appDirName}/${it.first}", it.second, false, isSystemApp))
            }
        }

        /**
         * Finally add the packet to [appPackets] and refresh the total zip size.
         */
        appPackets.add(zipAppPacket)
        refreshTotal()
    }

    /**
     * Add extra backups files related to [ZipExtraPacket].
     */
    fun addZipExtraPacket(zipExtraPacket: ZipExtraPacket){

        zipExtraPacket.refreshTotal()
        zipExtraPacket.extraFiles.forEach {
            val itemName = it.first
            val itemSize = it.second
            val isSystem = it.third

            val lastSlashIndex = itemName.lastIndexOf('/')

            /**
             * If slash is present, means the item is under a directory.
             * Add that directory separately.
             */
            if (lastSlashIndex != -1){
                val dirName = itemName.substring(0, lastSlashIndex)
                zipFiles.add(Quad(dirName, 0, third = true, fourth = false))
            }

            /**
             * Add the entry as a file.
             */
            zipFiles.add(Quad(itemName, itemSize, false, isSystem))
        }

        /**
         * Finally add the packet to [extraPackets] and refresh total.
         */
        extraPackets.add(zipExtraPacket)
        refreshTotal()

    }
}