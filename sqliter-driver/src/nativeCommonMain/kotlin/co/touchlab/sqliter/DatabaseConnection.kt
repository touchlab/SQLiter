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

interface DatabaseConnection {
    fun createStatement(sql: String): Statement
    fun beginTransaction()
    fun setTransactionSuccessful()
    fun endTransaction()
    fun close()
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
    withStatement("PRAGMA key = ?;"){
        bindString(1, cipherKey)
    }
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
    withStatement("PRAGMA rekey = ?;"){
        bindString(1, newKey)
    }
}

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
        JournalMode.forString(stringForQuery("PRAGMA journal_mode=${value.name}").toUpperCase())
    } else {
        value
    }
}

fun DatabaseConnection.updateForeignKeyConstraints(enabled: Boolean) {
    val newValue = if (enabled) {
        1
    } else {
        0
    }
    withStatement("PRAGMA foreign_keys=$newValue") { execute() }
}
