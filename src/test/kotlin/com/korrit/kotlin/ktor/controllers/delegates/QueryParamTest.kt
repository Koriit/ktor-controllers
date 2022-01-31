package com.korrit.kotlin.ktor.controllers.delegates

import com.korrit.kotlin.ktor.controllers.Ctx
import com.korrit.kotlin.ktor.controllers.EmptyBodyInput
import com.korrit.kotlin.ktor.controllers.GET
import com.korrit.kotlin.ktor.controllers.path
import com.korrit.kotlin.ktor.controllers.query
import com.korrit.kotlin.ktor.controllers.testServer
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.of

internal class QueryParamTest {

    @Test
    fun `Should retrieve and convert query params`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val dateParam: LocalDate by path() // note that special time converter is installed in test server
                val stringParam: String by query()
                val intParam: Int by query()
                val doubleParam: Double by query()
                val booleanParam: Boolean by query()

                override suspend fun Ctx.respond() {
                    assertEquals(of(1993, 12, 11), dateParam)
                    assertEquals("1337", stringParam)
                    assertEquals(1337, intParam)
                    assertEquals(13.37, doubleParam)
                    assertEquals(true, booleanParam)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test?dateParam=1993-12-11&stringParam=1337&intParam=1337&doubleParam=13.37&booleanParam=true"
                method = Get
            }
        ) {
            assertEquals(OK, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should use default value if missing`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val dateParam: LocalDate by path(default = of(1993, 12, 11)) // note that special time converter is installed in test server
                val stringParam: String by query(default = "1337")
                val intParam: Int by query(default = 1337)
                val doubleParam: Double by query(default = 13.37)
                val booleanParam: Boolean by query(default = true)
                val nullableParam: Boolean? by query<Boolean?>(default = null)

                override suspend fun Ctx.respond() {
                    assertEquals(of(1993, 12, 11), dateParam)
                    assertEquals("1337", stringParam)
                    assertEquals(1337, intParam)
                    assertEquals(13.37, doubleParam)
                    assertEquals(true, booleanParam)
                    assertEquals(null, nullableParam)

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
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should allow renaming`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val providedParam: String by query("stringParam")
                val missingParam: Int by query("intParam", default = 1337)

                override suspend fun Ctx.respond() {
                    assertEquals("1337", providedParam)
                    assertEquals(1337, missingParam)

                    call.respond(OK, "")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test?stringParam=1337"
                method = Get
            }
        ) {
            assertEquals(OK, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should respond 400 if missing`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val requiredParam: String by query()

                override suspend fun Ctx.respond() {
                    println(requiredParam)
                    fail("Should terminate earlier")
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
            assertEquals(BadRequest, response.status())
        }

        server.stop(0, 0)
    }

    @Test
    fun `Should respond 400 if cannot convert`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val intParam: Int by query()

                override suspend fun Ctx.respond() {
                    println(intParam)
                    fail("Should terminate earlier")
                }
            }

            GET("/test") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test?intParam=qwerty"
                method = Get
            }
        ) {
            assertEquals(BadRequest, response.status())
        }

        server.stop(0, 0)
    }
}
