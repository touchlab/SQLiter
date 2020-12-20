package sql

import kotlinx.cinterop.*
import platform.posix.usleep
import sqlite3.*

class SqliteStatement(private val db: SqliteDatabase, internal val stmtPointer: SqliteStatementPointer) {

    //Cursor methods
    fun nativeIsNull(index: Int): Boolean =
        sqlite3_column_type(stmtPointer, index) == SQLITE_NULL

    fun nativeColumnGetLong(columnIndex: Int): Long =
        sqlite3_column_int64(stmtPointer, columnIndex)

    fun nativeColumnGetDouble(columnIndex: Int): Double =
        sqlite3_column_double(stmtPointer, columnIndex)

    fun nativeColumnGetString(columnIndex: Int): String =
        sqlite3_column_text(stmtPointer, columnIndex)?.reinterpret<ByteVar>()?.toKStringFromUtf8() ?: ""

    fun nativeColumnGetBlob(columnIndex: Int): ByteArray {
        val blobSize = sqlite3_column_bytes(stmtPointer, columnIndex)
        val blob = sqlite3_column_blob(stmtPointer, columnIndex)
        if (blobSize < 0 || blob == null)
            throw SQLiteException("Byte array size/type issue col $columnIndex")

        return blob.readBytes(blobSize)
    }

    fun nativeColumnCount(): Int =
        sqlite3_column_count(stmtPointer)

    fun nativeColumnName(columnIndex: Int): String =
        sqlite3_column_name(stmtPointer, columnIndex)!!.toKStringFromUtf8()

    fun nativeColumnType(columnIndex: Int): Int =
        sqlite3_column_type(stmtPointer, columnIndex)

    fun nativeStep(): Boolean {

        //Maybe move a first call to pre-loop
        for (retryCount in 0 until 50) {
            val err = sqlite3_step(stmtPointer)
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
    fun nativeFinalizeStatement() {
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        ALOGV("Finalized statement $stmtPointer on connection $db")
        sqlite3_finalize(stmtPointer)
    }

    fun nativeBindParameterIndex(paramName: String): Int =
        sqlite3_bind_parameter_index(stmtPointer, paramName)

    fun nativeResetStatement() = opResult(db) {
        sqlite3_reset(stmtPointer)
    }

    fun nativeClearBindings() = opResult(db) {
        sqlite3_clear_bindings(stmtPointer)
    }

    fun nativeExecute() {
        executeNonQuery()
    }

    fun nativeExecuteForChangedRowCount(): Int {
        val err = executeNonQuery()
        return if (err == SQLITE_DONE) {
            sqlite3_changes(db.dbPointer)
        } else {
            -1
        }
    }

    fun nativeExecuteForLastInsertedRowId(): Long {
        val err = executeNonQuery();
        return if (err == SQLITE_DONE && sqlite3_changes(db.dbPointer) > 0) {
            sqlite3_last_insert_rowid(db.dbPointer)
        } else {
            -1
        }
    }

    fun nativeBindNull(index: Int) = opResult(db) {
        sqlite3_bind_null(stmtPointer, index)
    }

    fun nativeBindLong(index: Int, value: Long) = opResult(db) {
        sqlite3_bind_int64(stmtPointer, index, value)
    }

    fun nativeBindDouble(index: Int, value: Double) = opResult(db) {
        sqlite3_bind_double(stmtPointer, index, value)
    }

    fun nativeBindString(index: Int, value: String) = opResult(db) {
        //TODO: Was using UTF 16 function previously. Do a little research.
        sqlite3_bind_text(stmtPointer, index, value, value.length, SQLITE_TRANSIENT)
    }

    fun nativeBindBlob(index: Int, value: ByteArray) = opResult(db) {
        sqlite3_bind_blob(stmtPointer, index, value.refTo(0), value.size, SQLITE_TRANSIENT)
    }

    inline fun opResult(db: SqliteDatabase, block: () -> Int) {
        val err = block()
        if (err != SQLITE_OK) {
            throw SQLiteExceptionHandle(db, null)
        }
    }

    internal fun executeNonQuery(): Int {
        val err = sqlite3_step(stmtPointer)
        if (err == SQLITE_ROW) {
            throw SQLiteException("Queries can be performed using SQLiteDatabase query or rawQuery methods only.")
        } else if (err != SQLITE_DONE) {
            throw SQLiteExceptionHandle(db, null)
        }
        return err
    }
} 