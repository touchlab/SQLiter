package co.touchlab.sqliter.interop

open class SQLiteException internal constructor(message: String, private val config: SqliteDatabaseConfig) : Exception(message)

class SQLiteExceptionErrorCode internal constructor(message: String, config: SqliteDatabaseConfig, private val errorCode: Int) : SQLiteException(message, config) {
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
    SQLITE_OK(co.touchlab.sqliter.sqlite3.SQLITE_OK),   /* Successful result */

    /* beginning-of-error-codes */
    SQLITE_ERROR(co.touchlab.sqliter.sqlite3.SQLITE_ERROR),   /* Generic error */
    SQLITE_INTERNAL(co.touchlab.sqliter.sqlite3.SQLITE_INTERNAL),   /* Internal logic error in SQLite */
    SQLITE_PERM(co.touchlab.sqliter.sqlite3.SQLITE_PERM),   /* Access permission denied */
    SQLITE_ABORT(co.touchlab.sqliter.sqlite3.SQLITE_ABORT),   /* Callback routine requested an abort */
    SQLITE_BUSY(co.touchlab.sqliter.sqlite3.SQLITE_BUSY),   /* The database file is locked */
    SQLITE_LOCKED(co.touchlab.sqliter.sqlite3.SQLITE_LOCKED),   /* A table in the database is locked */
    SQLITE_NOMEM(co.touchlab.sqliter.sqlite3.SQLITE_NOMEM),   /* A malloc() failed */
    SQLITE_READONLY(co.touchlab.sqliter.sqlite3.SQLITE_READONLY),   /* Attempt to write a readonly database */
    SQLITE_INTERRUPT(co.touchlab.sqliter.sqlite3.SQLITE_INTERRUPT),   /* Operation terminated by sqlite3_interrupt()*/
    SQLITE_IOERR(co.touchlab.sqliter.sqlite3.SQLITE_IOERR),   /* Some kind of disk I/O error occurred */
    SQLITE_CORRUPT(co.touchlab.sqliter.sqlite3.SQLITE_CORRUPT),   /* The database disk image is malformed */
    SQLITE_NOTFOUND(co.touchlab.sqliter.sqlite3.SQLITE_NOTFOUND),   /* Unknown opcode in sqlite3_file_control() */
    SQLITE_FULL(co.touchlab.sqliter.sqlite3.SQLITE_FULL),   /* Insertion failed because database is full */
    SQLITE_CANTOPEN(co.touchlab.sqliter.sqlite3.SQLITE_CANTOPEN),   /* Unable to open the database file */
    SQLITE_PROTOCOL(co.touchlab.sqliter.sqlite3.SQLITE_PROTOCOL),   /* Database lock protocol error */
    SQLITE_EMPTY(co.touchlab.sqliter.sqlite3.SQLITE_EMPTY),   /* Internal use only */
    SQLITE_SCHEMA(co.touchlab.sqliter.sqlite3.SQLITE_SCHEMA),   /* The database schema changed */
    SQLITE_TOOBIG(co.touchlab.sqliter.sqlite3.SQLITE_TOOBIG),   /* String or BLOB exceeds size limit */
    SQLITE_CONSTRAINT(co.touchlab.sqliter.sqlite3.SQLITE_CONSTRAINT),   /* Abort due to constraint violation */
    SQLITE_MISMATCH(co.touchlab.sqliter.sqlite3.SQLITE_MISMATCH),   /* Data type mismatch */
    SQLITE_MISUSE(co.touchlab.sqliter.sqlite3.SQLITE_MISUSE),   /* Library used incorrectly */
    SQLITE_NOLFS(co.touchlab.sqliter.sqlite3.SQLITE_NOLFS),   /* Uses OS features not supported on host */
    SQLITE_AUTH(co.touchlab.sqliter.sqlite3.SQLITE_AUTH),   /* Authorization denied */
    SQLITE_FORMAT(co.touchlab.sqliter.sqlite3.SQLITE_FORMAT),   /* Not used */
    SQLITE_RANGE(co.touchlab.sqliter.sqlite3.SQLITE_RANGE),   /* 2nd parameter to sqlite3_bind out of range */
    SQLITE_NOTADB(co.touchlab.sqliter.sqlite3.SQLITE_NOTADB),   /* File opened that is not a database file */
    SQLITE_NOTICE(co.touchlab.sqliter.sqlite3.SQLITE_NOTICE),   /* Notifications from sqlite3_log() */
    SQLITE_WARNING(co.touchlab.sqliter.sqlite3.SQLITE_WARNING),   /* Warnings from sqlite3_log() */
    SQLITE_ROW(co.touchlab.sqliter.sqlite3.SQLITE_ROW),  /* sqlite3_step() has another row ready */
    SQLITE_DONE(co.touchlab.sqliter.sqlite3.SQLITE_DONE),  /* sqlite3_step() has finished executing */
}