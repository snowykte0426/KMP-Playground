package com.amond.kmpbook

import platform.Foundation.NSDate

actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
