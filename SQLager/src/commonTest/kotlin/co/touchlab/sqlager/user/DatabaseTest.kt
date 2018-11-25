package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseTest{
    @Test
    fun testInstances(){
        val man = createDatabaseManager(DatabaseConfiguration(
            name = makeDbName(),
            version = 1,
            inMemory = true,
            create = {
                val instance = wrapDatabaseInstance(it)
                instance.execute(TWO_COL)
            }
        ))

        val database = Database(databaseManager = man, instances = 3)

        for(i in 0 until 10){
            database.insert("insert into test(num, str)values(?, ?)"){
                long(i.toLong())
                string("Val $i")
            }
        }

        assertEquals(3, database.databaseInstances.size)
        assertTrue(database.close())
        database.databaseInstances.forEach { assertTrue { it.closed.value } }
    }

}