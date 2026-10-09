package com.scrnlimit.screen.time.control.domain

import java.security.MessageDigest
import java.util.UUID

object Pin {
    fun salt(): String = UUID.randomUUID().toString()

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
