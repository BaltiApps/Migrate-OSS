package balti.migrate.backupEngines.containers

import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_FILE_LIST
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

data class ZipAppBatch(val zipAppPackets: ArrayList<ZipAppPacket> = ArrayList(0)) {
    var batchSystemSize : Long = 0
    var batchDataSize : Long = 0
    var zipFullSize : Long = 0
    val extrasFiles: ArrayList<File> = ArrayList(0)
    var partName = ""
    var fileList: File? = null
    init {
        zipAppPackets.forEach {
            batchDataSize += it.appPacket_z.dataSizeBytes
            batchSystemSize += it.appPacket_z.systemSizeBytes
            zipFullSize += it.zipPacketSize
        }
    }
    fun addExtras(extras: ArrayList<File>){
        extrasFiles.clear()
        extrasFiles.addAll(extras)
        extrasFiles.forEach {
            batchDataSize += it.length()
            zipFullSize += it.length()
        }
    }
    fun createFileList(destination: String){
        val fl = File("$destination/$partName", FILE_FILE_LIST)

        fl.delete()
        BufferedWriter(FileWriter(fl)).run {
            zipAppPackets.forEach { zp ->
                zp.appFiles.forEach { af ->
                    if (af.name.endsWith(".app") && af.isDirectory && zp.appPacket_z.isSystem) {
                        write("${af.name}_sys\n")
                    }
                    else write("${af.name}\n")
                }
            }
            extrasFiles.forEach {ef ->
                write("${ef.name}\n")
            }
            close()
        }

        fileList = fl
    }
}