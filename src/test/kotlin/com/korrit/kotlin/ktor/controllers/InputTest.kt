package com.korrit.kotlin.ktor.controllers

import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.UnsupportedMediaType
import io.ktor.response.respond
import io.ktor.server.testing.setBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.time.LocalDate

internal class InputTest {

    data class Request(
        val stringParam: String,
        val intParam: Int
    )

    @Test
    fun `Should deserialize json request body`() {
        val server = testServer {
            class TestRequest : Input<Request>() {

                override suspend fun Ctx.respond() {
                    val request: Request = body()
                    assertEquals("1337", request.stringParam)
                    assertEquals(1337, request.intParam)

                    call.respond(OK, "")
                }
            }

            POST("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Post
                addHeader(ContentType, Application.Json.toString())
                setBody("{\n  \"stringParam\": \"1337\",\n  \"intParam\": 1337\n}")
            }
        ) {
            assertEquals(OK, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should deserialize any request body`() {
        val server = testServer {
            class TestRequest : Input<LocalDate>() {

                override suspend fun Ctx.respond() {
                    val request: LocalDate = body()
                    assertEquals(LocalDate.of(1993, 12, 11), request)

                    call.respond(OK, "")
                }
            }

            POST("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Post
                addHeader(ContentType, Application.Json.toString())
                setBody("\"1993-12-11\"") // note that application/json is deserialized by jackson with JavaTimeModule in the test server
            }
        ) {
            assertEquals(OK, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should should work with bytes`() {
        val file = File(javaClass.getResource("/fixture_file").toURI())

        val server = testServer {
            class TestRequest : Input<ByteArray>(contentType = Application.OctetStream, strict = true) {

                override suspend fun Ctx.respond() {
                    val request: ByteArray = body()

                    assertTrue(request contentEquals file.readBytes())

                    call.respond(OK, "")
                }
            }

            POST("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Post
                addHeader(ContentType, Application.OctetStream.toString())
                setBody(file.readBytes())
            }
        ) {
            assertEquals(OK, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should return 415 when content type does not match`() {
        val file = File(javaClass.getResource("/fixture_file").toURI())

        val server = testServer {
            class TestRequest : Input<ByteArray>(contentType = Application.OctetStream, strict = true) {

                override suspend fun Ctx.respond() {
                    body<ByteArray>()
                    fail("Should terminate earlier")
                }
            }

            POST("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test"
                method = Post
                setBody(file.readBytes())
            }
        ) {
            assertEquals(UnsupportedMediaType, response.status())
        }

        server.stop(0, 0)
    }
}
