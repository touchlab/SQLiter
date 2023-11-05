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

internal actual class File(dirPath: String? = null, name: String) {

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

    constructor(path: String) : this(name = path)

    companion object {
        /**
         * The system-dependent character used to separate components in filenames ('/').
         * Use of this (rather than hard-coding '/') helps portability to other operating systems.
         *
         *
         * This field is initialized from the system property "file.separator".
         * Later changes to that property will have no effect on this field or this class.
         */
        const val separatorChar: Char = '/'

        /**
         * The system-dependent string used to separate components in filenames ('/').
         * See [.separatorChar].
         */
        const val separator: String = "/"

        /**
         * The system-dependent character used to separate components in search paths (':').
         * This is used to split such things as the PATH environment variable and classpath
         * system properties into lists of directories to be searched.
         *
         *
         * This field is initialized from the system property "path.separator".
         * Later changes to that property will have no effect on this field or this class.
         */
        const val pathSeparatorChar: Char = ':'

        /**
         * The system-dependent string used to separate components in search paths (":").
         * See [.pathSeparatorChar].
         */
        const val pathSeparator: String = ":"

        private const val caseSensitive: Boolean = true
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

    // Removes duplicate adjacent slashes and any trailing slash.
    private fun fixSlashes(origPath: String): String {
        // Remove duplicate adjacent slashes.
        var lastWasSlash = false
        val newPath: CharArray = origPath.toCharArray()
        val length = newPath.size
        var newLength = 0
        val initialIndex = if (origPath.startsWith("file://", true)) 7 else 0
        for (i in initialIndex until length) {
            val ch = newPath[i]
            if (ch == '/') {
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

    private fun join(prefix: String, suffix: String): String {
        val prefixLength = prefix.length
        var haveSlash = prefixLength > 0 && prefix[prefixLength - 1] == separatorChar
        if (!haveSlash) {
            haveSlash = suffix.length > 0 && suffix[0] == separatorChar
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

    private fun doAccess(mode: Int): Boolean {
        return access(path, mode) == 0
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
        return if (caseSensitive) {
            this.getPath().compareTo(another.getPath())
        } else this.getPath().compareTo(another.getPath())
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
        return remove(path) == 0
    }

    /**
     * Returns a boolean indicating whether this file can be found on the
     * underlying file system.
     *
     * @return `true` if this file exists, `false` otherwise.
     */
    fun exists(): Boolean = doAccess(F_OK)

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
        if (separatorChar == '\\' && length > 2 && path[1] == ':') {
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
        val tempParent = getParent()
        return if (tempParent == null)
            null
        else
            File(name = tempParent)
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
    fun length(): Long = memScoped {
        val st = alloc<stat>()

        if (stat(path, st.ptr) != F_OK) {
            return 0
        }

        return st.st_size
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
        val dir = opendir(path)

        if (dir == NULL) {
            return null
        }

        val paths = mutableListOf<String>()
        var dirEntry = readdir(dir)
        while (dirEntry != NULL && dirEntry != null) {
            paths.add(dirEntry.pointed.d_name.toKString())
            dirEntry = readdir(dir)
        }
        closedir(dir)

        return paths.toTypedArray()
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
        val result = ArrayList<String>(filenames.size)
        for (filename in filenames) {
            if (filter.accept(this, filename)) {
                result.add(filename)
            }
        }
        return result.toTypedArray()
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
        val result = ArrayList<File>(files.size)
        for (file in files) {
            if (filter.accept(file)) {
                result.add(file)
            }
        }
        return result.toTypedArray()
    }

    /**
     * Converts a String[] containing filenames to a File[].
     * Note that the filenames must not contain slashes.
     * This method is to remove duplication in the implementation
     * of File.list's overloads.
     */
    private fun filenamesToFiles(filenames: Array<String>?): Array<File>? {
        if (filenames == null) {
            return null
        }
        val count = filenames.size

        return Array(count) { i -> File(this, filenames[i]) }
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
        return mkdir(filePath, S_IRWXU.toUInt()) == F_OK
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

    @Suppress("UNUSED_PARAMETER")
    private fun mkdirs(resultIfExists: Boolean): Boolean {
        /* If the terminal directory already exists, answer false */
        if (exists()) {
            return false
        }

        var pathTmp = if (path.startsWith(separator)) "/" else ""
        path.split(separator).forEach { pathSegment ->
            pathTmp += pathSegment
            if (!mkdirImpl(pathTmp)) {
                return false
            }
        }

        return true
    }

    fun createNewFile(): Boolean {
        if (0 == path.length) {
            throw Exception("No such file or directory")
        }
        if (isDirectory()) {  // true for paths like "dir/..", which can't be files.
            throw Exception("Cannot create: $path")
        }
        val file = fopen(path, "w+") //overwrites existing files
        if (file == NULL) {
            return false
        }
        fclose(file)
        return true
    }

    fun isDirectory(): Boolean = isDir(path)

    private fun isDir(path: String): Boolean = memScoped {
        val st = alloc<stat>()
        return stat(path, st.ptr) == F_OK && st.st_mode and S_IFMT.toUInt() == S_IFDIR.toUInt()
    }

    /**
     * Indicates if this file represents a *file* on the underlying
     * file system.
     *
     * @return `true` if this file is a file, `false` otherwise.
     */
    fun isFile(): Boolean = memScoped {
        val st = alloc<stat>()
        return stat(path, st.ptr) == F_OK && st.st_mode and S_IFMT.toUInt() == S_IFREG.toUInt()
    }


    /**
     * Renames this file to `newPath`. This operation is supported for both
     * files and directories.
     *
     *
     * Many failures are possible. Some of the more likely failures include:
     *
     *  * Write permission is required on the directories containing both the source and
     * destination paths.
     *  * Search permission is required for all parents of both paths.
     *  * Both paths be on the same mount point. On Android, applications are most likely to hit
     * this restriction when attempting to copy between internal storage and an SD card.
     *
     *
     *
     * Note that this method does *not* throw `IOException` on failure.
     * Callers must check the return value.
     *
     * @param newPath the new path.
     * @return true on success.
     */
    fun renameTo(newPath: File): Boolean {
        return rename(path, newPath.path) == F_OK
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
    fun accept(dir: File, filename: String): Boolean
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
    fun accept(pathname: File): Boolean
}
