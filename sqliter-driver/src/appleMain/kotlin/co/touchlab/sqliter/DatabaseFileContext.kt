/*
 * Copyright (C) 2018 Touchlab, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.sqliter

import co.touchlab.sqliter.internal.File
import co.touchlab.sqliter.internal.FileFilter
import kotlinx.cinterop.UnsafeNumber
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual object DatabaseFileContext{
    actual fun deleteDatabase(name: String, basePath:String?) {
        deleteDatabaseFile(databaseFile(name, basePath))
    }

    actual fun databasePath(databaseName:String, datapathPath:String?):String{
        return databaseFile(databaseName, datapathPath).path
    }

    internal fun databaseDirPath():String = iosDirPath("databases")

    @OptIn(UnsafeNumber::class)
    internal fun iosDirPath(folder:String):String{
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true);
        val documentsDirectory = paths[0] as String;

        val databaseDirectory = "$documentsDirectory/$folder"

        val fileManager = NSFileManager.defaultManager()

        if (!fileManager.fileExistsAtPath(databaseDirectory))
            fileManager.createDirectoryAtPath(databaseDirectory, true, null, null); //Create folder

        return databaseDirectory
    }

    internal actual fun databaseFile(databaseName:String, datapathPath:String?):File = File(datapathPath?:databaseDirPath(), databaseName)

    internal fun deleteDatabaseFile(file:File):Boolean {
        var deleted = false
        deleted = deleted or file.delete()
        deleted = deleted or File(file.getPath() + "-journal").delete()
        deleted = deleted or File(file.getPath() + "-shm").delete()
        deleted = deleted or File(file.getPath() + "-wal").delete()

        //TODO: Implement file list
        val dir = file.getParentFile()
        if (dir != null)
        {
            val prefix = file.getName() + "-mj"
            val files = dir.listFiles(object: FileFilter {
                override fun accept(pathname: File):Boolean {
                    return pathname.getName().startsWith(prefix)
                }
            })
            if (files != null)
            {
                for (masterJournal in files)
                {
                    deleted = deleted or masterJournal.delete()
                }
            }
        }
        return deleted
    }
}
