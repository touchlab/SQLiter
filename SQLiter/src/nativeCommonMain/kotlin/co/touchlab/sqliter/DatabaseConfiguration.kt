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

import sql.Logger

data class DatabaseConfiguration(
    val name: String?,
    val version: Int,
    val create: (DatabaseConnection) -> Unit,
    val upgrade: (DatabaseConnection, Int, Int) -> Unit = { _, _, _ -> },
    val journalMode: JournalMode = JournalMode.WAL,
    val foreignKeyConstraints: Boolean = false,
    val busyTimeout: Int = 2500,
    val pageSize: Int? = null,
    val inMemory: Boolean = false,
    val basePath: String? = null,
    val key: String? = null,
    val rekey: String? = null,
    val onCreateConnection: (DatabaseConnection) -> Unit = { _ -> },
    val onCloseConnection: (DatabaseConnection) -> Unit = { _ -> },
    val logger: Logger = WarningLogger,
    val verboseDataCalls: Boolean = false
) {
    init {
        checkFilename(name)
    }
}

internal object WarningLogger : Logger {
    override fun trace(message: String) {
        println(message)
    }

    override val vActive: Boolean = false

    override fun vWrite(message: String) {
        println(message)
    }

    override val eActive: Boolean = true

    override fun eWrite(message: String, exception: Throwable?) {
        println(message)
        exception?.printStackTrace()
    }

}

private fun checkFilename(name: String?) {
    if (name != null && name.contains("/")) {
        throw IllegalArgumentException(
            "File $name contains a path separator"
        )
    }
}

enum class JournalMode {
    DELETE, WAL;

    companion object {
        fun forString(s: String) =
            if (s.toUpperCase().equals(WAL.name)) {
                WAL
            } else {
                DELETE
            }
    }
}
