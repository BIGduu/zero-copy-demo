package com.bigduu.zerocopy

import java.io.File
import java.io.Serializable

data class TransFile(
    val path: String,
    val bytes: ByteArray,
    val length:Long
) :
    Serializable {
    companion object {
        private const val serialVersionUID = 2L
    }


}
