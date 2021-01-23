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

import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.waitForMultipleFutures

val TEST_DB_NAME = "testdb"

fun createTestDb(
    name:String = TEST_DB_NAME,
    version:Int = 1,
    timeout:Int? = null,
    update:(DatabaseConnection, Int, Int)->Unit = {_,_,_->},
    onCreateConnection: (DatabaseConnection) -> Unit = { _ -> },
    onCloseConnection: (DatabaseConnection) -> Unit = { _ -> },
    create:(DatabaseConnection)->Unit
    ):DatabaseManager{
    try {
        deleteDatabase(name)
    } catch (e: Exception) {
    }
    var extended = DatabaseConfiguration.Extended()
    if(timeout != null)
        extended = extended.copy(busyTimeout = timeout)

    return createDatabaseManager(DatabaseConfiguration(
        name,
        version,
        create,
        update,
        lifecycleConfig = DatabaseConfiguration.Lifecycle(
            onCreateConnection = onCreateConnection,
            onCloseConnection = onCloseConnection
        ),
        extendedConfig = extended
    ))
}

inline fun deleteAfter(name: String, manager: DatabaseManager, block:(DatabaseManager)->Unit){
    try {
        block(manager)
    }
    finally {
        deleteDatabase(name)
    }
}

val TWO_COL = "CREATE TABLE test (num INTEGER NOT NULL, " +
        "str TEXT NOT NULL)"

val FOUR_COL = "CREATE TABLE test (num INTEGER NOT NULL, " +
        "str TEXT NOT NULL, " +
        "anotherStr TEXT," +
        "rrr TEST NOT NULL)"

fun basicTestDb(
    createSql: String = FOUR_COL,
    timeout: Int? = null,
    onCreateConnection: (DatabaseConnection) -> Unit = { _ -> },
    onCloseConnection: (DatabaseConnection) -> Unit = { _ -> },
    block: (DatabaseManager) -> Unit
) {
    val dbname = TEST_DB_NAME
    val dbManager = createTestDb(
        timeout = timeout,
        onCreateConnection = onCreateConnection,
        onCloseConnection = onCloseConnection
    ) { db ->
        db.withStatement(createSql) {
            execute()
        }
    }

    deleteAfter(dbname, dbManager, block)
}

fun <T> Collection<Future<T>>.waitForAllFutures() {
    forEach { f ->
        try {
            f.result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * For connections where we don't care about threading, randomize(ish)
 */
fun DatabaseManager.surpriseMeConnection():DatabaseConnection {
    return if(currentTimeMillis() %2 == 0L){
        createMultiThreadedConnection()
    }else{
        createSingleThreadedConnection()
    }
}