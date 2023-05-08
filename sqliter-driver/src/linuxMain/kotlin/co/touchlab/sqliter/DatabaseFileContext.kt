package co.touchlab.sqliter

import co.touchlab.sqliter.internal.File
import co.touchlab.sqliter.internal.FileFilter
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.NULL
import platform.posix.PATH_MAX
import platform.posix.getcwd
import platform.posix.getenv

actual object DatabaseFileContext {
    actual fun deleteDatabase(name: String, basePath: String?) {
        deleteDatabaseFile(databaseFile(name, basePath))
    }

    actual fun databasePath(databaseName: String, datapathPath: String?): String {
        return databaseFile(databaseName, datapathPath).path
    }

    internal fun databaseDirPath(): String {
        return getHomeDirPath() ?: memScoped {
            val buff = ByteArray(PATH_MAX)
            if (getcwd(buff.refTo(0), buff.size.toULong()) != NULL) {
                return buff.toKString()
            }
            throw IllegalStateException("Cannot get home dir or current dir")
        }
    }

    internal fun getHomeDirPath(): String? = getenv("HOME")?.toKString()

    internal actual fun databaseFile(databaseName: String, datapathPath: String?): File =
        File(datapathPath ?: databaseDirPath(), databaseName)

    internal fun deleteDatabaseFile(file: File): Boolean {
        var deleted = false
        deleted = deleted or file.delete()
        deleted = deleted or File(file.getPath() + "-journal").delete()
        deleted = deleted or File(file.getPath() + "-shm").delete()
        deleted = deleted or File(file.getPath() + "-wal").delete()

        //TODO: Implement file list
        val dir = file.getParentFile()
        if (dir != null) {
            val prefix = file.getName() + "-mj"
            val files = dir.listFiles(object : FileFilter {
                override fun accept(pathname: File): Boolean {
                    return pathname.getName().startsWith(prefix)
                }
            })
            if (files != null) {
                for (masterJournal in files) {
                    deleted = deleted or masterJournal.delete()
                }
            }
        }
        return deleted
    }
}