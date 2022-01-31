package korrit.kotlin.ktor.controllers.delegates

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import korrit.kotlin.ktor.controllers.Ctx
import korrit.kotlin.ktor.controllers.EmptyBodyInput
import korrit.kotlin.ktor.controllers.GET
import korrit.kotlin.ktor.controllers.path
import korrit.kotlin.ktor.controllers.testServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.of

internal class PathParamTest {

    @Test
    fun `Should retrieve and convert path params`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val dateParam: LocalDate by path() // note that special time converter is installed in test server
                val stringParam: String by path()
                val intParam: Int by path()
                val doubleParam: Double by path()
                val booleanParam: Boolean by path()

                override suspend fun Ctx.respond() {
                    assertEquals(of(1993, 12, 11), dateParam)
                    assertEquals("1337", stringParam)
                    assertEquals(1337, intParam)
                    assertEquals(13.37, doubleParam)
                    assertEquals(true, booleanParam)

                    call.respond(OK, "")
                }
            }

            GET("/test/{dateParam}/{stringParam}/{intParam}/{doubleParam}/{booleanParam}") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test/1993-12-11/1337/1337/13.37/true"
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
                val stringParam: String by path(default = "1337")
                val intParam: Int by path(default = 1337)
                val doubleParam: Double by path(default = 13.37)
                val booleanParam: Boolean by path(default = true)
                val nullableParam: Boolean? by path<Boolean?>(default = null)

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

            GET("/test/{dateParam?}/{stringParam?}/{intParam?}/{doubleParam?}/{booleanParam?}") { TestRequest() }
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
                val providedParam: String by path("stringParam")
                val missingParam: Int by path("intParam", default = 1337)

                override suspend fun Ctx.respond() {
                    assertEquals("1337", providedParam)
                    assertEquals(1337, missingParam)

                    call.respond(OK, "")
                }
            }

            GET("/test/{stringParam}/{intParam?}") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test/1337"
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
                val requiredParam: String by path()

                override suspend fun Ctx.respond() {
                    println(requiredParam)
                    fail("Should terminate earlier")
                }
            }

            GET("/test/{requiredParam?}") { TestRequest() }
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
                val intParam: Int by path()

                override suspend fun Ctx.respond() {
                    println(intParam)
                    fail("Should terminate earlier")
                }
            }

            GET("/test/{intParam}") { TestRequest() }
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/test/qwerty"
                method = Get
            }
        ) {
            assertEquals(BadRequest, response.status())
        }

        server.stop(0, 0)
    }
}
