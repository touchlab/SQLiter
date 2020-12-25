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
import kotlin.native.concurrent.AtomicInt
import kotlin.test.*

class DatabaseManagerTest : BaseDatabaseTest(){

    @Test
    fun connectionCount(){
        val connectionCount = AtomicInt(0)
        basicTestDb(
            onCreateConnection = {connectionCount.increment()},
            onCloseConnection = {connectionCount.decrement()}
        ) {man ->
            val conn = man.surpriseMeConnection()
            assertEquals(1, connectionCount.value)
            man.withConnection {
                assertEquals(2, connectionCount.value)
            }
            assertEquals(1, connectionCount.value)
            conn.close()
            assertEquals(0, connectionCount.value)
        }
    }

    @Test
    fun createUpdateCalled(){
        val createCalled = AtomicInt(0)
        var updateCalled = AtomicInt(0)

        val config1 = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    createCalled.increment()
                    println("createCalled $createCalled")
                    execute()
                }
            },
            upgrade = { _, _, _ ->
                updateCalled.increment()
                println("updateCalled $updateCalled")
            }
        )

        val config2 = config1.copy(version = 2)

        val db1 = createDatabaseManager(config1)

        assertEquals(0, createCalled.value)
        db1.withConnection {  }
        assertEquals(1, createCalled.value)
        assertEquals(0, updateCalled.value)
        db1.withConnection {  }
        assertEquals(1, createCalled.value)

        val db2 = createDatabaseManager(config2)

        assertEquals(0, updateCalled.value)
        db2.withConnection {  }
        assertEquals(1, createCalled.value)
        assertEquals(1, updateCalled.value)

        val db3 = createDatabaseManager(config2)

        db3.withConnection {  }
        assertEquals(1, createCalled.value)
        assertEquals(1, updateCalled.value)
    }

    @Test
    fun downgradeNotAllowed(){
        val upgradeCalled = AtomicInt(0)
        val config1 = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            upgrade = { _, _, _ ->
                upgradeCalled.increment()
            }
        )

        createDatabaseManager(config1).withConnection {  }
        assertEquals(0, upgradeCalled.value)
        createDatabaseManager(config1.copy(version = 2)).withConnection {  }
        assertEquals(1, upgradeCalled.value)

        var conn: DatabaseConnection? = null
        assertFails {
            conn = createDatabaseManager(config1.copy(version = 1)).createSingleThreadedConnection()
        }

        conn?.close()
    }

    @Test
    fun failedCreateRollsBack(){
        val configFail = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                    throw Exception("rollback")
                }
            }
        )

        var createCalled = false
        val configOk = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                    createCalled = true
                }
            }
        )

        var conn: DatabaseConnection? = null
        assertFails {
            conn = createDatabaseManager(configFail).createSingleThreadedConnection()
        }
        conn?.close()

        createDatabaseManager(configOk).withConnection {  }
        assertTrue(createCalled)
    }

    @Test
    fun failedUpgradeRollsBack(){
        val config = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            upgrade = {conn,_,_ ->
                conn.withStatement("CREATE TABLE test2 (num INTEGER NOT NULL, " +
                        "str TEXT NOT NULL)"){execute()}
                conn.withStatement("insert into test2(num, str)values(?,?)"){
                    bindLong(1, 1)
                    bindString(2, "asdf")
                    executeInsert()
                }
                throw Exception("nah")
            }
        )

        //Create db
        createDatabaseManager(config).withConnection {  }

        val configUpgradeFails = config.copy(version = 2)

        //Upgrade to v2 fails
        assertFails { createDatabaseManager(configUpgradeFails).withConnection {  } }
        val configUpgrade = configUpgradeFails.copy(upgrade = {conn, from, to ->
            conn.withStatement("CREATE TABLE test2 (num INTEGER NOT NULL, " +
                    "str TEXT NOT NULL)"){execute()}
            conn.withStatement("insert into test2(num, str)values(?,?)"){
                bindLong(1, 1)
                bindString(2, "asdf")
                executeInsert()
            }
        })

        val upgraded = createDatabaseManager(configUpgrade)

        //Assert upgrade succeeds
        upgraded.withConnection {
            assertEquals(1, it.longForQuery("select count(*) from test2"))
        }
    }
}