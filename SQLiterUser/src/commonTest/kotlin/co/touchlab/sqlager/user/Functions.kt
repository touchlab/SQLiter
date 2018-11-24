package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager

expect fun createDatabaseManager(config:DatabaseConfiguration):DatabaseManager