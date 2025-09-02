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

class NativeCursorTest : BaseDatabaseTest(){

    @Test
    fun colNames(){
        withSample2Col { conn ->
            conn.withStatement("select * from test") {
                val cursor = query()

                assertEquals(2, cursor.columnCount)
            }
        }
    }

    private fun withSample2Col(block: (DatabaseConnection) -> Unit) {
        val manager = createDb()
        manager.withConnection { conn ->
            conn.withStatement("insert into test(num, str)values(?,?)") {
                bindLong(1, 22)
                bindString(2, "asdf")
                executeInsert()
                bindLong(1, 33)
                bindString(2, "qwert")
                executeInsert()
                1
            }

            block(conn)
        }
    }

    private fun createDb() =
        createDatabaseManager(
            DatabaseConfiguration(
                name = TEST_DB_NAME,
                version = 1,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()
                    }
                },
                journalMode = JournalMode.WAL,
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 30000),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )

}