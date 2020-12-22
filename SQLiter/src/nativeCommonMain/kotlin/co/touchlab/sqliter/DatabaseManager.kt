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

interface DatabaseManager{
    /**
     * Create a connection with locked access to the underlying sqlite instance. Use this
     * in most cases.
     */
    fun createMultiThreadedConnection():DatabaseConnection

    /**
     * Create a connection without locked access to the underlying sqlite instance. Use this only
     * if you are absolutely sure you're only accessing from one thread. It will proactively fail
     * if you are attempting otherwise. Performance is better, but marginally so.
     */
    fun createSingleThreadedConnection():DatabaseConnection
    val configuration:DatabaseConfiguration
}

fun <R> DatabaseManager.withConnection(block:(DatabaseConnection) -> R):R{
    val connection = createMultiThreadedConnection()
    try {
        return block(connection)
    }finally {
        connection.close()
    }
}
