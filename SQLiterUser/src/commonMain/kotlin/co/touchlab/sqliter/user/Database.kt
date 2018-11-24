package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseManager

class Database(private val databaseManager: DatabaseManager){
    private val instance = DatabaseInstance(databaseManager.createConnection())

    fun close(){
        instance.close()
    }

    fun instance():DatabaseInstance = instance
}