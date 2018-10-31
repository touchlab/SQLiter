package co.touchlab.sqliter

interface DatabaseManager{
    fun createConnection(openFlags:Int):DatabaseConnection
    fun close()
}