package com.bigduu.zerocopy

import java.io.Serializable
import java.lang.StringBuilder

data class BackInfo(val start: Long = 0, val md5: String = "", val progress: Long = 0, val ended: Boolean = false) :
    Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("progress:")
        sb.append(progress)
        sb.append("\t\tstart:")
        sb.append(start)
        return sb.toString()
    }


}