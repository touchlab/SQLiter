package co.touchlab.sqliter.interop

open class SQLiteException(message: String, private val config: SqliteDatabaseConfig) : Exception(message)

class SQLiteExceptionErrorCode(message: String, config: SqliteDatabaseConfig, private val errorCode: Int) : SQLiteException(message, config) {
    val errorType: SqliteErrorType by lazy {
        val checkErrorCode = errorCode and 0xff
        SqliteErrorType.values().find { it.code == checkErrorCode }
            ?: throw IllegalArgumentException("Unknown errorCode $errorCode, checkErrorCode $checkErrorCode")
    }
}

internal inline fun sqlException(logging: Logger, config: SqliteDatabaseConfig, message: String, errorCode: Int = -1): SQLiteException {
    return if (errorCode == -1) {
        val sqLiteException = SQLiteException(message, config)
        logging.e(sqLiteException) { message }
        sqLiteException
    } else {
        val sqLiteException = SQLiteExceptionErrorCode(message, config, errorCode)
        logging.e(sqLiteException) { "$message | error code ${sqLiteException.errorType}" }
        sqLiteException
    }
}

enum class SqliteErrorType(val code: Int) {
    SQLITE_OK(sqlite3.SQLITE_OK),   /* Successful result */

    /* beginning-of-error-codes */
    SQLITE_ERROR(sqlite3.SQLITE_ERROR),   /* Generic error */
    SQLITE_INTERNAL(sqlite3.SQLITE_INTERNAL),   /* Internal logic error in SQLite */
    SQLITE_PERM(sqlite3.SQLITE_PERM),   /* Access permission denied */
    SQLITE_ABORT(sqlite3.SQLITE_ABORT),   /* Callback routine requested an abort */
    SQLITE_BUSY(sqlite3.SQLITE_BUSY),   /* The database file is locked */
    SQLITE_LOCKED(sqlite3.SQLITE_LOCKED),   /* A table in the database is locked */
    SQLITE_NOMEM(sqlite3.SQLITE_NOMEM),   /* A malloc() failed */
    SQLITE_READONLY(sqlite3.SQLITE_READONLY),   /* Attempt to write a readonly database */
    SQLITE_INTERRUPT(sqlite3.SQLITE_INTERRUPT),   /* Operation terminated by sqlite3_interrupt()*/
    SQLITE_IOERR(sqlite3.SQLITE_IOERR),   /* Some kind of disk I/O error occurred */
    SQLITE_CORRUPT(sqlite3.SQLITE_CORRUPT),   /* The database disk image is malformed */
    SQLITE_NOTFOUND(sqlite3.SQLITE_NOTFOUND),   /* Unknown opcode in sqlite3_file_control() */
    SQLITE_FULL(sqlite3.SQLITE_FULL),   /* Insertion failed because database is full */
    SQLITE_CANTOPEN(sqlite3.SQLITE_CANTOPEN),   /* Unable to open the database file */
    SQLITE_PROTOCOL(sqlite3.SQLITE_PROTOCOL),   /* Database lock protocol error */
    SQLITE_EMPTY(sqlite3.SQLITE_EMPTY),   /* Internal use only */
    SQLITE_SCHEMA(sqlite3.SQLITE_SCHEMA),   /* The database schema changed */
    SQLITE_TOOBIG(sqlite3.SQLITE_TOOBIG),   /* String or BLOB exceeds size limit */
    SQLITE_CONSTRAINT(sqlite3.SQLITE_CONSTRAINT),   /* Abort due to constraint violation */
    SQLITE_MISMATCH(sqlite3.SQLITE_MISMATCH),   /* Data type mismatch */
    SQLITE_MISUSE(sqlite3.SQLITE_MISUSE),   /* Library used incorrectly */
    SQLITE_NOLFS(sqlite3.SQLITE_NOLFS),   /* Uses OS features not supported on host */
    SQLITE_AUTH(sqlite3.SQLITE_AUTH),   /* Authorization denied */
    SQLITE_FORMAT(sqlite3.SQLITE_FORMAT),   /* Not used */
    SQLITE_RANGE(sqlite3.SQLITE_RANGE),   /* 2nd parameter to sqlite3_bind out of range */
    SQLITE_NOTADB(sqlite3.SQLITE_NOTADB),   /* File opened that is not a database file */
    SQLITE_NOTICE(sqlite3.SQLITE_NOTICE),   /* Notifications from sqlite3_log() */
    SQLITE_WARNING(sqlite3.SQLITE_WARNING),   /* Warnings from sqlite3_log() */
    SQLITE_ROW(sqlite3.SQLITE_ROW),  /* sqlite3_step() has another row ready */
    SQLITE_DONE(sqlite3.SQLITE_DONE),  /* sqlite3_step() has finished executing */
}