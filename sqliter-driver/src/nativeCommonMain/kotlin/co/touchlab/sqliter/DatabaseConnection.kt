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

import co.touchlab.sqliter.interop.SqliteDatabasePointer

interface DatabaseConnection {
    fun rawExecSql(sql: String)
    fun createStatement(sql: String): Statement
    fun beginTransaction()
    fun setTransactionSuccessful()
    fun endTransaction()
    fun close()
    val closed:Boolean

    // Added here: https://github.com/touchlab/SQLiter/pull/73
    // I refactored a lot of the API to be internal but some clients need access to the underlying pointer.
    // This call may get moved in the future, or changed in some way, but some calling clients do need access to the
    // sqlite structures, or (possibly) some way to accomplish what the direct access is doing.
    fun getDbPointer(): SqliteDatabasePointer
}

fun <R> DatabaseConnection.withStatement(sql: String, proc: Statement.() -> R): R {
    val statement = createStatement(sql)
    try {
        return statement.proc()
    } finally {
        statement.finalizeStatement()
    }
}

fun <R> DatabaseConnection.withTransaction(proc: (DatabaseConnection) -> R): R {
    beginTransaction()
    try {
        val result = proc(this)
        setTransactionSuccessful()
        return result
    } finally {
        endTransaction()
    }
}

fun DatabaseConnection.longForQuery(sql: String): Long = withStatement(sql) {
    longForQuery()
}

fun DatabaseConnection.stringForQuery(sql: String): String = withStatement(sql) {
    stringForQuery()
}

/**
 * Sets the database cipher key.
 *
 * @param cipherKey the database cipher key
 */
fun DatabaseConnection.setCipherKey(cipherKey: String) {
    stringForQuery("PRAGMA key = '${cipherKey.escapeSql()}';")
}

/**
 * Resets the database cipher key.
 *
 * @param oldKey the old database cipher key
 * @param newKey the new database cipher key
 */
//TODO: Testing for sqlcipher
//TODO: Maybe figure out key suppress in log?
fun DatabaseConnection.resetCipherKey(oldKey: String, newKey: String) {
    setCipherKey(oldKey)
    stringForQuery("PRAGMA rekey = '${newKey.escapeSql()}';")
}

private fun String.escapeSql() = this.replace(oldValue = "'", newValue = "''")

/**
 * Gets the database version.
 *
 * @return the database version
 */
fun DatabaseConnection.getVersion(): Int = longForQuery("PRAGMA user_version;").toInt()

/**
 * Sets the database version.
 *
 * @param version the new database version
 */
fun DatabaseConnection.setVersion(version: Int) {
    withStatement("PRAGMA user_version = $version") { execute() }
}

val DatabaseConnection.journalMode: JournalMode
    get() = JournalMode.forString(stringForQuery("PRAGMA journal_mode"))

fun DatabaseConnection.updateJournalMode(value: JournalMode): JournalMode {
    return if (journalMode != value) {
        JournalMode.forString(stringForQuery("PRAGMA journal_mode=${value.name}").uppercase())
    } else {
        value
    }
}

fun DatabaseConnection.updateForeignKeyConstraints(enabled: Boolean) {
    withStatement("PRAGMA foreign_keys=${enabled.toInt()}") { execute() }
}

fun DatabaseConnection.updateSynchronousFlag(flag: SynchronousFlag) {
    withStatement("PRAGMA synchronous=${flag.value}") { execute() }
}

fun DatabaseConnection.updateRecursiveTriggers(enabled: Boolean) {
    withStatement("PRAGMA recursive_triggers=${enabled.toInt()}") { execute() }
}

private fun Boolean.toInt(): Int = if (this) 1 else 0