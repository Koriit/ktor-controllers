package korrit.kotlin.ktor.controllers.features

import io.ktor.features.callId
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import korrit.kotlin.ktor.controllers.Ctx
import korrit.kotlin.ktor.controllers.EmptyBodyInput
import korrit.kotlin.ktor.controllers.GET
import korrit.kotlin.ktor.controllers.header
import korrit.kotlin.ktor.controllers.testServer
import korrit.kotlin.ktor.features.UUIDCallId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotEquals

internal class UUIDCallIdTest {

    private val primaryHeader = UUIDCallId.Config().primaryHeader
    private val secondaryHeader = UUIDCallId.Config().secondaryHeader

    @Test
    fun `Should accept and return call id from primary header`() {
        val expectedCallId = UUID.randomUUID().toString()
        val secondaryId = UUID.randomUUID().toString()

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(primaryHeader)

                override suspend fun Ctx.respond() {
                    assertEquals(expectedCallId, callId)
                    assertEquals(callId, call.callId)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(server.handleRequest {
            uri = "/test"
            method = Get
            addHeader(primaryHeader, expectedCallId)
            addHeader(secondaryHeader, secondaryId)
        }) {
            assertEquals(OK, response.status())
            assertEquals(expectedCallId, response.headers[primaryHeader])
            assertEquals(secondaryId, response.headers[secondaryHeader])
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should accept and return call id from secondary header if primary missing`() {
        val expectedCallId = UUID.randomUUID().toString()

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(primaryHeader)

                override suspend fun Ctx.respond() {
                    assertEquals(expectedCallId, callId)
                    assertEquals(callId, call.callId)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(server.handleRequest {
            uri = "/test"
            method = Get
            addHeader(primaryHeader, "")
            addHeader(secondaryHeader, expectedCallId)
        }) {
            assertEquals(OK, response.status())
            assertEquals(expectedCallId, response.headers[primaryHeader])
            assertEquals(expectedCallId, response.headers[secondaryHeader])
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should generate secondary id if missing`() {
        val expectedCallId = UUID.randomUUID().toString()

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(primaryHeader)

                override suspend fun Ctx.respond() {
                    assertEquals(expectedCallId, callId)
                    assertEquals(callId, call.callId)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(server.handleRequest {
            uri = "/test"
            method = Get
            addHeader(primaryHeader, expectedCallId)
            addHeader(secondaryHeader, "")
        }) {
            assertEquals(OK, response.status())
            assertEquals(expectedCallId, response.headers[primaryHeader])
            assertFalse(response.headers[secondaryHeader].isNullOrEmpty())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should generate call id if both headers absent`() {
        var generatedCallId = ""

        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val callId: String by header(primaryHeader, default = "")

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

        with(server.handleRequest {
            uri = "/test"
            method = Get
        }) {
            assertEquals(OK, response.status())
            assertEquals(generatedCallId, response.headers[primaryHeader])
            assertEquals(generatedCallId, response.headers[secondaryHeader])
        }

        server.stop(0, 0)
    }
}
