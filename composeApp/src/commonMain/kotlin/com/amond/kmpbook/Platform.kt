package com.amond.kmpbook

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform