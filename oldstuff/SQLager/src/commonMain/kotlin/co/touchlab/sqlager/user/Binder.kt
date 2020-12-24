package co.touchlab.sqlager.user

interface Binder{
    fun bytes(
        index: Int,
        bytes: ByteArray?
    )

    fun double(
        index: Int,
        double: Double?
    )

    fun long(
        index: Int,
        long: Long?
    )

    fun string(
        index: Int,
        string: String?
    )

    fun nullArg(
        index: Int
    )

    fun bindParameterIndex(name:String):Int
}

fun Binder.bytes(bytes: ByteArray?) = bytes(AUTO_INDEX, bytes)
fun Binder.double(double: Double?) = double(AUTO_INDEX, double)
fun Binder.long(long: Long?) = long(AUTO_INDEX, long)
fun Binder.string(string: String?) = string(AUTO_INDEX, string)
fun Binder.nullArg() = nullArg(AUTO_INDEX)
fun Binder.bytes(name:String, bytes: ByteArray?) = bytes(bindParameterIndex(name), bytes)
fun Binder.double(name:String, double: Double?) = double(bindParameterIndex(name), double)
fun Binder.long(name:String, long: Long?) = long(bindParameterIndex(name), long)
fun Binder.string(name:String, string: String?) = string(bindParameterIndex(name), string)
fun Binder.nullArg(name:String) = nullArg(bindParameterIndex(name))

fun Binder.int(index: Int,int: Int?){
    long(index, int?.toLong())
}

fun Binder.int(int: Int?) = int(AUTO_INDEX, int)
fun Binder.int(name:String, int: Int?) = int(bindParameterIndex(name), int)

fun Binder.float(index: Int, float: Float?){
    double(index, float?.toDouble())
}

fun Binder.float(float: Float?) = double(AUTO_INDEX, float?.toDouble())
fun Binder.float(name:String, float: Float?) = double(bindParameterIndex(name), float?.toDouble())

internal val AUTO_INDEX = -1