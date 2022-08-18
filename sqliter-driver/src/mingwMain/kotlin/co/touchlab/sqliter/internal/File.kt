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

package co.touchlab.sqliter.internal

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.DeleteFileA

internal actual class File(dirPath:String? = null, name:String) {

    actual val path: String

    actual val exists: Boolean
        get() = exists()

    init {
        if (dirPath == null || dirPath.isEmpty()) {
            this.path = fixSlashes(name)
        } else if (name.isEmpty()) {
            this.path = fixSlashes(dirPath)
        } else {
            this.path = fixSlashes(join(dirPath, name))
        }
    }

    constructor(path:String):this(name = path)

    companion object {
        /**
         * The system-dependent character used to separate components in filenames ('/').
         */
        const val separatorChar: Char = '\\'

        /**
         * The system-dependent string used to separate components in filenames ('/').
         * See [.separatorChar].
         */
        const val separator: String = "\\"

        /**
         * The system-dependent character used to separate components in search paths (':').
         * This is used to split such things as the PATH environment variable and classpath
         * system properties into lists of directories to be searched.
         */
        const val pathSeparatorChar: Char = ':'
    }

    /**
     * Constructs a new file using the specified directory and name.
     *
     * @param dir
     * the directory where the file is stored.
     * @param name
     * the file's name.
     * @throws NullPointerException
     * if `name` is `null`.
     */
    constructor(dir: File, name: String) : this(dir.path, name)

    // Removes duplicate adjacent back slashes and any trailing back slashes.
    private fun fixSlashes(origPath: String): String {
        // Remove duplicate adjacent slashes.
        var lastWasSlash = false
        val newPath:CharArray = origPath.toCharArray()
        val length = newPath.size
        var newLength = 0
        val initialIndex = if (origPath.startsWith("file://", true)) 7 else 0
        for (i in initialIndex until length) {
            val ch = newPath[i]
            if (ch == separatorChar) {
                if (!lastWasSlash) {
                    newPath[newLength++] = separatorChar
                    lastWasSlash = true
                }
            } else {
                newPath[newLength++] = ch
                lastWasSlash = false
            }
        }

        // Remove any trailing slash (unless this is the root of the file system).
        if (lastWasSlash && newLength > 1) {
            newLength--
        }

        // Reuse the original string if possible.
        return if (newLength != length) buildString(newLength) {
            append(newPath)
            setLength(newLength)
        } else origPath
    }

    /**
     * Returns the combination of the prefix and suffix with a back slash added if necessary.
     */
    private fun join(prefix: String, suffix: String): String {
        val prefixLength = prefix.length
        var haveSlash = prefixLength > 0 && prefix[prefixLength - 1] == separatorChar
        if (!haveSlash) {
            haveSlash = suffix.isNotEmpty() && suffix[0] == separatorChar
        }
        return if (haveSlash) prefix + suffix else prefix + separatorChar + suffix
    }

    /**
     * Tests whether or not this process is allowed to execute this file.
     * Note that this is a best-effort result; the only way to be certain is
     * to actually attempt the operation.
     *
     * @return `true` if this file can be executed, `false` otherwise.
     * @since 1.6
     */
    fun canExecute(): Boolean {
        return doAccess(X_OK)
    }

    /**
     * Indicates whether the current context is allowed to read from this file.
     *
     * @return `true` if this file can be read, `false` otherwise.
     */
    fun canRead(): Boolean {
        return doAccess(R_OK)
    }

    /**
     * Indicates whether the current context is allowed to write to this file.
     *
     * @return `true` if this file can be written, `false`
     * otherwise.
     */
    fun canWrite(): Boolean {
        return doAccess(W_OK)
    }

    private fun doAccess(path: String, mode: Int):Boolean {
        return access(path, mode) == 0
    }

    private fun doAccess(mode: Int): Boolean {
        return doAccess(path, mode)
    }

    /**
     * Returns the relative sort ordering of the paths for this file and the
     * file `another`. The ordering is platform dependent.
     *
     * @param another
     * a file to compare this file to
     * @return an int determined by comparing the two paths. Possible values are
     * described in the Comparable interface.
     * @see Comparable
     */
    fun compareTo(another: File): Int {
        return this.getPath().compareTo(another.getPath())
    }

    /**
     * Deletes this file. Directories must be empty before they will be deleted.
     *
     *
     * Note that this method does *not* throw `IOException` on failure.
     * Callers must check the return value.
     *
     * @return `true` if this file was deleted, `false` otherwise.
     */
    fun delete(): Boolean {
        return DeleteFileA(path) != 0
    }

    /**
     * Returns a boolean indicating whether this file can be found on the
     * underlying file system.
     *
     * @return `true` if this file exists, `false` otherwise.
     */
    fun exists(): Boolean = doAccess(F_OK)

    private fun exists(path: String): Boolean = doAccess(path, F_OK)

    /**
     * Returns the name of the file or directory represented by this file.
     *
     * @return this file's name or an empty string if there is no name part in
     * the file's path.
     */
    fun getName(): String {
        val separatorIndex = path.lastIndexOf(separator)
        return if (separatorIndex < 0) path else path.substring(separatorIndex + 1, path.length)
    }

    /**
     * Returns the pathname of the parent of this file. This is the path up to
     * but not including the last name. `null` is returned if there is no
     * parent.
     *
     * @return this file's parent pathname or `null`.
     */
    fun getParent(): String? {
        val length = path.length
        var firstInPath = 0
        if (length > 2 && path[1] == pathSeparatorChar) {
            firstInPath = 2
        }
        var index = path.lastIndexOf(separatorChar)
        if (index == -1 && firstInPath > 0) {
            index = 2
        }
        if (index == -1 || path[length - 1] == separatorChar) {
            return null
        }
        return if (path.indexOf(separatorChar) == index && path[firstInPath] == separatorChar) {
            path.substring(0, index + 1)
        } else path.substring(0, index)
    }

    /**
     * Returns a new file made from the pathname of the parent of this file.
     * This is the path up to but not including the last name. `null` is
     * returned when there is no parent.
     *
     * @return a new file representing this file's parent or `null`.
     */
    fun getParentFile(): File? {
        return getParent()?.let { File(name = it) }
    }

    /**
     * Returns the path of this file.
     */
    fun getPath(): String {
        return path
    }

    /**
     * Returns the length of this file in bytes.
     * Returns 0 if the file does not exist.
     * The result for a directory is not defined.
     *
     * @return the number of bytes in this file.
     */
    fun length(): Long {
        return memScoped {
            val statBuf = alloc<stat>()
            stat(getName(), statBuf.ptr)
            statBuf.st_size.toLong()
        }
    }

    /**
     * Returns an array of strings with the file names in the directory
     * represented by this file. The result is `null` if this file is not
     * a directory.
     *
     *
     * The entries `.` and `..` representing the current and parent
     * directory are not returned as part of the list.
     *
     * @return an array of strings with file names or `null`.
     */
    fun list(): Array<String>? {
        return listImpl(path)
    }

    private fun listImpl(path: String): Array<String>? {
        memScoped {
            val list = mutableListOf<String>()

            // Open up the directory at the given path
            val dp = opendir(path) ?: return@listImpl null

            var entry: CPointer<dirent>?

            // Loop until entry is null
            while (true) {
                entry = readdir(dp)
                if (entry == null) {
                    closedir(dp)
                    return@listImpl list.toTypedArray()
                }

                // Add the entry to our list.
                val dirent = entry[0]
                val name = dirent.d_name.toKString()
                list.add(name)
            }
        }

        // The compiler doesn't realize that this is unreachable.
        return null
    }

    /**
     * Gets a list of the files in the directory represented by this file. This
     * list is then filtered through a FilenameFilter and the names of files
     * with matching names are returned as an array of strings. Returns
     * `null` if this file is not a directory. If `filter` is
     * `null` then all filenames match.
     *
     *
     * The entries `.` and `..` representing the current and parent
     * directories are not returned as part of the list.
     *
     * @param filter
     * the filter to match names against, may be `null`.
     * @return an array of files or `null`.
     */
    fun list(filter: FilenameFilter?): Array<String>? {
        val filenames = list()
        if (filter == null || filenames == null) {
            return filenames
        }

        return filenames.filter { filter.accept(this, it) }.toTypedArray()
    }

    /**
     * Returns an array of files contained in the directory represented by this
     * file. The result is `null` if this file is not a directory. The
     * paths of the files in the array are absolute if the path of this file is
     * absolute, they are relative otherwise.
     *
     * @return an array of files or `null`.
     */
    fun listFiles(): Array<File>? {
        return filenamesToFiles(list())
    }

    /**
     * Gets a list of the files in the directory represented by this file. This
     * list is then filtered through a FilenameFilter and files with matching
     * names are returned as an array of files. Returns `null` if this
     * file is not a directory. If `filter` is `null` then all
     * filenames match.
     *
     *
     * The entries `.` and `..` representing the current and parent
     * directories are not returned as part of the list.
     *
     * @param filter
     * the filter to match names against, may be `null`.
     * @return an array of files or `null`.
     */
    fun listFiles(filter: FilenameFilter): Array<File>? {
        return filenamesToFiles(list(filter))
    }

    /**
     * Gets a list of the files in the directory represented by this file. This
     * list is then filtered through a FileFilter and matching files are
     * returned as an array of files. Returns `null` if this file is not a
     * directory. If `filter` is `null` then all files match.
     *
     *
     * The entries `.` and `..` representing the current and parent
     * directories are not returned as part of the list.
     *
     * @param filter
     * the filter to match names against, may be `null`.
     * @return an array of files or `null`.
     */
    fun listFiles(filter: FileFilter?): Array<File>? {
        val files = listFiles()
        if (filter == null || files == null) {
            return files
        }

        return files.filter { filter.accept(it) }.toTypedArray()
    }

    /**
     * Converts a String[] containing filenames to a File[].
     * Note that the filenames must not contain slashes.
     * This method is to remove duplication in the implementation
     * of File.list's overloads.
     */
    private fun filenamesToFiles(filenames: Array<String>?): Array<File>? {
        return filenames?.map { File(this, it) }?.toTypedArray()
    }

    /**
     * Creates the directory named by this file, assuming its parents exist.
     * Use [.mkdirs] if you also want to create missing parents.
     *
     *
     * Note that this method does *not* throw `IOException` on failure.
     * Callers must check the return value. Note also that this method returns
     * false if the directory already existed. If you want to know whether the
     * directory exists on return, either use `(f.mkdir() || f.isDirectory())`
     * or simply ignore the return value from this method and simply call [.isDirectory].
     *
     * @return `true` if the directory was created,
     * `false` on failure or if the directory already existed.
     */
    fun mkdir(): Boolean {
        return mkdirImpl(path)
    }

    private fun mkdirImpl(filePath: String): Boolean {
        return platform.posix.mkdir(filePath) == 0
    }

    /**
     * Creates the directory named by this file, creating missing parent
     * directories if necessary.
     * Use [.mkdir] if you don't want to create missing parents.
     *
     *
     * Note that this method does *not* throw `IOException` on failure.
     * Callers must check the return value. Note also that this method returns
     * false if the directory already existed. If you want to know whether the
     * directory exists on return, either use `(f.mkdirs() || f.isDirectory())`
     * or simply ignore the return value from this method and simply call [.isDirectory].
     *
     * @return `true` if the directory was created,
     * `false` on failure or if the directory already existed.
     */
    fun mkdirs(): Boolean {
        return mkdirs(false)
    }

    private fun mkdirs(resultIfExists: Boolean): Boolean {
        if (exists()) return resultIfExists

        val pathArray = path.split(separator).dropLast(1)
        val itemPath = StringBuilder()

        for (item in pathArray) {
            itemPath.append(item)
            val currentPath = itemPath.toString()
            if (!exists(currentPath) && !mkdirImpl(currentPath)) {
                return false
            }
            itemPath.append(separator)
        }

        return true
    }

    fun createNewFile(): Boolean {

        if (path.isEmpty()) {
            throw Exception("No such file or directory")
        }
        if (isDirectory()) {  // true for paths like "dir/..", which can't be files.
            throw Exception("Cannot create: $path")
        }

        val fileDescriptor = creat(path, S_IRWXU)

        // -1 is returned if the method failed, otherwise we get a file descriptor
        if (fileDescriptor == -1) return false

        // We have to call close, or this will hold on to a reference.
        close(fileDescriptor)

        return true
    }

    private fun stMode(): Int {
        return memScoped {
            val statBuf = alloc<stat>()
            stat(path, statBuf.ptr)
            statBuf.st_mode.toInt()
        }
    }

    fun isDirectory(): Boolean {
        return stMode() and S_IFDIR == S_IFDIR
    }

    /**
     * Indicates if this file represents a *file* on the underlying
     * file system.
     *
     * @return `true` if this file is a file, `false` otherwise.
     */
    fun isFile(): Boolean {
        return stMode() and S_IFREG == S_IFREG
    }

    /**
     * Returns a string containing a concise, human-readable description of this
     * file.
     *
     * @return a printable representation of this file.
     */
    override fun toString(): String {
        return path
    }
}

internal interface FilenameFilter {
    /**
     * Indicates if a specific filename matches this filter.
     *
     * @param dir
     * the directory in which the {@code filename} was found.
     * @param filename
     * the name of the file in {@code dir} to test.
     * @return {@code true} if the filename matches the filter
     * and can be included in the list, {@code false}
     * otherwise.
     */
    fun accept(dir:File, filename:String):Boolean
}

internal interface FileFilter {
    /**
     * Indicating whether a specific file should be included in a pathname list.
     *
     * @param pathname
     * the abstract file to check.
     * @return {@code true} if the file should be included, {@code false}
     * otherwise.
     */
    fun accept(pathname:File):Boolean
}