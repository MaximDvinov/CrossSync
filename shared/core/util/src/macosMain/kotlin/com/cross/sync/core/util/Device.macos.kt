package com.cross.sync.core.util

import platform.Foundation.NSProcessInfo

actual fun getDeviceName(): String = NSProcessInfo.processInfo.hostName.substringBefore('.')
