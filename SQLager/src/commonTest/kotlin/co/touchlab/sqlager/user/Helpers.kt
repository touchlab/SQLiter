package co.touchlab.sqlager.user

val TWO_COL = "CREATE TABLE test (num INTEGER NOT NULL, " +
        "str TEXT NOT NULL)"

var dbCount = 0

/**
 * If the database doesn't close right we'll screw up the rest of the tests, so we need different
 * names
 */
fun makeDbName() = "testdb${dbCount++}"