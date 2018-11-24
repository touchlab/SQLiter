package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseManager

class Database(databaseManager: DatabaseManager, cacheSize: Int = 200){
    private val instance = DatabaseInstance(databaseManager.createConnection(), cacheSize)

    fun close():Boolean = instance.close()

    fun instance():DatabaseInstance = instance
}