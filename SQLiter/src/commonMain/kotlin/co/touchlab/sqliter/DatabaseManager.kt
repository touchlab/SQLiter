package co.touchlab.sqliter

interface DatabaseManager{
    fun createConnection():DatabaseConnection
    fun close()
}

expect fun createDatabaseManager(configuration: DatabaseConfiguration):DatabaseManager
expect fun deleteDatabase(name:String)

fun <R> DatabaseManager.withConnection(block:(DatabaseConnection) -> R):R{
    val connection = createConnection()
    try {
        return block(connection)
    }finally {
        connection.close()
    }
}
