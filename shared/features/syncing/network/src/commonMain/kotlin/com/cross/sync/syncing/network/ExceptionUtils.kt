package com.cross.sync.syncing.network

import io.ktor.client.plugins.ResponseException

inline fun <R> runCatchingForApi(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: ResponseException) {
        Result.failure(HttpException(c.response.status.value, c.response.status.description))
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

class HttpException(val code: Int, val errorMessage: String) : Exception(errorMessage)

class ClientException(val errorMessage: String = "Client is not initialized"): Exception(errorMessage)