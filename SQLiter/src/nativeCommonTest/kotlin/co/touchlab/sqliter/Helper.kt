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

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.waitForMultipleFutures

fun createTestDb(name:String = "testdb", version:Int = 1, create:(DatabaseConnection)->Unit):DatabaseManager{
    try {
        deleteDatabase(name)
    } catch (e: Exception) {
    }
    return createDatabaseManager(DatabaseConfiguration(name, version, create))
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

inline fun basicTestDb(createSql:String = FOUR_COL, block:(DatabaseManager)->Unit){
    val dbname = "testdb"
    val dbManager = createTestDb {db ->
        db.withStatement(createSql){
            execute()
        }
    }

    deleteAfter(dbname, dbManager, block)
}

fun <T> Collection<Future<T>>.waitForAllFutures() {
    var consumed = 0
    while (consumed < this.size) {
        val ready = this.waitForMultipleFutures(10000)
        ready.forEach {
            it.consume { result ->
                consumed++
            }
        }
    }
}