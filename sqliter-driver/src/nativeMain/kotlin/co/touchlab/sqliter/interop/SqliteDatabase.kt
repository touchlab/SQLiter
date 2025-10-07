package co.touchlab.sqliter.interop

import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.*
import co.touchlab.sqliter.sqlite3.*

internal class SqliteDatabase(path: String, label: String, val logger: Logger, private val verboseDataCalls: Boolean, val dbPointer: SqliteDatabasePointer) {
    val config = SqliteDatabaseConfig(path, label)

    fun prepareStatement(sqlString: String): SqliteStatement {
        val statement = memScoped {
            val statementPtr = alloc<CPointerVar<sqlite3_stmt>>()
            val sqlUgt16 = sqlString.wcstr
            val err = sqlite3_prepare16_v2(
                dbPointer,
                sqlUgt16.ptr,
                sqlUgt16.size, statementPtr.ptr, null
            )

            if (err != SQLITE_OK) {
                val error = sqlite3_errmsg(dbPointer)?.toKString()

                throw sqlException(logger, config, "error while compiling: $sqlString\n$error", err)
            }

            statementPtr.value!!
        }

        logger.v { "prepareStatement for [$statement] on $config" }

        val rawStatement = ActualSqliteStatement(this, statement)
        return if(verboseDataCalls){
            TracingSqliteStatement(logger, rawStatement)
        } else {
            rawStatement
        }
    }

    fun rawExecSql(sqlString: String){
        val err = sqlite3_exec(dbPointer, sqlString, null, null, null)
        if (err != SQLITE_OK) {
            val error = sqlite3_errmsg(dbPointer)?.toKString()
            throw sqlException(logger, config, "error rawExecSql: $sqlString, ${error?:""}", err)
        }
    }

    fun close(){
        logger.v { "close $config" }

        val err = sqlite3_close_v2(dbPointer)
        if (err != SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            throw sqlException(logger, config, "sqlite3_close($dbPointer) failed", err)
        }
    }
}

internal data class SqliteDatabaseConfig(val path:String, val label:String)

internal enum class OpenFlags {
    CREATE_IF_NECESSARY,
    OPEN_READONLY
}

@Suppress("UNUSED_PARAMETER")
internal fun dbOpen(
    path: String,
    openFlags: List<OpenFlags>,
    label: String,
    enableTrace: Boolean,
    enableProfile: Boolean,
    lookasideSlotSize: Int,
    lookasideSlotCount: Int,
    busyTimeout: Int,
    logging: Logger,
    verboseDataCalls: Boolean
): SqliteDatabase {

    val sqliteFlags = if (openFlags.contains(OpenFlags.CREATE_IF_NECESSARY)) {
        SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE;
    } else if (openFlags.contains(OpenFlags.OPEN_READONLY)) {
        SQLITE_OPEN_READONLY;
    } else {
        SQLITE_OPEN_READWRITE;
    } or SQLITE_OPEN_URI // This ensures that regardless of how sqlite was compiled it will support uri file paths.
// this is important for using in memory databases.

    val db = memScoped {
        val dbPtr = alloc<CPointerVar<sqlite3>>()
        val openResult = sqlite3_open_v2(path, dbPtr.ptr, sqliteFlags, null)
        if (openResult != SQLITE_OK) {
            throw sqlException(logging, SqliteDatabaseConfig(path, label), sqlite3_errmsg(dbPtr.value)?.toKString() ?: "", openResult)
        }
        dbPtr.value!!
    }

    if (lookasideSlotSize >= 0 && lookasideSlotCount >= 0) {
        val err = sqlite3_db_config(db, SQLITE_DBCONFIG_LOOKASIDE, null, lookasideSlotSize, lookasideSlotCount);
        if (err != SQLITE_OK) {
            val error = sqlite3_errmsg(db)?.toKString()
            sqlite3_close(db)
            throw sqlException(logging, SqliteDatabaseConfig(path, label), "Cannot set lookaside : sqlite3_db_config(..., ${lookasideSlotSize}, %${lookasideSlotCount}) failed, ${error?:""}", err)
        }
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags and SQLITE_OPEN_READWRITE > 0) && sqlite3_db_readonly(db, null) != 0) {
        sqlite3_close(db)
        throw sqlException(logging, SqliteDatabaseConfig(path, label), "Could not open the database in read/write mode")
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    val err = sqlite3_busy_timeout(db, busyTimeout)
    if (err != SQLITE_OK) {
        sqlite3_close(db)
        throw sqlException(logging, SqliteDatabaseConfig(path, label), "Could not set busy timeout", err)
    }

    /*// Create wrapper object.
    SQLiteConnection* connection = new SQLiteConnection(db, openFlags, path, label);

    // Enable tracing and profiling if requested.
    if (enableTrace) {
        sqlite3_trace_v2(db, SQLITE_TRACE_STMT, &sqliteTraceV2Callback, connection);
    }
    if (enableProfile) {
        sqlite3_trace_v2(db, SQLITE_TRACE_PROFILE, &sqliteTraceV2Callback, connection);
    }*/

    logging.v { "dbOpen path [$path] label [$label] ${SqliteDatabaseConfig(path, label)}" }

    return SqliteDatabase(path, label, logging, verboseDataCalls, db)
}