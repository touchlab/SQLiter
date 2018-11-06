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
            it.execute()
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