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

import kotlin.test.*

class DatabaseManagerTest{

    @BeforeEach
    fun before() {
        deleteDatabase(TEST_DB_NAME)
    }

    @AfterEach
    fun after() {
        deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun connectionCount(){
        basicTestDb {man ->
            val conn = man.createConnection()
            assertEquals(1, countLiveConnections(man))
            man.withConnection {
                assertEquals(2, countLiveConnections(man))
            }
            assertEquals(1, countLiveConnections(man))
            conn.close()
            assertEquals(0, countLiveConnections(man))
        }
    }

    @Test
    fun createUpdateCalled(){
        var createCalled = 0
        var updateCalled = 0

        val config1 = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    createCalled++
                    println("createCalled $createCalled")
                    execute()
                }
            },
            upgrade = { _, _, _ ->
                updateCalled++
                println("updateCalled $updateCalled")
            }
        )

        val config2 = config1.copy(version = 2)

        val db1 = createDatabaseManager(config1)

        assertEquals(0, createCalled)
        db1.withConnection {  }
        assertEquals(1, createCalled)
        assertEquals(0, updateCalled)
        db1.withConnection {  }
        assertEquals(1, createCalled)

        val db2 = createDatabaseManager(config2)

        assertEquals(0, updateCalled)
        db2.withConnection {  }
        assertEquals(1, createCalled)
        assertEquals(1, updateCalled)

        val db3 = createDatabaseManager(config2)

        db3.withConnection {  }
        assertEquals(1, createCalled)
        assertEquals(1, updateCalled)
    }

    @Test
    fun downgradeNotAllowed(){
        var upgradeCalled = 0
        val config1 = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            upgrade = { _, _, _ ->
                upgradeCalled++
            }
        )

        createDatabaseManager(config1).withConnection {  }
        assertEquals(0, upgradeCalled)
        createDatabaseManager(config1.copy(version = 2)).withConnection {  }
        assertEquals(1, upgradeCalled)
        assertFails {
            createDatabaseManager(config1.copy(version = 1)).withConnection {  }
        }
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

        assertFails { createDatabaseManager(configFail).withConnection {  } }
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

fun countLiveConnections(man:DatabaseManager):Int =
    (man as NativeDatabaseManager).connectionCount.value