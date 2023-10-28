package co.touchlab.sqliter.interop

import kotlinx.cinterop.*
import co.touchlab.sqliter.sqlite3.*
import platform.posix.usleep

expect inline fun bytesToString(bv:CPointer<ByteVar>):String

private const val EMPTY_STRING = ""

internal class ActualSqliteStatement(private val db: SqliteDatabase, private val stmtPointer: SqliteStatementPointer) :
    SqliteStatement {

    //Cursor methods
    override fun isNull(index: Int): Boolean =
        sqlite3_column_type(stmtPointer, index) == SQLITE_NULL

    override fun columnGetLong(columnIndex: Int): Long =
        sqlite3_column_int64(stmtPointer, columnIndex)

    override fun columnGetDouble(columnIndex: Int): Double =
        sqlite3_column_double(stmtPointer, columnIndex)

    override fun columnGetString(columnIndex: Int): String =
        sqlite3_column_text(stmtPointer, columnIndex)?.reinterpret<ByteVar>()?.let { bytesToString(it) }
            ?: EMPTY_STRING

    override fun columnGetBlob(columnIndex: Int): ByteArray {
        val blobSize = sqlite3_column_bytes(stmtPointer, columnIndex)
        val blob = sqlite3_column_blob(stmtPointer, columnIndex)

        if (blobSize < 0 || blob == null)
            throw sqlException(db.logger, db.config, "Byte array size/type issue col $columnIndex")

        return blob.readBytes(blobSize)
    }

    override fun columnCount(): Int =
        sqlite3_column_count(stmtPointer)

    override fun columnName(columnIndex: Int): String =
        bytesToString(sqlite3_column_name(stmtPointer, columnIndex)!!)

    override fun columnType(columnIndex: Int): Int =
        sqlite3_column_type(stmtPointer, columnIndex)

    override fun step(): Boolean {

        //Maybe move a first call to pre-loop
        for (retryCount in 0 until 50) {
            val err = sqlite3_step(stmtPointer)
            if (err == SQLITE_ROW) {
                return true;
            } else if (err == SQLITE_DONE) {
                return false;
            } else if (err == SQLITE_LOCKED || err == SQLITE_BUSY) {
                // The table is locked, retry
                traceLogCallback("Database locked, retrying")
                usleep(1000u)
            } else {
                throw sqlException(db.logger, db.config, "sqlite3_step failed", err)
            }
        }

        throw sqlException(db.logger, db.config, "sqlite3_step retry count exceeded")
    }

    //Statement methods
    override fun finalizeStatement() {
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        db.logger.v { "Finalized statement $stmtPointer on connection $db" }
        sqlite3_finalize(stmtPointer)
    }

    override fun bindParameterIndex(paramName: String): Int =
        sqlite3_bind_parameter_index(stmtPointer, paramName)

    override fun resetStatement() = opResult(db) {
        sqlite3_reset(stmtPointer)
    }

    override fun clearBindings() = opResult(db) {
        sqlite3_clear_bindings(stmtPointer)
    }

    override fun execute() {
        executeNonQuery()
    }

    override fun executeForChangedRowCount(): Int {
        val err = executeNonQuery()
        return if (err == SQLITE_DONE) {
            sqlite3_changes(db.dbPointer)
        } else {
            -1
        }
    }

    override fun executeForLastInsertedRowId(): Long {
        val err = executeNonQuery();
        return if (err == SQLITE_DONE && sqlite3_changes(db.dbPointer) > 0) {
            sqlite3_last_insert_rowid(db.dbPointer)
        } else {
            -1
        }
    }

    override fun bindNull(index: Int) = opResult(db) {
        sqlite3_bind_null(stmtPointer, index)
    }

    override fun bindLong(index: Int, value: Long) = opResult(db) {
        sqlite3_bind_int64(stmtPointer, index, value)
    }

    override fun bindDouble(index: Int, value: Double) = opResult(db) {
        sqlite3_bind_double(stmtPointer, index, value)
    }

    override fun bindString(index: Int, value: String) = opResult(db) {
        //TODO: Was using UTF 16 function previously. Do a little research.
        sqlite3_bind_text(stmtPointer, index, value, -1, SQLITE_TRANSIENT)
    }

    override fun bindBlob(index: Int, value: ByteArray) = opResult(db) {
        sqlite3_bind_blob(stmtPointer, index, value.refTo(0), value.size, SQLITE_TRANSIENT)
    }

    private inline fun opResult(db: SqliteDatabase, block: () -> Int) {
        val err = block()
        if (err != SQLITE_OK) {
            val error = sqlite3_errmsg(db.dbPointer)?.toKString()
            throw sqlException(db.logger, db.config, "Sqlite operation failure ${error?:""}", err)
        }
    }

    override fun executeNonQuery(): Int {
        val err = sqlite3_step(stmtPointer)
        if (err == SQLITE_ROW) {
            throw sqlException(db.logger, db.config, "Queries can be performed using SQLiteDatabase query or rawQuery methods only.")
        } else if (err != SQLITE_DONE) {
            throw sqlException(db.logger, db.config, "executeNonQuery error", err)
        }
        return err
    }

    override fun traceLogCallback(message: String) {
        //No logging
    }
} 