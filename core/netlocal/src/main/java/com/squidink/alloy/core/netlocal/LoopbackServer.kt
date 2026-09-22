package com.squidink.alloy.core.netlocal

import com.squidink.alloy.core.common.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ktor local loopback server bound strictly to 127.0.0.1 with bearer token gating.
 */
@Singleton
class LoopbackServer @Inject constructor() {

    private var server: EmbeddedServer<*, *>? = null
    var authToken: String? = null

    fun start(port: Int = 8787): Result<Unit> {
        if (server != null) return Result.success(Unit)

        return try {
            server = embeddedServer(CIO, port = port, host = "127.0.0.1") {
                routing {
                    get("/v1/health") {
                        call.respondText("OK")
                    }

                    get("/v1/models") {
                        val tokenHeader = call.request.header("Authorization")
                        if (authToken != null && tokenHeader != "Bearer $authToken") {
                            call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        } else {
                            call.respondText("{\"data\": []}")
                        }
                    }
                }
            }.start(wait = false)

            Logger.i(TAG, "Local loopback server started on 127.0.0.1:$port")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start local loopback server on port $port", e)
            Result.failure(e)
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        Logger.i(TAG, "Local loopback server stopped")
    }

    companion object {
        private const val TAG = "LoopbackServer"
    }
}
