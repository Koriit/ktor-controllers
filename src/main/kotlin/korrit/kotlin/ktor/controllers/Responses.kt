package korrit.kotlin.ktor.controllers

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.util.AttributeKey
import kotlin.reflect.KClass

/**
 * Ktor attribute key of the list of possible API responses.
 */
val ResponsesKey = AttributeKey<List<ResponseType>>("Response Types")

/**
 * Describes header included in response.
 */
class HttpHeader(
    val name: String,
    val type: KClass<*> = String::class,
    val required: Boolean = true,
    val deprecated: Boolean = false
)

/**
 * Describes full API HTTP response.
 */
class ResponseType(
    val status: HttpStatusCode,
    val type: KClass<*>? = if (status.value >= HttpStatusCode.BadRequest.value) null else throw IllegalArgumentException("OutputType must define return type for non-error statuses"),
    val contentType: ContentType? = null,
    val headers: List<HttpHeader>? = null
)

/**
 * Adds possible response from given route.
 *
 * Currently doesn't change API behaviour - by default.
 */
inline fun <reified T : Any> Route.responds(status: HttpStatusCode, contentType: ContentType? = null, headers: List<HttpHeader>? = null): Route {
    val responses = attributes.computeIfAbsent(ResponsesKey) { mutableListOf() } as MutableList
    responses.add(ResponseType(status = status, type = T::class, contentType = contentType, headers = headers))
    return this
}

/**
 * Adds possible error response from given route. It is assumed that all such error responses have common type.
 *
 * Currently doesn't change API behaviour - by default.
 */
fun Route.errors(status: HttpStatusCode, contentType: ContentType? = null, headers: List<HttpHeader>? = null): Route {
    val responses = attributes.computeIfAbsent(ResponsesKey) { mutableListOf() } as MutableList
    responses.add(ResponseType(status = status, contentType = contentType, headers = headers))
    return this
}
