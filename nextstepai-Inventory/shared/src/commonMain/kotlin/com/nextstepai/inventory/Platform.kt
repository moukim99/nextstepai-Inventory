package com.nextstepai.inventory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform