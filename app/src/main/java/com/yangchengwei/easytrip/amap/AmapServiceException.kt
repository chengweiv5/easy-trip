package com.yangchengwei.easytrip.amap

class AmapServiceException(
    val operation: String,
    val errorCode: Int,
    detail: String,
) : Exception("$operation failed ($errorCode): $detail")
