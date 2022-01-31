package com.korrit.kotlin.ktor.controllers.features

import com.korrit.kotlin.ktor.controllers.Ctx
import com.korrit.kotlin.ktor.controllers.EmptyBodyInput
import com.korrit.kotlin.ktor.controllers.GET
import com.korrit.kotlin.ktor.controllers.header
import com.korrit.kotlin.ktor.controllers.testServer
import com.korrit.kotlin.ktor.features.UUIDCallId
import io.ktor.features.callId
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UUIDCallIdTest {

    private val callIdHeader = UUIDCallId.Config().header

    @Test
    fun `Should accept and return call id`() {
        val expectedCallId = UUID.randomUUID().toString()

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(callIdHeader)

                override suspend fun Ctx.respond() {
                    assertEquals(expectedCallId, callId)
                    assertEquals(callId, call.callId)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Get
                addHeader(callIdHeader, expectedCallId)
            }
        ) {
            assertEquals(OK, response.status())
            assertEquals(expectedCallId, response.headers[callIdHeader])
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should generate call id`() {
        var generatedCallId = ""

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(callIdHeader, default = "")

                override suspend fun Ctx.respond() {
                    assertEquals("", callId)
                    assertNotEquals("", call.callId)
                    generatedCallId = call.callId!!
                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Get
            }
        ) {
            assertEquals(OK, response.status())
            assertEquals(generatedCallId, response.headers[callIdHeader]!!)
        }

        server.stop(0, 0)
    }
}
