package korrit.kotlin.ktor.controllers.delegates

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import korrit.kotlin.ktor.controllers.Ctx
import korrit.kotlin.ktor.controllers.EmptyBodyInput
import korrit.kotlin.ktor.controllers.GET
import korrit.kotlin.ktor.controllers.header
import korrit.kotlin.ktor.controllers.testServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.of

internal class HeaderParamTest {

    @Test
    fun `Should retrieve and convert header params`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val dateParam: LocalDate by header("My-Date-Header") // note that special time converter is installed in test server
                val stringParam: String by header("My-String-Header")
                val intParam: Int by header("My-Int-Header")
                val doubleParam: Double by header("My-Double-Header")
                val booleanParam: Boolean by header("My-Bool-Header")

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
                uri = "/test"
                method = Get
                addHeader("My-Date-Header", "1993-12-11")
                addHeader("My-String-Header", "1337")
                addHeader("My-Int-Header", "1337")
                addHeader("My-Double-Header", "13.37")
                addHeader("My-Bool-Header", "true")
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
                val dateParam: LocalDate by header("My-Date-Header", default = of(1993, 12, 11)) // note that special time converter is installed in test server
                val stringParam: String by header("My-String-Header", default = "1337")
                val intParam: Int by header("My-Int-Header", default = 1337)
                val doubleParam: Double by header("My-Double-Header", default = 13.37)
                val booleanParam: Boolean by header("My-Bool-Header", default = true)
                val nullableParam: Boolean? by header<Boolean?>("My-Nullable-Header", default = null)

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
    fun `Should respond 400 if missing`() {
        val server = testServer {
            class TestRequest : EmptyBodyInput() {
                val requiredParam: String by header("My-String-Header")

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
                val intParam: Int by header("My-Int-Header")

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
                uri = "/test"
                method = Get
                addHeader("My-Int-Header", "qwerty")
            }
        ) {
            assertEquals(BadRequest, response.status())
        }

        server.stop(0, 0)
    }
}
