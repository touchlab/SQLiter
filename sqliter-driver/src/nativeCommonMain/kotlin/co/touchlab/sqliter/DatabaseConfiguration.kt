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

import co.touchlab.sqliter.interop.Logger

/**
 * The database manager will skip version checks, create, and update when this is set. This is useful if you
 * need to do some kind of operation on a db without initializing it. For example, converting a clear text db
 * to an encrypted db.
 *
 * Using this value is a bit of a hack. The next major version will likely include a refactor of config.
 *
 * User version is usually positive, but there are no enforced restrictions from the sqlite side. Using an "uncommon"
 * negative number in case somebody uses negatives for some reason.
 */
const val NO_VERSION_CHECK = -50_001

data class DatabaseConfiguration(
    val name: String?,
    val version: Int,
    val create: (DatabaseConnection) -> Unit,
    val upgrade: (DatabaseConnection, Int, Int) -> Unit = { _, _, _ -> },
    val inMemory: Boolean = false,
    val journalMode: JournalMode = JournalMode.WAL,
    val extendedConfig:Extended = Extended(),
    val loggingConfig:Logging = Logging(),
    val lifecycleConfig:Lifecycle = Lifecycle(),
    val encryptionConfig:Encryption = Encryption()
) {
    data class Extended(
        val foreignKeyConstraints: Boolean = false,
        val busyTimeout: Int = 5000,
        val pageSize: Int? = null,
        val basePath: String? = null,
        val synchronousFlag: SynchronousFlag? = null,
        val recursiveTriggers: Boolean = false,
        val lookasideSlotSize: Int = -1,
        val lookasideSlotCount: Int = -1,
    )
    data class Logging(
        val logger: Logger = WarningLogger,
        val verboseDataCalls: Boolean = false
    )
    data class Lifecycle(
        val onCreateConnection: (DatabaseConnection) -> Unit = { _ -> },
        val onCloseConnection: (DatabaseConnection) -> Unit = { _ -> },
    )
    data class Encryption(
        val key: String? = null,
        val rekey: String? = null,
    )
    init {
        checkFilename(name)
    }
}

internal object NoneLogger : Logger {
    override fun trace(message: String) = Unit

    override val vActive: Boolean = false

    override fun vWrite(message: String) = Unit

    override val eActive: Boolean = false

    override fun eWrite(message: String, exception: Throwable?) = Unit
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
            if (s.uppercase() == WAL.name) {
                WAL
            } else {
                DELETE
            }
    }
}

enum class SynchronousFlag(val value: Int) {
    OFF(0), NORMAL(1), FULL(2), EXTRA(3);
}
