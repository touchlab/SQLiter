package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.journalMode
import co.touchlab.sqliter.withConnection
import kotlin.test.assertEquals

val TWO_COL = "CREATE TABLE test (num INTEGER NOT NULL, str TEXT NOT NULL)"

var dbCount = 0

/**
 * If the database doesn't close right we'll screw up the rest of the tests, so we need different
 * names
 */
fun makeDbName() = "testdb${dbCount++}"

fun makeTen(block:(Database)->Unit){
    testDatabase {database ->
        database.transaction {
            database.useStatement("insert into test(num, str)values(?, ?)"){
                for(i in 0 until 10){
                    int(i)
                    string("Row $i")
                    insert()
                }
            }
        }

        block(database)
    }
}

fun testDatabase(instances:Int = 1,
                 journalMode: JournalMode = JournalMode.WAL,
                 createSql: String = TWO_COL,
                 inMemory:Boolean = true, block:(Database)->Unit){
    val dbName = makeDbName()
    deleteDatabase(dbName)
    val man = createDatabaseManager(DatabaseConfiguration(
        name = dbName,
        version = 1,
        inMemory = inMemory,
        journalMode = journalMode,
        busyTimeout = 10000,
        create = {
            val instance = wrapDatabaseInstance(it)
            instance.execute(createSql)
        }
    ))

    try {
        block(Database(databaseManager = man, instances = instances))
        man.withConnection {
            if(!inMemory)
                assertEquals(journalMode, it.journalMode)
        }
    } finally {
        deleteDatabase(dbName)
    }
}