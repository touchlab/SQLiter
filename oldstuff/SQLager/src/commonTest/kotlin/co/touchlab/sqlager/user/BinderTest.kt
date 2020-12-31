package co.touchlab.sqlager.user

import kotlin.test.Test
import kotlin.test.assertFails

class BinderTest{
    @Test
    fun namedBindings(){
        testBindingsDb { database ->
            database.insert("insert into test(ival, dval, bval, sval)" +
                    "values(:ibind, :dbind, :bbind, :sbind)"){
                long(":ibind", 123L)
                double(":dbind", 2.0)
                bytes(":bbind", null)
                string(":sbind", "two")
            }
        }
    }

    @Test
    fun namedBindingsFail(){
        testBindingsDb { database ->
            assertFails {
                database.insert(
                    "insert into test(ival, dval, bval, sval)" +
                            "values(:ibind, :dbind, :bbind, :sbind)"
                ) {
                    long(":hibind", 123L)
                }
            }
        }
    }

    @Test
    fun numericIndex(){
        testBindingsDb { database ->
            database.insert("insert into test(ival, dval, bval, sval)" +
                    "values(?, ?, ?, ?)"){
                long(1, 123L)
                double(2, 2.0)
                bytes(3, null)
                string(4, "two")
            }
        }
    }

    @Test
    fun numericIndexFail(){
        testBindingsDb { database ->
            assertFails {
                database.insert(
                    "insert into test(ival, dval, bval, sval)" +
                            "values(?, ?, ?, ?)"
                ) {
                    long(0, 123L)
                }
            }
        }
    }

    @Test
    fun noIndex(){
        testBindingsDb { database ->
            database.insert("insert into test(ival, dval, bval, sval)" +
                    "values(?, ?, ?, ?)"){
                long(123L)
                double(2.0)
                bytes(null)
                string("two")
            }
        }
    }

    @Test
    fun noIndexTooMany(){
        testBindingsDb { database ->
            assertFails {
                database.insert(
                    "insert into test(ival, dval, bval, sval)" +
                            "values(?, ?, ?, ?)"
                ) {
                    long(123L)
                    double(2.0)
                    bytes(null)
                    string("two")
                    string("two")
                }
            }
        }
    }

    @Test
    fun mixIndexAndAutoFails(){
        testBindingsDb { database ->
            assertFails {
                database.insert(
                    "insert into test(ival, dval, bval, sval)" +
                            "values(?, ?, ?, ?)"
                ) {
                    long(1, 123L)
                    double(2.0)
                }
            }
        }
    }

    private fun testBindingsDb(block:(Database)->Unit){
        testDatabase(createSql = "CREATE TABLE test (" +
                "ival INTEGER NOT NULL, " +
                "dval REAL NOT NULL, " +
                "bval BLOB, " +
                "sval TEXT NOT NULL)") {database ->
            block(database)
        }
    }
}