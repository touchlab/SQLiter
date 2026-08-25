package co.touchlab.sqliter.interop

interface Logger {
    fun trace(message: String)
    val vActive:Boolean
    fun vWrite(message: String)
    val eActive:Boolean
    fun eWrite(message: String, exception: Throwable? = null)
}

inline fun Logger.v(block:()->String){
    if(vActive)
        vWrite(block())
}

inline fun Logger.e(exception: Throwable?, block:()->String){
    if(eActive)
        eWrite(block(), exception)
}