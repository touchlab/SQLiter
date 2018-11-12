package co.touchlab.sqliter

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WALTest{
    @Test
    fun playWithWal(){
        try {
            deleteDatabase("normaldb")
            deleteDatabase("manualdb")
        }catch (e:Throwable)
        {
            e.printStackTrace()
        }

        val configuration = DatabaseConfiguration("asdf", 1, {
            it.withStatement(TWO_COL){it.execute()}
            it.withStatement("insert into test(num, str)values(?, ?)"){
                it.bindLong(1, 123)
                it.bindString(2, "rrr")
                it.executeInsert()
            }
        })
        val databasePath = getDatabasePath("normaldb")

        val manager = NativeDatabaseManager(databasePath.path, configuration)
        val conn = manager.createConnection()
        conn.close()

//        assertFalse { databasePath.listFiles()!!.any { it.getName().endsWith("wal") } }

        val configAutooff = configuration.copy(walAutocheckpoint = 0)

        val databaseAutooffPath = getDatabasePath("manualdb")

        val managerAutooff = NativeDatabaseManager(databaseAutooffPath.path, configAutooff)
        val connAutooff = managerAutooff.createConnection()
        connAutooff.close()

//        assertTrue { databaseAutooffPath.listFiles()!!.any { it.getName().endsWith("wal") } }

        println("Out in dir ${databaseAutooffPath.path}")
    }
}