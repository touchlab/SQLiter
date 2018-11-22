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

import platform.posix.usleep
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertEquals

class MultithreadedTest{
    /*@Test
    fun journalMultipleInsert(){
        basicTestDb(TWO_COL){manager ->

            val connection = manager.createConnection()
//            connection.journalMode = JournalMode.WAL

            val worker = Worker.start()
            val future = worker.execute(TransferMode.SAFE, { manager.freeze() }) {
                val connection = it.createConnection()
                connection.withTransaction {
                    it.withStatement("insert into test(num, str)values(?,?)") {

                        usleep(500_000)
                        for (i in 0 until 10_000) {
                            it.bindLong(1, i.toLong())
                            it.bindString(2, "Oh $i")
                            it.executeInsert()
                        }
                    }
                }
            }

            connection.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)"){
                    for (i in 0 until 10_000){
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "Hey $i")
                        it.executeInsert()
                    }
                    usleep(9_000_000)
                    for (i in 0 until 10_000){
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "Hey $i")
                        it.executeInsert()
                    }
                }
            }

            future.consume { println("We got here") }

            connection.withStatement("select count(*) from test"){
                assertEquals(30_000, it.query().forLong())
            }
        }

    }*/
}