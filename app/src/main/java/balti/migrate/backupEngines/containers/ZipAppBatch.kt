package balti.migrate.backupEngines.containers

import balti.filex.FileX
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_FILE_LIST

data class ZipAppBatch(val zipAppPackets: ArrayList<ZipAppPacket> = ArrayList(0)) {
    var batchSystemSize : Long = 0
    var batchDataSize : Long = 0
    var zipFullSize : Long = 0
    val extrasFiles: ArrayList<FileX> = ArrayList(0)
    var partName = ""
    var fileList: FileX? = null
    init {
        zipAppPackets.forEach {
            batchDataSize += it.appPacket_z.dataSizeBytes
            batchSystemSize += it.appPacket_z.systemSizeBytes
            zipFullSize += it.zipPacketSize
        }
    }
    fun addExtras(extras: ArrayList<FileX>){
        extrasFiles.clear()
        extrasFiles.addAll(extras)
        extrasFiles.forEach {
            batchDataSize += it.length()
            zipFullSize += it.length()
        }
    }
    fun createFileList(destination: String){
        val fl = if (partName.isNotBlank()) FileX.new("$destination/$partName", FILE_FILE_LIST)
        else FileX.new(destination, FILE_FILE_LIST)

        fl.delete()
        fl.startWriting(object : FileX.Writer(){
            override fun writeLines() {
                zipAppPackets.forEach { zp ->
                    zp.appFileNames.forEach { name ->
                        if (name.endsWith(".app")) {
                            if (zp.appPacket_z.isSystem) {
                                writeLine("${name}_sys")
                            }
                            else writeLine(name)
                        }
                        else writeLine(name)
                    }
                }
                extrasFiles.forEach {ef ->
                    writeLine(ef.name)
                }
            }
        })

        fileList = fl
    }
}