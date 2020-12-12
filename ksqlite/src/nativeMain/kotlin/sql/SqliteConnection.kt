package sql

import kotlinx.cinterop.*
import platform.posix.usleep
import sqlite3.*

enum class OpenFlags {
    CREATE_IF_NECESSARY,
    OPEN_READONLY
}

fun nativeOpen(
    path: String, openFlags: List<OpenFlags>, label: String,
    enableTrace: Boolean, enableProfile: Boolean,
    lookasideSlotSize: Int,
    lookasideSlotCount: Int,
    busyTimeout: Int
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
            throw SQLiteExceptionErr(openResult, sqlite3_errmsg(dbPtr.value)?.toKString())
        }
        dbPtr.value!!
    }

    if (lookasideSlotSize >= 0 && lookasideSlotCount >= 0) {
        val err = sqlite3_db_config(db, SQLITE_DBCONFIG_LOOKASIDE, null, lookasideSlotSize, lookasideSlotCount);
        if (err != SQLITE_OK) {
            ALOGE("sqlite3_db_config(..., ${lookasideSlotSize}, %${lookasideSlotCount}) failed: ${err}")
            sqlite3_close(db)
            throw SQLiteExceptionHandle(db, "Cannot set lookaside")
        }
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags and SQLITE_OPEN_READWRITE > 0) && sqlite3_db_readonly(db, null) != 0) {
        sqlite3_close(db);
        throw SQLiteExceptionHandle(db, "Could not open the database in read/write mode.");
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    val err = sqlite3_busy_timeout(db, busyTimeout)
    if (err != SQLITE_OK) {
        sqlite3_close(db);
        throw SQLiteExceptionHandle(db, "Could not set busy timeout");
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

    ALOGV("Opened connection $db with label '${label}'");

    return db
}

fun nativePrepareStatement(db: SqliteDatabase, sqlString: String): SqliteStatement {
    val statement = memScoped {
        val statementPtr = alloc<CPointerVar<sqlite3_stmt>>()
        val sqlUgt16 = sqlString.wcstr
        val err = sqlite3_prepare16_v2(
            db,
            sqlUgt16.ptr,
            sqlUgt16.size, statementPtr.ptr, null
        )

        if (err != SQLITE_OK) {
            throw SQLiteExceptionHandle(db, " error while compiling: $sqlString")
        }

        statementPtr.value!!
    }

    ALOGV("Prepared statement $statement on connection $db")
    return statement
}

fun nativeClose(db: SqliteDatabase){
    ALOGV("Closing connection $db")
    val err = sqlite3_close(db)
    if (err != SQLITE_OK) {
        // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
        ALOGE("sqlite3_close($db) failed: $err");
        throw SQLiteExceptionHandle(db, "Could not close db.");
    }
}

//Cursor methods
fun nativeIsNull(statement: SqliteStatement, index: Int): Boolean =
    sqlite3_column_type(statement, index) == SQLITE_NULL

fun nativeColumnGetLong(statement: SqliteStatement, columnIndex: Int): Long =
    sqlite3_column_int64(statement, columnIndex)

fun nativeColumnGetDouble(statement: SqliteStatement, columnIndex: Int): Double =
    sqlite3_column_double(statement, columnIndex)

fun nativeColumnGetString(statement: SqliteStatement, columnIndex: Int): String =
    sqlite3_column_text(statement, columnIndex)?.reinterpret<ByteVar>()?.toKStringFromUtf8() ?: ""

fun nativeColumnGetBlob(statement: SqliteStatement, columnIndex: Int): ByteArray {
    val blobSize = sqlite3_column_bytes(statement, columnIndex)
    val blob = sqlite3_column_blob(statement, columnIndex)
    if(blobSize < 0 || blob == null)
        throw SQLiteException("Byte array size/type issue col $columnIndex")

    return blob.readBytes(blobSize)
}

fun nativeColumnCount(statement: SqliteStatement): Int =
    sqlite3_column_count(statement)

fun nativeColumnName(statement: SqliteStatement, columnIndex: Int): String =
    sqlite3_column_name(statement, columnIndex)!!.toKStringFromUtf8()

fun nativeColumnType(statement: SqliteStatement, columnIndex: Int): Int =
    sqlite3_column_type(statement, columnIndex)

fun nativeStep(db:SqliteDatabase, statement: SqliteStatement): Boolean {

    //Maybe move a first call to pre-loop
    for(retryCount in 0 until 50){
        val err = sqlite3_step(statement)
        if (err == SQLITE_ROW) {
            return true;
        } else if (err == SQLITE_DONE) {
            return false;
        } else if (err == SQLITE_LOCKED || err == SQLITE_BUSY) {
            // The table is locked, retry
//            LOG_WINDOW("Database locked, retrying");
            usleep(1000);
        } else {
            throw SQLiteExceptionErr(err, "sqlite3_step failed")
        }
    }

    //                ALOGE("Bailing on database busy retry");
    throw SQLiteExceptionHandle(db, "retrycount exceeded")
}

//Statement methods
fun nativeFinalizeStatement(db: SqliteDatabase, statement: SqliteStatement){
    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
    ALOGV("Finalized statement $statement on connection $db")
    sqlite3_finalize(statement)
}

fun nativeBindParameterIndex(statement: SqliteStatement, paramName:String):Int =
    sqlite3_bind_parameter_index(statement, paramName)

fun nativeResetStatement(db: SqliteDatabase, statement: SqliteStatement) = opResult(db) {
    sqlite3_reset(statement)
}

fun nativeClearBindings(db: SqliteDatabase, statement: SqliteStatement) = opResult(db) {
    sqlite3_clear_bindings(statement)
}

fun nativeExecute(db: SqliteDatabase, statement: SqliteStatement){
    executeNonQuery(db, statement)
}

fun nativeExecuteForChangedRowCount(db: SqliteDatabase, statement: SqliteStatement): Int {
    val err = executeNonQuery(db, statement)
    return if (err == SQLITE_DONE) {
        sqlite3_changes(db)
    } else {
        -1
    }
}

fun nativeExecuteForLastInsertedRowId(db: SqliteDatabase, statement: SqliteStatement): Long {
    val err = executeNonQuery(db, statement);
    return if (err == SQLITE_DONE && sqlite3_changes(db) > 0) {
        sqlite3_last_insert_rowid(db)
    } else {
        -1
    }
}

fun nativeBindNull(db: SqliteDatabase, statement: SqliteStatement, index: Int) = opResult(db) {
    sqlite3_bind_null(statement, index)
}

fun nativeBindLong(db: SqliteDatabase, statement: SqliteStatement, index: Int, value:Long) = opResult(db) {
    sqlite3_bind_int64(statement, index, value)
}

fun nativeBindDouble(db: SqliteDatabase, statement: SqliteStatement, index: Int, value:Double) = opResult(db) {
    sqlite3_bind_double(statement, index, value)
}

fun nativeBindString(db: SqliteDatabase, statement: SqliteStatement, index: Int, value:String) = opResult(db) {
    //TODO: Was using UTF 16 function previously. Do a little research.
    sqlite3_bind_text(statement, index, value, value.length, SQLITE_TRANSIENT)
}

fun nativeBindBlob(db: SqliteDatabase, statement: SqliteStatement, index: Int, value:ByteArray) = opResult(db) {
    sqlite3_bind_blob(statement, index, value.refTo(0), value.size, SQLITE_TRANSIENT)
}

inline fun opResult(db: SqliteDatabase, block:()->Int){
    val err = block()
    if (err != SQLITE_OK) {
        throw SQLiteExceptionHandle(db, null)
    }
}

internal fun executeNonQuery(db: SqliteDatabase, statement: SqliteStatement):Int{
    val err = sqlite3_step(statement)
    if (err == SQLITE_ROW) {
        throw SQLiteException("Queries can be performed using SQLiteDatabase query or rawQuery methods only.")
    } else if (err != SQLITE_DONE) {
        throw SQLiteExceptionHandle( db, null)
    }
    return err
}

