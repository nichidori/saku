package org.arraflydori.fin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform