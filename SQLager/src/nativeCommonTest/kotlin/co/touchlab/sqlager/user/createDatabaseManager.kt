package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.NativeFileContext

actual fun createDatabaseManager(config: DatabaseConfiguration): DatabaseManager =
    co.touchlab.sqliter.createDatabaseManager(config)

actual fun deleteDatabase(name:String){
    NativeFileContext.deleteDatabase(name)
}