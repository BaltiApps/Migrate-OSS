package balti.migrate.backupEngines.containers

import balti.module.baltitoolbox.functions.Misc

/**
 * Container to store file name and sizes of each extra backup option.
 * Usage: For things like contacts, SMS, calls backup etc, create 1 packet for 1 db file.
 * In future, if there are plans to backup a bunch of files as one extra
 * (example external media, obb, adb keys...) then 1 packet can be used to bunch them together in one packet.
 */
class ZipExtraPacket {

    /**
     * List containing:
     * - String - path of the extra file from the backup location
     * - Long - Size of the file
     * - Boolean - If the extra is to be restored under /system
     */
    val extraFiles = ArrayList<Triple<String, Long, Boolean>>(0)

    var zipPacketSize: Long = 0
        private set

    /**
     * Calculate total size of packet.
     */
    fun refreshTotal(){
        zipPacketSize = 0
        extraFiles.forEach {
            zipPacketSize += it.second
        }
    }

    /**
     * Private function to remove extra slash from the string.
     * Also remove front and rear slash '/'
     */
    private fun addEntry(triple: Triple<String, Long, Boolean>){
        var formattedName = Misc.removeDuplicateSlashes(triple.first.trim())
        if (formattedName.startsWith('/')) formattedName = formattedName.substring(1)
        if (formattedName.endsWith('/')) formattedName = formattedName.run { substring(0, length-1) }
        extraFiles.add(Triple(formattedName, triple.second, triple.third))
    }

    /**
     * All entries must be actual files not directories.
     */
    fun addExtra(filePathFromBackupRoot: String, size: Long, toExtractInSystem: Boolean = false){
        addEntry(Triple(filePathFromBackupRoot, size, toExtractInSystem))
        refreshTotal()
    }

    /**
     * All entries must be actual files not directories.
     */
    fun addExtra(list: ArrayList<Triple<String, Long, Boolean>>){
        list.forEach { addEntry(it) }
        refreshTotal()
    }

}