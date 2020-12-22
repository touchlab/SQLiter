package sql

internal const val LOG_TRACE = true

internal inline fun trace_log(s: String) {
    if (LOG_TRACE) {
        print("trace - ")
        println(s)
    }
}

interface Logger {
    fun trace(message: String)
    val vActive:Boolean
    fun vWrite(message: String)
    val eActive:Boolean
    fun eWrite(message: String, exception: Throwable? = null)
}

internal inline fun Logger.v(block:()->String){
    if(vActive)
        vWrite(block())
}

internal inline fun Logger.e(exception: Throwable?, block:()->String){
    if(eActive)
        eWrite(block(), exception)
}