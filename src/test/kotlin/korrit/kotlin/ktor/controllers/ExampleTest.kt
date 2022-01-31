package korrit.kotlin.ktor.controllers

import io.ktor.http.ContentType.Text
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.header
import io.ktor.response.respondText
import korrit.kotlin.ktor.controllers.exceptions.InputException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ExampleTest {

    @Test
    fun example() {
        val server = testServer {
            class GetDate : EmptyBodyInput() {

                val someParam: Int by query()

                override suspend fun Ctx.respond() {
                    if (someParam < 0) {
                        throw InputException("Param cannot be negative")
                    }

                    call.response.header("Elite-Header", someParam)

                    call.respondText("1993-12-11", Text.Plain, OK)
                }
            }

            GET("/date") { GetDate() }
                // describe the API
                .responds<String>(
                    status = OK,
                    contentType = Text.Plain,
                    headers = listOf(HttpHeader("EliteHeader", deprecated = true))
                )
                .errors(BadRequest)
        }

        server.start()

        with(
            server.handleRequest {
                uri = "/date?someParam=1337"
                method = Get
            }
        ) {
            assertEquals(OK, response.status())
            assertEquals("1337", response.headers["Elite-Header"])
            assertEquals("1993-12-11", response.content)
        }

        with(
            server.handleRequest {
                uri = "/date?someParam=-1"
                method = Get
            }
        ) {
            assertEquals(BadRequest, response.status())
        }

        server.stop(0, 0)
    }
}
