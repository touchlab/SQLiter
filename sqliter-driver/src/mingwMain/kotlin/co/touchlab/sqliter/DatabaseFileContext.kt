package co.touchlab.sqliter

import co.touchlab.sqliter.internal.File
import co.touchlab.sqliter.internal.FileFilter
import co.touchlab.sqliter.internal.Utils

actual object DatabaseFileContext {
    actual fun deleteDatabase(name: String, basePath:String?) {
        deleteDatabaseFile(databaseFile(name, basePath))
    }

    actual fun databasePath(databaseName:String, datapathPath:String?):String {
        return databaseFile(databaseName, datapathPath).path
    }

    internal actual fun databaseFile(databaseName:String, datapathPath:String?):File {
        return File(datapathPath ?: Utils.getUserDirectory(), databaseName)
    }

    internal fun deleteDatabaseFile(file:File):Boolean {
        var deleted = false
        deleted = deleted or file.delete()
        deleted = deleted or File(file.getPath() + "-journal").delete()
        deleted = deleted or File(file.getPath() + "-shm").delete()
        deleted = deleted or File(file.getPath() + "-wal").delete()

        val dir = file.getParentFile()
        if (dir != null) {
            val prefix = file.getName() + "-mj"
            val files = dir.listFiles(object: FileFilter {
                override fun accept(pathname: File):Boolean {
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