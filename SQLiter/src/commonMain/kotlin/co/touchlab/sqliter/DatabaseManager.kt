package co.touchlab.sqliter

interface DatabaseManager{
    fun createConnection(openFlags:Int):DatabaseConnection
    fun close()
}

interface DatabaseMigration{
    abstract fun onCreate(db: DatabaseConnection)
    abstract fun onUpgrade(db: DatabaseConnection, oldVersion: Int, newVersion: Int)
}