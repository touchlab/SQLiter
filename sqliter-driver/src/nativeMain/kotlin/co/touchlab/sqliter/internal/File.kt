package co.touchlab.sqliter.internal

internal expect class File {
    val path: String
    val exists: Boolean
}