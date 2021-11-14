package com.bigduu.zerocopy

import java.io.Serializable

data class RequestFile(val path: String) : Serializable {
    companion object {
        private const val serialVersionUID = 3L
    }
}
