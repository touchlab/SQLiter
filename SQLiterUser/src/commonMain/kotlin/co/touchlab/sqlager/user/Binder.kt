package co.touchlab.sqlager.user

interface Binder{

    fun bytes(
        bytes: ByteArray,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun double(
        double: Double,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun long(
        long: Long,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun string(
        string: String,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun nullArg(
        index: Int = AUTO_INDEX,
        name: String? = null
    )
}

internal val AUTO_INDEX = -1