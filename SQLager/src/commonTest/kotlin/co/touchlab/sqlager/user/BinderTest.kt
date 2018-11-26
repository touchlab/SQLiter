package co.touchlab.sqlager.user

import kotlin.test.Test

class BinderTest{
    @Test
    fun namedBindings(){
        testDatabase(createSql = "CREATE TABLE test (" +
                "ival INTEGER NOT NULL, " +
                "dval REAL NOT NULL, " +
                "bval BLOB, " +
                "sval TEXT NOT NULL)") {database ->
            database.insert("insert into test(ival, dval, bval, sval)" +
                    "values(:ibind, :dbind, :bbind, :sbind)"){
                long(123L, name = ":lbind")
                double(2.0)
//                bytes(it)
                string("two")
            }
        }
    }
}