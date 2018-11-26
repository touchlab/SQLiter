package co.touchlab.sqlager.user

interface Binder{
    fun bytes(
        bytes: ByteArray?,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun double(
        double: Double?,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun long(
        long: Long?,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun string(
        string: String?,
        index: Int = AUTO_INDEX,
        name: String? = null
    )

    fun nullArg(
        index: Int = AUTO_INDEX,
        name: String? = null
    )
}

fun Binder.int(
    int: Int?,
    index: Int = AUTO_INDEX,
    name: String? = null
){
    long(int?.toLong(), index, name)
}

fun Binder.float(
    float: Float?,
    index: Int = AUTO_INDEX,
    name: String? = null
){
    double(float?.toDouble(), index, name)
}

internal val AUTO_INDEX = -1