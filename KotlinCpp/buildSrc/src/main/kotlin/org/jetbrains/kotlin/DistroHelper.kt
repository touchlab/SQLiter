package org.jetbrains.kotlin

import org.apache.commons.io.FileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.net.URL
import java.util.*

class DistroHelper{
    /*companion object{
        fun downloadKotlinNativeDistro(path:String, dstFile:File){
            val url = URL(path)
            FileUtils.copyURLToFile(url, dstFile)
        }
    }*/


}

fun downloadKotlinNativeDistro(path:String, dstFile:File){
    val url = URL(path)
    FileUtils.copyURLToFile(url, dstFile)
}

fun distroPath(unzipFolder: File): File{
    val folders: Array<File> = unzipFolder
            .listFiles { pathname -> pathname?.name?.contains("kotlin-native") ?: false }

    Arrays.sort(folders)

    return folders.last()
}

fun unzipFileToFolder(zipFile: File, extractFolder: File){
    ZipUtil.unpack(zipFile, extractFolder)
}