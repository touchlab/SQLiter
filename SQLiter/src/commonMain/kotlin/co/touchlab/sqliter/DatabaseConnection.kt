package co.touchlab.sqliter

interface DatabaseConnection{
    fun createStatement(sql:String):Statement
    fun beginTransaction()
    fun setTransactionSuccessful()
    fun endTransaction()
    fun close()
}

fun <R> DatabaseConnection.withStatement(sql: String, proc: (Statement) -> R): R {
    val statement = createStatement(sql)
    try{
        return proc(statement)
    }
    finally {
        statement.finalize()
    }
}

fun <R> DatabaseConnection.withTransaction(proc: (DatabaseConnection) -> R): R {
    beginTransaction()
    try{
        val result = proc(this)
        setTransactionSuccessful()
        return result
    }finally {
        endTransaction()
    }
}

fun DatabaseConnection.longForQuery(sql:String):Long = withStatement(sql){
    val query = it.query()
    query.next()
    return@withStatement query.getLong(0)
}

fun DatabaseConnection.stringForQuery(sql:String):String = withStatement(sql){
    val query = it.query()
    query.next()
    return@withStatement query.getString(0)
}



val DatabaseConnection.journalMode: JournalMode
    get() = JournalMode.forString(stringForQuery("PRAGMA journal_mode"))

fun DatabaseConnection.updateJournalMode(value: JournalMode):JournalMode{
    return if(journalMode != value) {
        JournalMode.forString(stringForQuery("PRAGMA journal_mode=${value.name}").toUpperCase())
    }else{
        value
    }
}