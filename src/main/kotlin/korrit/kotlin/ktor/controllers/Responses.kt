package korrit.kotlin.ktor.controllers

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.util.AttributeKey
import kotlin.reflect.KClass

val ResponsesKey = AttributeKey<List<ResponseType>>("Response Types")

class HttpHeader(
        val name: String,
        val type: KClass<*> = String::class,
        val required: Boolean = true,
        val deprecated: Boolean = false
)

class ResponseType(
        val status: HttpStatusCode,
        val type: KClass<*>? = if (status.value >= HttpStatusCode.BadRequest.value) null else throw IllegalArgumentException("OutputType must define return type for non-error statuses"),
        val contentType: ContentType? = null,
        val headers: List<HttpHeader>? = null
)

inline fun <reified T : Any> Route.responds(status: HttpStatusCode, contentType: ContentType? = null, headers: List<HttpHeader>? = null): Route {
    val responses = attributes.computeIfAbsent(ResponsesKey) { mutableListOf() } as MutableList
    responses.add(ResponseType(status = status, type = T::class, contentType = contentType, headers = headers))
    return this
}

fun Route.errors(status: HttpStatusCode, contentType: ContentType? = null, headers: List<HttpHeader>? = null): Route {
    val responses = attributes.computeIfAbsent(ResponsesKey) { mutableListOf() } as MutableList
    responses.add(ResponseType(status = status, contentType = contentType, headers = headers))
    return this
}